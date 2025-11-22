package com.palorder.smp.kotlin

import com.electronwill.nightconfig.core.file.FileConfig
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.palorder.smp.java.PalorderSMPMainJava
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
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
import net.minecraft.world.level.storage.LevelResource
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
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.floor
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
    annotation class hello

    init {
        // Register this class to the Forge event bus
        MinecraftForge.EVENT_BUS.register(this)
    }

    @Throws(java.lang.Exception::class)
    private fun injectModsConfigCC(server: MinecraftServer) {
        val worldFolder = server.getWorldPath(LevelResource.ROOT)

        val configPath = worldFolder.resolve("serverconfig/computercraft-server.toml")
        if (!Files.exists(configPath)) return

        FileConfig.of(configPath).use { config ->
            config.load()
            config.set<Any?>("computer_space_limit", 1073741824)
            config.set<Any?>("floppy_space_limit", 1073741824)
            config.set<Any?>("upload_max_size", 524288)
            config.set<Any?>("maximum_open_files", 128)
            config.set<Any?>("default_computer_settings", "")
            config.set<Any?>("log_computer_errors", true)
            config.set<Any?>("command_require_creative", true)
            config.set<Any?>("disabled_generic_methods", ArrayList<Any?>())

            config.set<Any?>("execution.computer_threads", 1)
            config.set<Any?>("execution.max_main_global_time", 10)
            config.set<Any?>("execution.max_main_computer_time", 5)

            config.set<Any?>("http.enabled", true)
            config.set<Any?>("http.websocket_enabled", true)
            config.set<Any?>("http.max_requests", 100)
            config.set<Any?>("http.max_websockets", 100)
            config.set<Any?>("http.bandwidth.global_download", 1073741824)
            config.set<Any?>("http.bandwidth.global_upload", 1073741824)
            config.set<Any?>("http.proxy.type", "HTTP")
            config.set<Any?>("http.proxy.host", "")
            config.set<Any?>("http.proxy.port", 8080)

            val httpRules: MutableList<Any?> = ArrayList<Any?>()
            val rule1: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            rule1.put("host", "\$private")
            rule1.put("action", "deny")
            httpRules.add(rule1)

            val rule2: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            rule2.put("host", "*")
            rule2.put("action", "allow")
            rule2.put("max_download", 16777216)
            rule2.put("max_upload", 4194304)
            rule2.put("max_websocket_message", 131072)
            rule2.put("use_proxy", false)
            httpRules.add(rule2)

            config.set<Any?>("http.rules", httpRules)

            config.set<Any?>("peripheral.command_block_enabled", true)
            config.set<Any?>("peripheral.modem_range", 64)
            config.set<Any?>("peripheral.modem_high_altitude_range", 384)
            config.set<Any?>("peripheral.modem_range_during_storm", 64)
            config.set<Any?>("peripheral.modem_high_altitude_range_during_storm", 384)
            config.set<Any?>("peripheral.max_notes_per_tick", 8)
            config.set<Any?>("peripheral.monitor_bandwidth", 1000000)

            config.set<Any?>("turtle.need_fuel", false)
            config.set<Any?>("turtle.normal_fuel_limit", 20000)
            config.set<Any?>("turtle.advanced_fuel_limit", 100000)
            config.set<Any?>("turtle.can_push", true)

            config.set<Any?>("term_sizes.computer.width", 51)
            config.set<Any?>("term_sizes.computer.height", 19)

            config.set<Any?>("term_sizes.pocket_computer.width", 26)
            config.set<Any?>("term_sizes.pocket_computer.height", 20)

            config.set<Any?>("term_sizes.monitor.width", 8)
            config.set<Any?>("term_sizes.monitor.height", 6)
            config.save()
        }
    }
    // ---------------- Server Events ----------------
    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        registerCommands(event.server.commands.dispatcher)

        val server = event.server
        if (ModList.get().isLoaded("computercraft")) {
            // Run config injection asynchronously to prevent server hang
            Thread {
                try {
                    injectModsConfigCC(server)
                } catch (e: Exception) {
                    PalorderSMPMainJava.logger.warn(
                        "Failed to inject configuration into: [computercraft] \n please find a compatible version.",
                        e
                    )
                }
            }.start()
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
        val logger: Logger = LogManager.getLogger(PalorderSMPMainKotlin::class.java)

        // ---------------- Server / Scheduler ----------------
        val OWNER_UUID: UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c")
        val OWNER_UUID2: UUID = UUID.fromString("33909bea-79f1-3cf6-a597-068954e51686")
        val DEV_UUID: UUID = PalorderSMPMainJava.DEV_UUID
        val nukePendingConfirmation: MutableSet<UUID> = HashSet()
        val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        // ---------------- Nuke tracking ----------------
        val nukePlayerTeleportBack: MutableMap<UUID, Vec3> = HashMap()
        val pausedChunks: MutableMap<ServerLevel, MutableSet<ChunkPos>> = HashMap()
        val nukeSpawnedEntities: MutableMap<ServerLevel, MutableSet<Entity>> = HashMap()

        // ---------------- Chat rewards ----------------
        private val chatItemRewards: MutableMap<String, ItemStack> = HashMap()
        private val log: Logger = LogManager.getLogger(
            PalorderSMPMainKotlin::class.java
        )

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
                player.inventory.add(chatItemRewards[message])
            }
        }

        @SubscribeEvent
        fun onServerStopping(event: ServerStoppingEvent?) {
            scheduler.shutdown()
        }

        // ---------------- Commands ----------------
        fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack?>) {
            dispatcher.register(Commands.literal("orbital")
                .requires { source: CommandSourceStack ->
                    try {
                        val player = source.playerOrException
                        return@requires player.gameProfile.id == OWNER_UUID
                                || player.gameProfile.id == DEV_UUID || player.gameProfile.id == OWNER_UUID2
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes { context ->
                        val player = context.source.server.playerList.getPlayerByName(StringArgumentType.getString(context, "target")) ?: return@executes 0
                        val playerId = player.gameProfile.id
                        if (!(playerId == OWNER_UUID || playerId == DEV_UUID || playerId == OWNER_UUID2)) return@executes 0
                        if (nukePendingConfirmation.contains(playerId)) {
                            player.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"))
                        } else {
                            nukePendingConfirmation.add(playerId)
                            player.sendSystemMessage(Component.literal("Type /orbitalConfirm to spawn 2000 TNT packed in one block."))
                            scheduler.schedule({ nukePendingConfirmation.remove(playerId) }, 30, TimeUnit.SECONDS)
                        }
                        1
                    })
                .executes { context ->
                    val player = context.source.playerOrException
                    val playerId = player.gameProfile.id
                    if (nukePendingConfirmation.contains(playerId)) {
                        player.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"))
                    } else {
                        nukePendingConfirmation.add(playerId)
                        player.sendSystemMessage(Component.literal("Type /orbitalConfirm to spawn 2000 TNT packed in one block."))
                        scheduler.schedule({ nukePendingConfirmation.remove(playerId) }, 30, TimeUnit.SECONDS)
                    }
                    1
                }
            )

            dispatcher.register(Commands.literal("orbitalConfirm")
                .requires { source: CommandSourceStack ->
                    try {
                        val player = source.playerOrException
                        return@requires player.gameProfile.id == OWNER_UUID
                                || player.gameProfile.id == DEV_UUID || player.gameProfile.id == OWNER_UUID2
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
                .then(Commands.argument("target", StringArgumentType.word())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .then(Commands.argument("type", StringArgumentType.string())
                            .suggests { _, builder ->
                                net.minecraft.commands.SharedSuggestionProvider.suggest(listOf("nuke", "stab"), builder)
                            }
                            .then(Commands.argument("layers", IntegerArgumentType.integer(1, 50))
                                .executes { context ->
                                    val player = context.source.server.playerList.getPlayerByName(StringArgumentType.getString(context, "target")) ?: return@executes 0
                                    val tntCount = IntegerArgumentType.getInteger(context, "amount")
                                    var type = StringArgumentType.getString(context, "type")
                                    val layers = IntegerArgumentType.getInteger(context, "layers")
                                    if (!type.equals("nuke", true) && !type.equals("stab", true)) type = "nuke"
                                    if (nukePendingConfirmation.remove(player.gameProfile.id)) spawnTNTNuke(player, tntCount, type, layers)
                                    1
                                })))))
            dispatcher.register(
                Commands.literal("loadallchunks")
                    .requires { source: CommandSourceStack ->
                        try {
                            val player = source.playerOrException
                            return@requires player.gameProfile.id == OWNER_UUID
                                    || player.gameProfile.id == DEV_UUID || player.gameProfile.id == OWNER_UUID2
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
        fun spawnTNTNuke(player: ServerPlayer, tnts: Int?, type: String, layers: Int) {
            val world = player.level() as ServerLevel

            val eyePos = player.getEyePosition(1.0f)
            val lookVec = player.lookAngle
            val end = eyePos.add(lookVec.scale(100000.0))
            val hitResult = world.clip(
                ClipContext(
                    eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
                )
            )
            val targetPos = if (hitResult != null) hitResult.location else end

            nukePlayerTeleportBack[player.gameProfile.id] = player.position()

            val spawnHeight = targetPos.y + 30
            val layerStepY = 1.0
            val spacing = 1.5
            val rand = Random()

            val totalTNT = if (tnts != null && tnts > 0) tnts else 300

            val playerChunkX = player.x.toInt() shr 4
            val playerChunkZ = player.z.toInt() shr 4
            val tpX = ((playerChunkX + 2) * 16 + 8).toDouble()
            val tpZ = ((playerChunkZ + 2) * 16 + 8).toDouble()
            player.teleportTo(world, tpX, player.y, tpZ, player.yRot, player.xRot)

            val strikeChunk = ChunkPos(targetPos.x.toInt() shr 4, targetPos.z.toInt() shr 4)
            if (playerChunkX == strikeChunk.x && playerChunkZ == strikeChunk.z) {
                pausedChunks.computeIfAbsent(world) { k: ServerLevel? -> HashSet() }.add(strikeChunk)
            }

            var spawned = 0

            if (type == "stab") {
                for (i in 0..<totalTNT) {
                    val tnt = EntityType.TNT.create(world)
                    if (tnt != null) {
                        tnt.setPos(targetPos.x, player.y, targetPos.z)
                        tnt.fuse = 60 + rand.nextInt(20)
                        world.addFreshEntity(tnt)
                        nukeSpawnedEntities.computeIfAbsent(world) { k: ServerLevel? -> HashSet() }.add(tnt)
                    }
                }
            } else if (type == "nuke") {
                var layer = 0
                while (layer < layers && spawned < totalTNT) {
                    val y = spawnHeight + layer * layerStepY

                    var ring = 1
                    while (spawned < totalTNT && ring <= 5) {
                        // PATCH: radius now increases with layer too
                        val radius = ring * 3.0 * (layer + 1)

                        val tntInRing = floor((2 * Math.PI * radius) / spacing).toInt()

                        var i = 0
                        while (i < tntInRing && spawned < totalTNT) {
                            val angle = 2 * Math.PI * i / tntInRing
                            val x = targetPos.x + cos(angle) * radius
                            val z = targetPos.z + sin(angle) * radius

                            val tnt = EntityType.TNT.create(world)
                            if (tnt != null) {
                                tnt.setPos(x, y, z)
                                tnt.fuse = 60 + rand.nextInt(20)
                                world.addFreshEntity(tnt)
                                nukeSpawnedEntities.computeIfAbsent(world) { k: ServerLevel? -> HashSet() }.add(tnt)
                                spawned++
                            }
                            i++
                        }
                        ring++
                    }
                    layer++
                }
            } else if (type == null) {
                val ex =
                    IllegalArgumentException("type cant be nothing, how did you do this.")
                logger.error("Invalid argument encountered", ex)
            }

            player.sendSystemMessage(
                Component.literal(
                    "Orbital strike launched! Total TNT: $totalTNT, Type: $type"
                )
            )
        }

        // ---------------- Derender TNT safely ----------------
        @SubscribeEvent
        fun onWorldTick(event: LevelTickEvent) {
            if (event.phase == TickEvent.Phase.START) return
            if (event.level !is ServerLevel) return

            val frozen: Set<ChunkPos>? = pausedChunks[event.level]
            if (frozen == null || frozen.isEmpty()) return

            val entities = nukeSpawnedEntities[event.level]
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
            if (entities.isEmpty()) nukeSpawnedEntities.remove(event.level)
            if (frozen.isEmpty()) pausedChunks.remove(event.level)
        }
    }
}