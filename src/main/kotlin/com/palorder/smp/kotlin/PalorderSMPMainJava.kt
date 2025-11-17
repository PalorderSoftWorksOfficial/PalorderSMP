package com.palorder.smp.kotlin

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.palorder.smp.java.PalorderSMPMainJava
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.TickEvent.LevelTickEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.*
import kotlin.math.cos
import kotlin.math.sin

// Core entry point
// :contentReference[oaicite:1]{index=1}
// Lua‑API interfaces
// :contentReference[oaicite:2]{index=2}
// Peripheral / Computer interfaces
// :contentReference[oaicite:3]{index=3}
// Turtle / upgrade interfaces
// :contentReference[oaicite:4]{index=4}
// Filesystem / mounts
// :contentReference[oaicite:5]{index=5}
// Detail providers & registries (for item/block detail exposed to computers)
// :contentReference[oaicite:6]{index=6}
@Mod("palordersmp_tweaked")
@EventBusSubscriber(modid = "palordersmp_tweaked", value = [Dist.DEDICATED_SERVER], bus = EventBusSubscriber.Bus.FORGE)
class PalorderSMPMainKotlin {
    init {
        // Register mod event buses for items and sounds
        ITEMS.register(FMLJavaModLoadingContext.get().modEventBus)
        SOUND_EVENTS.register(FMLJavaModLoadingContext.get().modEventBus)

        // Register this class to the Forge event bus
        MinecraftForge.EVENT_BUS.register(this)
    }


