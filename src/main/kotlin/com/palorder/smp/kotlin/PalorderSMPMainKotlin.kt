package com.palorder.smp.kotlin

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.material.*
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import net.minecraftforge.fml.ModList
import com.palorder.smp.java.PalorderSMPMainJava.rand
import org.jetbrains.annotations.Nullable
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.file.FileConfig
import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.ILuaAPIFactory
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.ILuaFunction
import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaTable
import dan200.computercraft.api.lua.LuaValues
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IDynamicPeripheral
import dan200.computercraft.api.turtle.ITurtleUpgrade
import dan200.computercraft.api.turtle.ITurtleAccess
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider
import dan200.computercraft.api.filesystem.Mount
import dan200.computercraft.api.filesystem.WritableMount
import dan200.computercraft.api.detail.DetailProvider
import dan200.computercraft.api.detail.DetailRegistry
import net.minecraft.world.level.Explosion
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

@Mod("palordersmp_tweaked_kotlin_beta")
@Mod.EventBusSubscriber(modid = "palordersmp_tweaked_kotlin_beta", value = [Dist.DEDICATED_SERVER], bus = Mod.EventBusSubscriber.Bus.FORGE)
class PalorderSMPMainKotlin {
    /*
    Its recommended that you reuse classes from the java version tho changes are consistent so you would need to consistently update the function calls
    Hey java devs, Please do not reuse code from the kotlin version as it's just mirrored from the java version!
    */

    init {
        MinecraftForge.EVENT_BUS.register(this)

        if (ModList.get().isLoaded("computercraft")) {
            logger.info("ComputerCraft is installed, Registering addon stuff. (non-existent lmao for now...)")
        } else {
            logger.warn("ComputerCraft is NOT present!")
        }
    }

