package com.palorder.smp.kotlin

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.palorder.smp.java.PalorderSMPMainJava
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.entity.projectile.Arrow
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
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
        val scheduled = HashMap<Int, MutableList<() -> Unit>>()

        fun runLater(world: ServerLevel, ticks: Int, r: () -> Unit) {
            val targetTick = (world.gameTime + ticks).toInt()
            scheduled.computeIfAbsent(targetTick) { ArrayList() }.add(r)
        }

        val logger: Logger = LogManager.getLogger(PalorderSMPMainKotlin::class)

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

        @SubscribeEvent
        fun onUse(e: PlayerInteractEvent.RightClickItem) {
            if (e.level.isClientSide) return

            val s = e.itemStack
            if (s.item !is FishingRodItem) return

            val t = s.orCreateTag
            val type = if (t.contains("RodType")) t.getString("RodType") else null
            if (type == null) return

            val p = e.entity as ServerPlayer
            val world = p.serverLevel()

            val rodUse = t.getInt("RodUse") + 1
            t.putInt("RodUse", rodUse)

            if (rodUse == 1) {
                runLater(world, 130) {
                    if (s.hasTag()) s.tag!!.putInt("RodUse", 0)
                }
                return
            }

            if (rodUse < 2) return

            val amount = when (type) {
                "stab", "ArrowStab" -> 1800
                "nuke", "ArrowNuke" -> 775
                "chunklaser" -> 256
                "chunkdel" -> 49152
                "Wolf" -> 150
                else -> 0
            }

            val layers = when (type) {
                "stab", "chunklaser", "chunkdel", "ArrowStab" -> 1
                "nuke" -> 0
                else -> 0
            }

            runLater(world, 10) {
                if (!p.isAlive) return@runLater
                if (type == "ArrowNuke" || type == "ArrowStab") {
                    spawnArrowTNTNuke(p, amount, type)
                }
                else if (type == "Wolf") {
                    summonWolves(p,amount)
                }
                else {
                    spawnTNTNuke(p, amount, type, layers)
                }

                t.putInt("RodUse", 0)
            }
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
                                                net.minecraft.commands.SharedSuggestionProvider.suggest(listOf("nuke", "stab","chunkdel","chunklaser"), builder)
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
                                    spawnTNTNuke(player, 1000, "nuke", 50000)
                                    player.sendSystemMessage(Component.literal("Fastorbitaled be ready lmao"))
                                } else {
                                    source.sendSuccess({ Component.literal("Fastorbitaled be ready lmao.") }, false)
                                }
                                1
                            }
                    )
            )
            dispatcher.register(
                Commands.literal("linkfrod")
                    .requires(Predicate requires@{ source: CommandSourceStack? ->
                        try {
                            val player = source!!.getPlayer()
                            if (player != null) {
                                return@requires player.getGameProfile().getId() == PalorderSMPMainJava.OWNER_UUID
                                        || player.getGameProfile().getId() == PalorderSMPMainJava.OWNER_UUID2
                                        || "dev".equals(player.getName().getString(), ignoreCase = true)
                            }
                            return@requires true
                        } catch (e: java.lang.Exception) {
                            throw java.lang.RuntimeException(e)
                        }
                    })
                    .then(
                        Commands.argument<String?>("target", StringArgumentType.word())
                            .then(
                                Commands.argument<String?>("type", StringArgumentType.string())
                                    .suggests(SuggestionProvider { ctx: CommandContext<CommandSourceStack?>?, builder: SuggestionsBuilder? ->
                                        SharedSuggestionProvider.suggest(
                                            mutableListOf<String?>("nuke", "stab", "chunklaser", "chunkdel","ArrowNuke","ArrowStab"),
                                            builder
                                        )
                                    })
                                    .executes(Command executes@{ context: CommandContext<CommandSourceStack?>? ->
                                        val p = context!!.getSource()!!.getPlayer()
                                        val type = StringArgumentType.getString(context, "type")
                                        checkNotNull(p)
                                        val i = p.getMainHandItem()
                                        if (i.getItem() !is FishingRodItem) return@executes 0
                                        i.getOrCreateTag().putString("RodType", type)
                                        i.setHoverName(Component.literal(type + " shot"))
                                        i.damageValue = i.maxDamage
                                        1
                                    })
                            )
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
                Commands.literal("fastchunklaser")
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
                                    spawnTNTNuke(player, 256, "chunklaser", 1)
                                    player.sendSystemMessage(Component.literal("Fastchunklaser be ready lmao"))
                                } else {
                                    source.sendSuccess({ Component.literal("Fastchunklaser be ready lmao.") }, false)
                                }
                                1
                            }
                    )
            )
            dispatcher.register(
                Commands.literal("fastchunkdel")
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
                                    spawnTNTNuke(player, 49152, "chunkdel", 1)
                                    player.sendSystemMessage(Component.literal("Fastchunkdel be ready lmao"))
                                } else {
                                    source.sendSuccess({ Component.literal("Fastchunkdel be ready lmao.") }, false)
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
                            val tnt = PrimedTntExtendedAPI(EntityType.TNT, world)
                            if (tnt != null) {
                                tnt.setPos(targetPos.x, y, targetPos.z)
                                tnt.fuse = 0
                                tnt.isNoGravity = true
                                tnt.setDamage(100000f)
                                tnt.setDeltaMovement(0.0, 0.0, 0.0)
                                tnt.setExplosionRadius(16.0)
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

                        var placed = 0
                        val spacing = max(1, ((16 * 16 * (maxY - minY)) / total.toDouble()).pow(1.0 / 3).toInt())

                        for (y in maxY - 1 downTo minY step spacing) {
                            for (cx in (chunkX shl 4) until (chunkX shl 4) + 16 step spacing) {
                                for (cz in (chunkZ shl 4) until (chunkZ shl 4) + 16 step spacing) {
                                    if (placed >= total) break
                                    val state = world.getBlockState(BlockPos(cx, y, cz))
                                    if (!state.isAir) {
                                        val tnt = PrimedTntExtendedAPI(EntityType.TNT, world)
                                        if (tnt != null) {
                                            tnt.setPos(cx + 0.5, y + 0.5, cz + 0.5)
                                            tnt.setFuse(0)
                                            tnt.setNoGravity(true)
                                            tnt.setDeltaMovement(0.0, 0.0, 0.0)
                                            tnt.setExplosionRadius(1.0)
                                            tnt.setDamage(1000f)
                                            world.addFreshEntity(tnt)
                                            nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(tnt)
                                            placed++
                                        }
                                    }
                                }
                            }
                        }
                    }


                    "chunklaser" -> {
                        val chunkX = (targetPos.x.toInt()) shr 4
                        val chunkZ = (targetPos.z.toInt()) shr 4
                        val y0 = targetPos.y.toInt()

                        var placed = 0
                        val spacing = max(1, (16 * 16 / total.toDouble()).pow(0.5).toInt())

                        for (cx in (chunkX shl 4) until (chunkX shl 4) + 16 step spacing) {
                            for (cz in (chunkZ shl 4) until (chunkZ shl 4) + 16 step spacing) {
                                if (placed >= total) break
                                val tnt = PrimedTntExtendedAPI(EntityType.TNT, world)
                                if (tnt != null) {
                                    tnt.setPos(cx + 0.5, y0 + 0.5, cz + 0.5)
                                    tnt.setFuse(0)
                                    tnt.setNoGravity(true)
                                    tnt.setDeltaMovement(0.0, 0.0, 0.0)
                                    tnt.setExplosionRadius(1.0)
                                    world.addFreshEntity(tnt)
                                    nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(tnt)
                                    placed++
                                }
                            }
                        }
                    }

                    "nuke" -> {
                        val baseFuse = 80
                        val gravity = -0.03
                        val velocityMultiplier = 1.4
                        val baseRadii = intArrayOf(12, 22, 32, 42, 52, 62, 72, 82, 92, 102)

                        val fallHeight = 0.5 * -gravity * baseFuse * baseFuse
                        val spawnY = targetPos.y + fallHeight

                        var spawned = 0

                        val center = PrimedTntExtendedAPI(EntityType.TNT, world)
                        center.setPos(targetPos.x + 0.5, spawnY, targetPos.z + 0.5)
                        center.setFuse(baseFuse)
                        center.deltaMovement = Vec3(0.0, 0.0, 0.0)
                        world.addFreshEntity(center)
                        nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(center)
                        spawned++

                        while (spawned < total) {
                            for (r in baseRadii) {
                                if (spawned >= total) break
                                val tntsInRing = minOf(100, total - spawned)

                                for (i in 0 until tntsInRing) {
                                    if (spawned >= total) break
                                    val angle = Math.random() * 2.0 * Math.PI
                                    val dx = kotlin.math.cos(angle)
                                    val dz = kotlin.math.sin(angle)
                                    val vx = dx * (r / baseFuse) * velocityMultiplier
                                    val vz = dz * (r / baseFuse) * velocityMultiplier

                                    val tnt = PrimedTntExtendedAPI(EntityType.TNT, world)
                                    tnt.setPos(targetPos.x + 0.5, spawnY, targetPos.z + 0.5)
                                    tnt.setFuse(baseFuse)
                                    tnt.deltaMovement = Vec3(vx, 0.0, vz)
                                    world.addFreshEntity(tnt)
                                    nukeSpawnedEntities.computeIfAbsent(world) { HashSet() }.add(tnt)
                                    spawned++
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
        fun spawnArrowTNTNuke(player: ServerPlayer, tnts: Int?, type: String) {
            val world = player.level() as ServerLevel

            val eyePos = player.getEyePosition(1.0f)
            val lookVec = player.lookAngle
            val end = eyePos.add(lookVec.scale(100000.0))

            val hit = world.clip(ClipContext(
                eyePos, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            ))

            val targetPos = hit?.location ?: end
            val total = tnts?.takeIf { it > 0 } ?: 100

            world.server.execute {
                when (type) {
                    "ArrowNuke" -> {
                        val baseRadii = intArrayOf(12,22,32,42,52,62,72,82,92,102)
                        var spawned = 0

                        var ringIndex = 0
                        while (spawned < total) {
                            val r = if (ringIndex < baseRadii.size) baseRadii[ringIndex] else 98 + (ringIndex - baseRadii.size + 1) * 10
                            val arrowsInRing = when {
                                ringIndex == 0 -> 84
                                ringIndex == 1 -> 50
                                ringIndex in 2..5 -> 70
                                ringIndex == 6 -> 90
                                else -> minOf(100, (r*2 - ringIndex*1.2).toInt())
                            }

                            for (i in 0 until arrowsInRing) {
                                if (spawned >= total) break
                                val angle = Math.random() * 2.0 * Math.PI
                                val dx = kotlin.math.cos(angle)
                                val dz = kotlin.math.sin(angle)
                                val vx = dx * (r / 80.0) * 1.4
                                val vz = dz * (r / 80.0) * 1.4

                                val arrow = Arrow(world, player)
                                arrow.setPos(targetPos.x, 20.0, targetPos.z)
                                arrow.deltaMovement = Vec3(vx, 0.0, vz)
                                arrow.isNoGravity = false
                                arrow.pierceLevel = 127.toByte()
                                arrow.isCritArrow = true
                                arrow.pickup = AbstractArrow.Pickup.DISALLOWED
                                world.addFreshEntity(arrow)
                                spawned++
                            }

                            ringIndex++
                        }
                    }

                    "ArrowStab" -> {
                        val freezeY = 50.0

                        val arrow = Arrow(world, targetPos.x, freezeY, targetPos.z)
                        arrow.isNoGravity = true
                        arrow.deltaMovement = Vec3.ZERO
                        arrow.pierceLevel = 127.toByte()
                        arrow.isCritArrow = true
                        arrow.pickup = AbstractArrow.Pickup.DISALLOWED
                        world.addFreshEntity(arrow)

                        world.server.execute {
                            arrow.isNoGravity = false
                            arrow.deltaMovement = Vec3(0.0, (-Integer.MAX_VALUE).toDouble(), 0.0)
                        }
                    }
                }


                player.sendSystemMessage(
                    Component.literal("Arrow TNT Nuke launched! Type: $type, Count: $total")
                )
            }
        }

        fun summonWolves(player: ServerPlayer, amount: Int) {
            val level = player.serverLevel()

            val x = player.getX()
            val y = player.getY()
            val z = player.getZ()

            for (i in 0..<amount) {
                val wolf = Wolf(EntityType.WOLF, level)

                wolf.setPos(x, y, z)
                wolf.tame(player)
                wolf.ownerUUID = player.getUUID()

                wolf.addEffect(MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1, false, true))
                wolf.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 1, false, true))
                wolf.addEffect(MobEffectInstance(MobEffects.REGENERATION, 9600, 0, false, true))

                val angle = Math.random() * Math.PI * 2.0
                val pitch = (Math.random() - 0.5) * Math.PI * 0.5
                val horizontalSpeed = 0.8

                val vx = cos(angle) * cos(pitch) * horizontalSpeed
                val vy = sin(pitch) * 0.6 + 0.3
                val vz = sin(angle) * cos(pitch) * horizontalSpeed

                wolf.setDeltaMovement(vx, vy, vz)
                level.addFreshEntity(wolf)
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