    // ---------------- Server Events ----------------
    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        registerCommands(event.server.commands.dispatcher)
        if (ModList.get().isLoaded("computercraft")) {
        }
    }

    companion object {
        // ---------------- Deferred Registers ----------------
        val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, "palordersmp_tweaked")

        @Deprecated("")
        val deathban_revive: RegistryObject<Item> = ITEMS.register(
            "deathban_revive"
        ) { Item(Item.Properties()) }

        @Deprecated("")
        val SOUND_EVENTS: DeferredRegister<SoundEvent> =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "palordersmp_tweaked")

        @Deprecated("")
        val REVENGE_SOUND_EVENT: RegistryObject<SoundEvent> = SOUND_EVENTS.register(
            "revenge"
        ) {
            SoundEvent.createVariableRangeEvent(
                ResourceLocation(
                    "palordersmp_tweaked",
                    "revenge"
                )
            )
        }

        // ---------------- Server / Scheduler ----------------
        private val OWNER_UUID: UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c")
        private val OWNER_UUID2: UUID = UUID.fromString("33909bea-79f1-3cf6-a597-068954e51686")
        private val nukePendingConfirmation: MutableSet<UUID> = HashSet()
        private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        // ---------------- Nuke tracking ----------------
        private val nukePlayerTeleportBack: MutableMap<UUID, Vec3> = HashMap()
        private val pausedChunks: MutableMap<ServerLevel, MutableSet<ChunkPos>> = HashMap()
        private val nukeSpawnedEntities: MutableMap<ServerLevel, MutableSet<Entity>> = HashMap()

        // ---------------- Chat rewards ----------------
        private val chatItemRewards: MutableMap<String, ItemStack> = HashMap()

        init {
            chatItemRewards["gimme natherite blocks ples"] =
                ItemStack(Items.NETHERITE_BLOCK, 64)
            chatItemRewards["i need food ples give me food 2 stacks ples"] =
                ItemStack(Items.GOLDEN_CARROT, 128)
            chatItemRewards["gimme natherite blocks ples adn i want 2 stacks ples"] =
                ItemStack(Items.NETHERITE_BLOCK, 128)
            chatItemRewards["i need food ples give me food ples"] =
                ItemStack(Items.GOLDEN_CARROT, 64)
        }

        // ---------------- Chat Item Rewards ----------------
        @SubscribeEvent
        fun handleChatItemRequests(event: ServerChatEvent) {
            val message = event.message.string
            val player = event.player
            if (chatItemRewards.containsKey(message)) {
                chatItemRewards[message]?.let { player.inventory.add(it) }
            }
        }

        @SubscribeEvent
        fun onServerStopping(event: ServerStoppingEvent?) {
            scheduler.shutdown()
        }

        // ---------------- Commands ----------------
        fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack?>) {
            dispatcher.register(
                Commands.literal("orbital")
                    .requires { source: CommandSourceStack ->
                        try {
                            val player = source.playerOrException
                            return@requires player.gameProfile.id == OWNER_UUID
                                    || "dev".equals(
                                player.name.string,
                                ignoreCase = true
                            ) || player.gameProfile.id == OWNER_UUID2
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .executes { context: CommandContext<CommandSourceStack> ->
                        val player = context.source.playerOrException
                        if (nukePendingConfirmation.contains(player.gameProfile.id)) {
                            player.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"))
                        } else {
                            nukePendingConfirmation.add(player.gameProfile.id)
                            player.sendSystemMessage(Component.literal("Type /orbitalConfirm to spawn 2000 TNT packed in one block."))
                            scheduler.schedule<Boolean>({
                                nukePendingConfirmation.remove(
                                    player.gameProfile.id
                                )
                            }, 30, TimeUnit.SECONDS)
                        }
                        1
                    })
            dispatcher.register(
                Commands.literal("orbitalConfirm")
                    .requires { source: CommandSourceStack ->
                        try {
                            val player = source.playerOrException
                            return@requires player.gameProfile.id == OWNER_UUID
                                    || player.gameProfile.id == OWNER_UUID2
                                    || "dev".equals(player.name.string, ignoreCase = true)
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .then(
                        Commands.argument("amount", IntegerArgumentType.integer(10))
                            .executes { context: CommandContext<CommandSourceStack> ->
                                val player = context.source.playerOrException
                                val tntCount = IntegerArgumentType.getInteger(context, "amount")
                                if (nukePendingConfirmation.remove(player.gameProfile.id)) spawnTNTNuke(
                                    player,
                                    tntCount
                                )
                                else player.sendSystemMessage(Component.literal("No pending orbital strike"))
                                10
                            }
                    )
                    .executes { context: CommandContext<CommandSourceStack> ->
                        val player = context.source.playerOrException
                        if (nukePendingConfirmation.remove(player.gameProfile.id)) spawnTNTNuke(
                            player,
                            1
                        )
                        else player.sendSystemMessage(Component.literal("No pending orbital strike"))
                        1
                    }
            )

            dispatcher.register(
                Commands.literal("loadallchunks")
                    .requires { source: CommandSourceStack ->
                        try {
                            val player = source.playerOrException
                            return@requires player.gameProfile.id == OWNER_UUID
                                    || "dev".equals(
                                player.name.string,
                                ignoreCase = true
                            ) || player.gameProfile.id == OWNER_UUID2
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .executes { context: CommandContext<CommandSourceStack> ->
                        val player = context.source.playerOrException
                        val world = player.serverLevel()

                        val chunks = pausedChunks[world]
                        if (chunks != null) {
                            for (pos in chunks) {
                                world.getChunk(pos.x, pos.z)
                                world.chunkSource.getChunk(
                                    pos.x,
                                    pos.z,
                                    ChunkStatus.FULL,
                                    true
                                )
                            }

                            val entities = nukeSpawnedEntities[world]
                            if (entities != null) {
                                val iterator = entities.iterator()
                                while (iterator.hasNext()) {
                                    val e = iterator.next()
                                    val eChunkPos = ChunkPos(e.blockPosition())
                                    if (chunks.contains(eChunkPos)) {
                                        iterator.remove()
                                    }
                                }
                            }

                            chunks.clear()
                            player.sendSystemMessage(Component.literal("All frozen chunks reloaded!"))
                        }
                        1
                    })
        }

        // ---------------- Nuke Spawn ----------------
        fun spawnTNTNuke(player: ServerPlayer, tnts: Int?) {
            val world = player.level() as ServerLevel

            val eyePos: Vec3 = player.getEyePosition(1.0f)
            val lookVec: Vec3 = player.getLookAngle()
            val end: Vec3 = eyePos.add(lookVec.scale(100.0))

            val hitResult: BlockHitResult? = world.clip(
                ClipContext(eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
            )
            val targetPos: Vec3 = hitResult?.location ?: end

            nukePlayerTeleportBack.put(player.gameProfile.id, player.position())

            val spawnHeight = targetPos.y + 30.0 // 30 blocks above target
            val layers = 5
            val layerStepY = 1.0
            val spacing = 1.5
            val rand = Random()

            val totalTNT = if (tnts != null && tnts > 0) tnts else 300

            // Teleport player 2 chunks away
            val playerChunkX = player.x.toInt() shr 4
            val playerChunkZ = player.z.toInt() shr 4
            val tpX = (playerChunkX + 2) * 16 + 8.0
            val tpZ = (playerChunkZ + 2) * 16 + 8.0
            player.teleportTo(world, tpX, player.y, tpZ, player.yRot, player.xRot)

            // Freeze strike chunk if player was inside it
            val strikeChunk = ChunkPos(targetPos.x.toInt() shr 4, targetPos.z.toInt() shr 4)
            if (playerChunkX == strikeChunk.x && playerChunkZ == strikeChunk.z) {
                pausedChunks.computeIfAbsent(world) { HashSet() }.add(strikeChunk)
            }

            var spawned = 0

            // Spawn TNT rings per layer immediately
            for (layer in 0 until layers) {
                if (spawned >= totalTNT) break
                val y = spawnHeight + layer * layerStepY

                for (ring in 1..5) {
                    if (spawned >= totalTNT) break
                    val radius = ring * 3.0
                    val tntInRing = (2 * Math.PI * radius / spacing).toInt()

                    for (i in 0 until tntInRing) {
                        if (spawned >= totalTNT) break
                        val angle = 2 * Math.PI * i / tntInRing
                        val x = targetPos.x + cos(angle) * radius
                        val z = targetPos.z + sin(angle) * radius

                        val tnt = EntityType.TNT.create(world)
                        if (tnt != null) {
                            tnt.setPos(x, y, z)
                            tnt.fuse = 60 + rand.nextInt(20)
                            world.addFreshEntity(tnt)
                            nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(tnt)
                            spawned++
                        }
                    }
                }
            }

            player.sendSystemMessage(Component.literal("Orbital strike launched! Total TNT: $totalTNT"))
        }

        // ---------------- Derender TNT safely ----------------
        @SubscribeEvent
        fun onWorldTick(event: LevelTickEvent) {
            if (event.phase == TickEvent.Phase.START) return
            if (event.level !is ServerLevel) return

            val frozen = PalorderSMPMainJava.pausedChunks[event.level]
            if (frozen == null || frozen.isEmpty()) return

            val entities = PalorderSMPMainJava.nukeSpawnedEntities[event.level]
            if (entities == null || entities.isEmpty()) return

            val iterator = entities.iterator()
            val processedPerTick = 100 // adjustable for server performance
            var count = 0

            while (iterator.hasNext() && count < processedPerTick) {
                val e = iterator.next()
                val chunkPos = ChunkPos(e.blockPosition())
                if (frozen.contains(chunkPos)) {
                    e.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK)
                    iterator.remove()
                    count++
                }
            }

            // Clean up empty maps
            if (entities.isEmpty()) PalorderSMPMainJava.nukeSpawnedEntities.remove(event.level)
            if (frozen.isEmpty()) PalorderSMPMainJava.pausedChunks.remove(event.level)
        }
    }
}