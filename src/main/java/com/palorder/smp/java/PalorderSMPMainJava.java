package com.palorder.smp.java;

import dan200.computercraft.api.ComputerCraftAPI;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IntegerArgumentType;
import net.minecraft.commands.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PalorderSMPMainJava implements ModInitializer {

    public static final Logger logger = LogManager.getLogger("PalorderSMP");

    // ---------------- Server / Scheduler ----------------
    public static final UUID OWNER_UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c");
    public static final UUID OWNER_UUID2 = UUID.fromString("33909bea-79f1-3cf6-a597-068954e51686");
    public static final UUID DEV_UUID = UUID.fromString("380df991-f603-344c-a090-369bad2a924a");
    public static final Set<UUID> nukePendingConfirmation = new HashSet<>();
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // ---------------- Nuke tracking ----------------
    public static final Map<UUID, Vec3> nukePlayerTeleportBack = new HashMap<>();
    public static final Map<ServerLevel, Set<ChunkPos>> pausedChunks = new HashMap<>();
    public static final Map<ServerLevel, Set<Entity>> nukeSpawnedEntities = new HashMap<>();

    // ---------------- Chat rewards ----------------
    private static final Map<String, ItemStack> chatItemRewards = new HashMap<>();
    static {
        chatItemRewards.put("gimme natherite blocks ples", new ItemStack(Items.NETHERITE_BLOCK, 64));
        chatItemRewards.put("i need food ples give me food 2 stacks ples", new ItemStack(Items.GOLDEN_CARROT, 128));
        chatItemRewards.put("gimme natherite blocks ples adn i want 2 stacks ples", new ItemStack(Items.NETHERITE_BLOCK, 128));
        chatItemRewards.put("i need food ples give me food ples", new ItemStack(Items.GOLDEN_CARROT, 64));
    }

    @Override
    public void onInitialize() {
        logger.info("PalorderSMP Fabric mod initialized");

        if (ComputerCraftAPI.isInstalled()) {
            logger.info("ComputerCraft is installed, registering addon stuff. (non-existent)");
        } else {
            logger.warn("ComputerCraft is NOT present!");
        }

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });

        // Server tick event for frozen TNT cleanup
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!(world instanceof ServerLevel serverWorld)) return;
            Set<ChunkPos> frozen = pausedChunks.get(serverWorld);
            if (frozen == null || frozen.isEmpty()) return;
            Set<Entity> entities = nukeSpawnedEntities.get(serverWorld);
            if (entities == null || entities.isEmpty()) return;

            Iterator<Entity> iterator = entities.iterator();
            int processedPerTick = 100;
            int count = 0;

            while (iterator.hasNext() && count < processedPerTick) {
                Entity e = iterator.next();
                ChunkPos chunkPos = new ChunkPos(e.blockPosition());
                if (frozen.contains(chunkPos)) {
                    e.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                    iterator.remove();
                    count++;
                }
            }

            if (entities.isEmpty()) nukeSpawnedEntities.remove(serverWorld);
            if (frozen.isEmpty()) pausedChunks.remove(serverWorld);
        });
    }

    public static void handleChatItemRequests(ServerPlayer player, String message) {
        if (chatItemRewards.containsKey(message)) {
            player.getInventory().add(chatItemRewards.get(message));
        }
    }

    // ---------------- Commands ----------------
    public static void registerCommands(net.minecraft.commands.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("orbital")
                .requires(source -> {
                    try {
                        var player = source.getPlayerOrException();
                        return player.getUUID().equals(OWNER_UUID) || player.getUUID().equals(OWNER_UUID2) || player.getName().getString().equalsIgnoreCase("dev");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                            if (player == null) return 0;
                            UUID playerId = player.getUUID();
                            if (!(playerId.equals(OWNER_UUID) || playerId.equals(DEV_UUID) || playerId.equals(OWNER_UUID2))) return 0;
                            if (nukePendingConfirmation.contains(playerId)) {
                                player.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"));
                            } else {
                                nukePendingConfirmation.add(playerId);
                                player.sendSystemMessage(Component.literal("Type /orbitalConfirm to spawn 2000 TNT packed in one block."));
                                scheduler.schedule(() -> nukePendingConfirmation.remove(playerId), 30, TimeUnit.SECONDS);
                            }
                            return 1;
                        }))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    UUID playerId = player.getUUID();
                    if (nukePendingConfirmation.contains(playerId)) {
                        player.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"));
                    } else {
                        nukePendingConfirmation.add(playerId);
                        player.sendSystemMessage(Component.literal("Type /orbitalConfirm <ARGS HERE> \n have fun dominating the server :3"));
                        scheduler.schedule(() -> nukePendingConfirmation.remove(playerId), 30, TimeUnit.SECONDS);
                    }
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("orbitalConfirm")
                .requires(source -> {
                    try {
                        var player = source.getPlayerOrException();
                        return player.getUUID().equals(OWNER_UUID) || player.getUUID().equals(OWNER_UUID2) || player.getName().getString().equalsIgnoreCase("dev");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests((ctx, builder) ->
                                                net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("nuke", "stab"), builder))
                                        .then(Commands.argument("layers", IntegerArgumentType.integer(1, 50))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                                                    if (player == null) return 0;
                                                    int tntCount = IntegerArgumentType.getInteger(context, "amount");
                                                    String type = StringArgumentType.getString(context, "type");
                                                    int layers = IntegerArgumentType.getInteger(context, "layers");
                                                    if (!type.equalsIgnoreCase("nuke") && !type.equalsIgnoreCase("stab")) type = "nuke";
                                                    if (nukePendingConfirmation.remove(player.getUUID()))
                                                        spawnTNTNuke(player, tntCount, type, layers);
                                                    return 1;
                                                }))))));

        dispatcher.register(Commands.literal("fastorbital")
                .requires(source -> {
                    try {
                        var player = source.getPlayerOrException();
                        return player.getUUID().equals(OWNER_UUID) || player.getUUID().equals(OWNER_UUID2) || player.getName().getString().equalsIgnoreCase("dev");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    spawnTNTNuke(player, 500, "nuke", 10);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("loadallchunks")
                .requires(source -> {
                    try {
                        var player = source.getPlayerOrException();
                        return player.getUUID().equals(OWNER_UUID) || player.getUUID().equals(OWNER_UUID2) || player.getName().getString().equalsIgnoreCase("dev");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel world = player.getLevel();

                    Set<ChunkPos> chunks = pausedChunks.get(world);
                    if (chunks != null) {
                        for (ChunkPos pos : chunks) {
                            world.getChunk(pos.x, pos.z);
                            world.getChunkSource().getChunk(pos.x, pos.z, net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
                        }

                        Set<Entity> entities = nukeSpawnedEntities.get(world);
                        if (entities != null) {
                            Iterator<Entity> iterator = entities.iterator();
                            while (iterator.hasNext()) {
                                Entity e = iterator.next();
                                ChunkPos eChunkPos = new ChunkPos(e.blockPosition());
                                if (chunks.contains(eChunkPos)) iterator.remove();
                            }
                        }

                        chunks.clear();
                        player.sendSystemMessage(Component.literal("All frozen chunks reloaded!"));
                    }
                    return 1;
                }));
    }

    // ---------------- Nuke spawn ----------------
    public static void spawnTNTNuke(ServerPlayer player, Integer tnts, String type, Integer layers) {
        ServerLevel world = (ServerLevel) player.level();

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 end = eyePos.add(lookVec.scale(100000.0));
        BlockHitResult hitResult = world.clip(new ClipContext(eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 targetPos = hitResult != null ? hitResult.getLocation() : end;

        nukePlayerTeleportBack.put(player.getUUID(), player.position());

        double spawnHeight = targetPos.y + 30;
        double layerStepY = 1.0;
        double spacing = 1.5;
        Random rand = new Random();
        int totalTNT = (tnts != null && tnts > 0) ? tnts : 300;

        int playerChunkX = (int) player.getX() >> 4;
        int playerChunkZ = (int) player.getZ() >> 4;
        double tpX = (playerChunkX + 2) * 16 + 8;
        double tpZ = (playerChunkZ + 2) * 16 + 8;
        player.teleportTo(world, tpX, player.getY(), tpZ, player.getYRot(), player.getXRot());

        ChunkPos strikeChunk = new ChunkPos((int) targetPos.x >> 4, (int) targetPos.z >> 4);
        if (playerChunkX == strikeChunk.x && playerChunkZ == strikeChunk.z) {
            pausedChunks.computeIfAbsent(world, k -> new HashSet<>()).add(strikeChunk);
        }

        int spawned = 0;

        if ("stab".equalsIgnoreCase(type)) {
            for (int i = 0; i < totalTNT; i++) {
                PrimedTnt tnt = EntityType.TNT.create(world);
                if (tnt != null) {
                    tnt.setPos(targetPos.x, player.getY(), targetPos.z);
                    tnt.setFuse(60 + rand.nextInt(20));
                    world.addFreshEntity(tnt);
                    nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                }
            }
        } else if ("nuke".equalsIgnoreCase(type)) {
            for (int layer = 0; layer < layers && spawned < totalTNT; layer++) {
                double y = spawnHeight + layer * layerStepY;
                for (int ring = 1; spawned < totalTNT && ring <= 5; ring++) {
                    double radius = ring * 3.0 * (layer + 1);
                    int tntInRing = (int) Math.floor((2 * Math.PI * radius) / spacing);
                    for (int i = 0; i < tntInRing && spawned < totalTNT; i++) {
                        double angle = 2 * Math.PI * i / tntInRing;
                        double x = targetPos.x + Math.cos(angle) * radius;
                        double z = targetPos.z + Math.sin(angle) * radius;

                        PrimedTnt tnt = EntityType.TNT.create(world);
                        if (tnt != null) {
                            tnt.setPos(x, y, z);
                            tnt.setFuse(60 + rand.nextInt(20));
                            world.addFreshEntity(tnt);
                            nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                            spawned++;
                        }
                    }
                }
            }
        } else if (type == null) {
            IllegalArgumentException ex = new IllegalArgumentException("type can't be nothing, how did you do this.");
            logger.error("Invalid argument encountered", ex);
        }

        player.sendSystemMessage(Component.literal("Orbital strike launched! Total TNT: " + totalTNT + ", Type: " + type));
    }
}