    companion object {
        // ---------------- Deferred Registers ----------------
        val ITEMS: DeferredRegister<Item> =
            DeferredRegister.create(ForgeRegistries.ITEMS, "palordersmp_tweaked_kotlin_beta")
        val BLOCKS: DeferredRegister<Block> =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "palordersmp_tweaked_kotlin_beta")
        val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "palordersmp_tweaked_kotlin_beta")
        val scheduled = HashMap<Int, MutableList<() -> Unit>>()

        fun runLater(world: ServerLevel, ticks: Int, r: () -> Unit) {
            val targetTick = (world.gameTime + ticks).toInt()
            scheduled.computeIfAbsent(targetTick) { ArrayList() }.add(r)
        }

        val logger: Logger = LogManager.getLogger(PalorderSMPMainKotlin::class.java)

        // ---------------- Server / Scheduler ----------------
        val OWNER_UUID: UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c")
        val OWNER_UUID2: UUID = UUID.fromString("33909bea-79f1-3cf6-a597-068954e51686")
        val DEV_UUID: UUID = UUID.fromString("380df991-f603-344c-a090-369bad2a924a")
        val nukePendingConfirmation: MutableSet<UUID> = HashSet()
        val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        // ---------------- Nuke tracking ----------------
        val nukePlayerTeleportBack: MutableMap<UUID, Vec3> = HashMap()
        val pausedChunks: MutableMap<ServerLevel, MutableSet<ChunkPos>> = HashMap()
        val nukeSpawnedEntities: MutableMap<ServerLevel, MutableSet<Entity>> = HashMap()

        // ---------------- Chat rewards ----------------
        private val chatItemRewards: MutableMap<String, ItemStack> = HashMap()
        private val log: Logger = LogManager.getLogger(PalorderSMPMainKotlin::class.java)

        init {
            chatItemRewards["gimme natherite blocks ples"] = ItemStack(Items.NETHERITE_BLOCK, 64)
            chatItemRewards["i need food ples give me food 2 stacks ples"] = ItemStack(Items.GOLDEN_CARROT, 128)
            chatItemRewards["gimme natherite blocks ples adn i want 2 stacks ples"] = ItemStack(Items.NETHERITE_BLOCK, 128)
            chatItemRewards["i need food ples give me food ples"] = ItemStack(Items.GOLDEN_CARROT, 64)
        }

        @JvmStatic
        @SubscribeEvent
        fun handleChatItemRequests(event: ServerChatEvent) {
            val message = event.message.string
            val player = event.player
            val reward = chatItemRewards[message]
            if (reward != null) {
                player.inventory.add(reward)
            }
        }

        @JvmStatic
        @SubscribeEvent
        fun onServerStarting(event: ServerStartingEvent) {
            registerCommands(event.server.commands.dispatcher)
        }

        @JvmStatic
        @SubscribeEvent
        fun onServerStopping(event: ServerStoppingEvent) {
            scheduler.shutdown()
        }
        @JvmStatic
        @SubscribeEvent
        fun onServerTick(e: TickEvent.ServerTickEvent) {
            if (e.phase != TickEvent.Phase.END) return
            val world = e.server.overworld()
            val time = world.gameTime.toInt()
            val list = scheduled.remove(time)
            list?.forEach { it.invoke() }
        }

        // ---------------- Commands ----------------
        @JvmStatic
        fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {

            dispatcher.register(
                Commands.literal("orbital")
                    .requires { source ->
                        try {
                            val player = source.player
                            if (player != null) {
                                player.gameProfile.id == OWNER_UUID ||
                                        player.name.string.equals("dev", ignoreCase = false) ||
                                        player.gameProfile.id == OWNER_UUID2
                            } else true
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .then(
                        Commands.argument("target", StringArgumentType.word())
                            .executes { context ->
                                val player = context.source.server.playerList
                                    .getPlayerByName(StringArgumentType.getString(context, "target"))
                                    ?: return@executes 0

                                val playerId = player.gameProfile.id
                                if (!(playerId == OWNER_UUID || playerId == DEV_UUID || playerId == OWNER_UUID2)) return@executes 0

                                if (nukePendingConfirmation.contains(playerId)) {
                                    context.source.sendSuccess({ Component.literal("Pending confirmation! Use /orbitalConfirm") }, false)
                                } else {
                                    nukePendingConfirmation.add(playerId)
                                    context.source.sendSuccess({ Component.literal("Type /orbitalConfirm to spawn 2000 TNT packed in one block.") }, false)
                                    scheduler.schedule({ nukePendingConfirmation.remove(playerId) }, 30, TimeUnit.SECONDS)
                                }
                                1
                            }
                    )
                    .executes { context ->
                        val player = try {
                            context.source.playerOrException
                        } catch (e: Exception) {
                            return@executes 0
                        }

                        val playerId = player.gameProfile.id
                        if (nukePendingConfirmation.contains(playerId)) {
                            context.source.sendSuccess({ Component.literal("Pending confirmation! Use /orbitalConfirm") }, false)
                        } else {
                            nukePendingConfirmation.add(playerId)
                            context.source.sendSuccess(
                                { Component.literal("Type /orbitalConfirm <ARGS HERE>\nHave fun dominating the server :3") },
                                false
                            )
                            scheduler.schedule({ nukePendingConfirmation.remove(playerId) }, 30, TimeUnit.SECONDS)
                        }
                        1
                    }
            )

            dispatcher.register(
                Commands.literal("orbitalConfirm")
                    .requires { source ->
                        try {
                            val player = source.player
                            if (player != null) {
                                player.gameProfile.id == OWNER_UUID ||
                                        player.name.string.equals("dev", ignoreCase = false) ||
                                        player.gameProfile.id == OWNER_UUID2
                            } else true
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .then(
                        Commands.argument("target", StringArgumentType.word())
                            .then(
                                Commands.argument("amount", IntegerArgumentType.integer(0))
                                    .then(
                                        Commands.argument("type", StringArgumentType.string())
                                            .suggests { _, builder ->
                                                net.minecraft.commands.SharedSuggestionProvider.suggest(listOf("nuke", "stab"), builder)
                                            }
                                            .then(
                                                Commands.argument("layers", IntegerArgumentType.integer(1, 5000))
                                                    .executes { context ->
                                                        val player = context.source.server.playerList
                                                            .getPlayerByName(StringArgumentType.getString(context, "target"))
                                                            ?: return@executes 0

                                                        val tntCount = IntegerArgumentType.getInteger(context, "amount")
                                                        var type = StringArgumentType.getString(context, "type")
                                                        val layers = IntegerArgumentType.getInteger(context, "layers")

                                                        if (!type.equals("nuke", ignoreCase = true) && !type.equals("stab", ignoreCase = true))
                                                            type = "nuke"

                                                        if (nukePendingConfirmation.remove(player.gameProfile.id))
                                                            spawnTNTNuke(player, tntCount, type, layers)

                                                        1
                                                    }
                                            )
                                    )
                            )
                    )
            )

            dispatcher.register(
                Commands.literal("fastorbital")
                    .requires { source ->
                        try {
                            val player = source.player
                            if (player != null) {
                                val id = player.gameProfile.id
                                id == OWNER_UUID || id == OWNER_UUID2 || player.name.string.equals("dev", ignoreCase = true)
                            } else {
                                true
                            }
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .then(
                        Commands.argument("target", StringArgumentType.word())
                            .executes { context ->
                                val source = context.source
                                val player = source.server.playerList.getPlayerByName(StringArgumentType.getString(context, "target"))
                                if (player != null) {
                                    spawnTNTNuke(player, 500, "nuke", 5000)
                                    player.sendSystemMessage(Component.literal("Fastorbitaled be ready lmao"))
                                } else {
                                    source.sendSuccess({ Component.literal("Fastorbitaled be ready lmao.") }, false)
                                }
                                1
                            }
                    )
            )
            dispatcher.register(
                Commands.literal("faststab")
                    .requires { source ->
                        try {
                            val player = source.player
                            if (player != null) {
                                val id = player.gameProfile.id
                                id == OWNER_UUID || id == OWNER_UUID2 || player.name.string.equals("dev", ignoreCase = true)
                            } else {
                                true
                            }
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .then(
                        Commands.argument("target", StringArgumentType.word())
                            .executes { context ->
                                val source = context.source
                                val player = source.server.playerList.getPlayerByName(StringArgumentType.getString(context, "target"))
                                if (player != null) {
                                    spawnTNTNuke(player, 900, "stab", 1)
                                    player.sendSystemMessage(Component.literal("Faststabbed be ready lmao"))
                                } else {
                                    source.sendSuccess({ Component.literal("Faststabbed be ready lmao.") }, false)
                                }
                                1
                            }
                    )
            )
            dispatcher.register(
                Commands.literal("loadallchunks")
                    .requires { source ->
                        try {
                            val player = source.playerOrException
                            player.gameProfile.id == OWNER_UUID ||
                                    player.name.string.equals("dev", ignoreCase = true) ||
                                    player.gameProfile.id == OWNER_UUID2
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .executes { context ->
                        val player = context.source.playerOrException
                        val world = player.serverLevel()

                        val chunks = pausedChunks[world]
                        if (chunks != null) {
                            for (pos in chunks) {
                                world.getChunk(pos.x, pos.z)
                                world.chunkSource.getChunk(pos.x, pos.z, ChunkStatus.FULL, true)
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
                            context.source.sendSuccess({ Component.literal("All frozen chunks reloaded!") }, false)
                        }

                        1
                    }
            )
        }

        // ---------------- Nuke Spawn ----------------
        @JvmStatic
        fun spawnTNTNuke(player: ServerPlayer, tnts: Int?, type: String?, layers: Int?) {
            val world = player.level() as ServerLevel
            val eyePos = player.eyePosition
            val lookVec = player.lookAngle
            val end = eyePos.add(lookVec.scale(100000.0))
            val hitResult = world.clip(ClipContext(eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
            val targetPos = hitResult?.location ?: end

            nukePlayerTeleportBack[player.gameProfile.id] = player.position()
            val total = tnts?.takeIf { it > 0 } ?: 300
            val layersFinal = layers ?: 0

            world.server.execute {
                when (type) {
                    "stab" -> {
                        var y = targetPos.y + 30.0
                        val minY = world.minBuildHeight.toDouble()
                        var count = 0
                        while (y >= minY && count < total) {
                            val tnt = EntityType.TNT.create(world) as? PrimedTnt
                            if (tnt != null) {
                                tnt.setPos(targetPos.x, y, targetPos.z)
                                tnt.fuse = 100 + rand.nextInt(5)
                                tnt.isNoGravity = true
                                tnt.setDeltaMovement(0.0, 0.0, 0.0)
                                world.addFreshEntity(tnt)
                                nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(tnt)
                            }
                            y -= 1.0
                            count++
                        }
                    }

                    "chunkdel" -> {
                        val chunkX = (targetPos.x.toInt()) shr 4
                        val chunkZ = (targetPos.z.toInt()) shr 4
                        val minY = world.minBuildHeight
                        val maxY = world.maxBuildHeight
                        for (cx in (chunkX shl 4) until (chunkX shl 4) + 16) {
                            for (cz in (chunkZ shl 4) until (chunkZ shl 4) + 16) {
                                for (y in maxY - 1 downTo minY) {
                                    val tnt = EntityType.TNT.create(world) as? PrimedTnt
                                    if (tnt != null) {
                                        tnt.setPos(cx + 0.5, y + 0.5, cz + 0.5)
                                        tnt.setFuse(60)
                                        tnt.setNoGravity(true)
                                        tnt.setDeltaMovement(0.0, 0.0, 0.0)
                                        world.addFreshEntity(tnt)
                                        nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(tnt)
                                    }
                                }
                            }
                        }
                    }

                    "chunklaser" -> {
                        val chunkX = (targetPos.x.toInt()) shr 4
                        val chunkZ = (targetPos.z.toInt()) shr 4
                        val y0 = targetPos.y.toInt()
                        for (cx in (chunkX shl 4) until (chunkX shl 4) + 16) {
                            for (cz in (chunkZ shl 4) until (chunkZ shl 4) + 16) {
                                val tnt = EntityType.TNT.create(world) as? PrimedTnt
                                if (tnt != null) {
                                    tnt.setPos(cx + 0.5, y0 + 0.5, cz + 0.5)
                                    tnt.setFuse(60)
                                    tnt.setNoGravity(true)
                                    tnt.setDeltaMovement(0.0, 0.0, 0.0)
                                    world.addFreshEntity(tnt)
                                    nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(tnt)
                                }
                            }
                        }
                    }

                    "nuke" -> {
                        val spawnHeight = targetPos.y + 30.0
                        val layerStepY = 1.0
                        val spacing = 1.5
                        var spawned = 0
                        for (layer in 0 until layersFinal) {
                            if (spawned >= total) break
                            val y = spawnHeight + layer * layerStepY
                            for (ring in 1..5) {
                                if (spawned >= total) break
                                val radius = ring * 3.0 * (layer + 1)
                                val tntsInRing = kotlin.math.floor(2 * Math.PI * radius / spacing).toInt()
                                for (i in 0 until tntsInRing) {
                                    if (spawned >= total) break
                                    val angle = 2 * Math.PI * i / tntsInRing
                                    val x = targetPos.x + kotlin.math.cos(angle) * radius
                                    val z = targetPos.z + kotlin.math.sin(angle) * radius
                                    val tnt = EntityType.TNT.create(world) as? PrimedTnt
                                    if (tnt != null) {
                                        tnt.setPos(x, y, z)
                                        tnt.setFuse(100)
                                        tnt.setNoGravity(false)
                                        tnt.setDeltaMovement(0.0, 0.0, 0.0)
                                        world.addFreshEntity(tnt)
                                        nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(tnt)
                                        spawned++
                                    }
                                }
                            }
                        }
                    }

                    null -> {
                        val ex = IllegalArgumentException("type cant be nothing, how did you do this.")
                        logger.error("Invalid argument encountered", ex)
                    }
                }

                player.sendSystemMessage(Component.literal("Orbital strike launched! Type: $type, Total: $total"))
            }
        }

        // ---------------- Derender TNT safely ----------------
        @Deprecated(
            message = "This method is unsafe in the main thread, separate it from the main thread or just don't use it, DO NOT USE!",
            level = DeprecationLevel.ERROR
        )
        @JvmStatic
        @SubscribeEvent
        fun onWorldTick(event: TickEvent.LevelTickEvent) {
            if (event.phase == TickEvent.Phase.START) return
            val level = event.level
            if (level !is ServerLevel) return
            val world = level as ServerLevel

            val frozen = pausedChunks[world] ?: return
            if (frozen.isEmpty()) return

            val entities = nukeSpawnedEntities[world] ?: return
            if (entities.isEmpty()) return

            val iterator = entities.iterator()
            val processedPerTick = 100
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

            if (entities.isEmpty()) nukeSpawnedEntities.remove(world)
            if (frozen.isEmpty()) pausedChunks.remove(world)
        }
    }
}
