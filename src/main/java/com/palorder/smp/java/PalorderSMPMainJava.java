package com.palorder.smp.java;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.ModList;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Core entry point
import dan200.computercraft.api.ComputerCraftAPI;  // :contentReference[oaicite:1]{index=1}


// Lua‑API interfaces
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaFunction;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaTable;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.api.lua.MethodResult;  // :contentReference[oaicite:2]{index=2}


// Peripheral / Computer interfaces
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;  // :contentReference[oaicite:3]{index=3}


// Turtle / upgrade interfaces
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;  // :contentReference[oaicite:4]{index=4}


// Filesystem / mounts
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;  // :contentReference[oaicite:5]{index=5}


// Detail providers & registries (for item/block detail exposed to computers)
import dan200.computercraft.api.detail.DetailProvider;
import dan200.computercraft.api.detail.DetailRegistry;  // :contentReference[oaicite:6]{index=6}
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("palordersmp_tweaked")
@Mod.EventBusSubscriber(modid = "palordersmp_tweaked", value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PalorderSMPMainJava {

    // ---------------- Deferred Registers ----------------
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "palordersmp_tweaked");
    @Deprecated(forRemoval = true, since = "rewritten")
    public static final RegistryObject<Item> deathban_revive =
            ITEMS.register("deathban_revive", () -> new Item(new Item.Properties()));
    @Deprecated(forRemoval = true, since = "rewritten")
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "palordersmp_tweaked");
    @Deprecated(forRemoval = true, since = "rewritten")
    public static final RegistryObject<SoundEvent> REVENGE_SOUND_EVENT =
            SOUND_EVENTS.register("revenge", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("palordersmp_tweaked", "revenge")));
    public static final Logger logger = LogManager.getLogger(PalorderSMPMainJava.class);
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
    private static final Logger log = LogManager.getLogger(PalorderSMPMainJava.class);

    static {
        chatItemRewards.put("gimme natherite blocks ples", new ItemStack(Items.NETHERITE_BLOCK, 64));
        chatItemRewards.put("i need food ples give me food 2 stacks ples", new ItemStack(Items.GOLDEN_CARROT, 128));
        chatItemRewards.put("gimme natherite blocks ples adn i want 2 stacks ples", new ItemStack(Items.NETHERITE_BLOCK, 128));
        chatItemRewards.put("i need food ples give me food ples", new ItemStack(Items.GOLDEN_CARROT, 64));
    }
    public @interface hello {

    }
    public PalorderSMPMainJava() {
        // Register this class to the Forge event bus
        MinecraftForge.EVENT_BUS.register(this);
    }
    public static Path getWorldFolder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT);
    }

    private void injectModsConfigCC(MinecraftServer server) throws Exception {
        // Get the actual world folder safely
        Path worldFolder = server.getWorldPath(LevelResource.ROOT);

        // Path to serverconfig
        Path configPath = worldFolder.resolve("serverconfig/computercraft-server.toml");
        if (!Files.exists(configPath)) return;

        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();

            config.set("computer_space_limit", 1073741824);
            config.set("floppy_space_limit", 1073741824);
            config.set("upload_max_size", 524288);
            config.set("maximum_open_files", 128);
            config.set("default_computer_settings", "");
            config.set("log_computer_errors", true);
            config.set("command_require_creative", true);
            config.set("disabled_generic_methods", new ArrayList<>());

            config.set("execution.computer_threads", 1);
            config.set("execution.max_main_global_time", 10);
            config.set("execution.max_main_computer_time", 5);

            config.set("http.enabled", true);
            config.set("http.websocket_enabled", true);
            config.set("http.max_requests", 100);
            config.set("http.max_websockets", 100);
            config.set("http.bandwidth.global_download", 1073741824);
            config.set("http.bandwidth.global_upload", 1073741824);
            config.set("http.proxy.type", "HTTP");
            config.set("http.proxy.host", "");
            config.set("http.proxy.port", 8080);

            List<Object> httpRules = new ArrayList<>();
            Map<String, Object> rule1 = new HashMap<>();
            rule1.put("host", "$private");
            rule1.put("action", "deny");
            httpRules.add(rule1);

            Map<String, Object> rule2 = new HashMap<>();
            rule2.put("host", "*");
            rule2.put("action", "allow");
            rule2.put("max_download", 16777216);
            rule2.put("max_upload", 4194304);
            rule2.put("max_websocket_message", 131072);
            rule2.put("use_proxy", false);
            httpRules.add(rule2);

            config.set("http.rules", httpRules);

            config.set("peripheral.command_block_enabled", true);
            config.set("peripheral.modem_range", 64);
            config.set("peripheral.modem_high_altitude_range", 384);
            config.set("peripheral.modem_range_during_storm", 64);
            config.set("peripheral.modem_high_altitude_range_during_storm", 384);
            config.set("peripheral.max_notes_per_tick", 8);
            config.set("peripheral.monitor_bandwidth", 1000000);

            config.set("turtle.need_fuel", false);
            config.set("turtle.normal_fuel_limit", 20000);
            config.set("turtle.advanced_fuel_limit", 100000);
            config.set("turtle.can_push", true);

            config.set("term_sizes.computer.width", 51);
            config.set("term_sizes.computer.height", 19);

            config.set("term_sizes.pocket_computer.width", 26);
            config.set("term_sizes.pocket_computer.height", 20);

            config.set("term_sizes.monitor.width", 8);
            config.set("term_sizes.monitor.height", 6);

            config.save();
        }
    }

    // ---------------- Chat Item Rewards ----------------
    @SubscribeEvent
    public static void handleChatItemRequests(ServerChatEvent event) {
        String message = event.getMessage().getString();
        ServerPlayer player = event.getPlayer();
        if (chatItemRewards.containsKey(message)) {
            player.getInventory().add(chatItemRewards.get(message));
        }

    }

    // ---------------- Server Events ----------------
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        registerCommands(event.getServer().getCommands().getDispatcher());
        MinecraftServer server = event.getServer();
        if (ModList.get().isLoaded("computercraft")) {
            try {
                injectModsConfigCC(server);
            } catch (Exception e) {
                logger.warn("Failed to inject configuration into: [computercraft] \n please find a compatible version.");
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        scheduler.shutdown();
    }

    // ---------------- Commands ----------------
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("orbital")
                .requires(source -> {
                    try {
                        var player = source.getPlayerOrException();
                        return player.getGameProfile().getId().equals(OWNER_UUID)
                                || "dev".equalsIgnoreCase(player.getName().getString()) || player.getGameProfile().getId().equals(OWNER_UUID2);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                            if (player == null) return 0;
                            UUID playerId = player.getGameProfile().getId();
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
                    UUID playerId = player.getGameProfile().getId();
                    if (nukePendingConfirmation.contains(playerId)) {
                        player.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"));
                    } else {
                        nukePendingConfirmation.add(playerId);
                        player.sendSystemMessage(Component.literal("Type /orbitalConfirm to spawn 2000 TNT packed in one block."));
                        scheduler.schedule(() -> nukePendingConfirmation.remove(playerId), 30, TimeUnit.SECONDS);
                    }
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("orbitalConfirm")
                .requires(source -> {
                    try {
                        var player = source.getPlayerOrException();
                        return player.getGameProfile().getId().equals(OWNER_UUID)
                                || "dev".equalsIgnoreCase(player.getName().getString()) || player.getGameProfile().getId().equals(OWNER_UUID2);
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
                                                    if (nukePendingConfirmation.remove(player.getGameProfile().getId()))
                                                        spawnTNTNuke(player, tntCount, type, layers);
                                                    return 1;
                                                }))))));
        dispatcher.register(Commands.literal("fastorbital")
                .requires(source -> {
                    try {
                        var player = source.getPlayerOrException();
                        return player.getGameProfile().getId().equals(OWNER_UUID)
                                || "dev".equalsIgnoreCase(player.getName().getString()) || player.getGameProfile().getId().equals(OWNER_UUID2);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    spawnTNTNuke(player,500,"nuke",10);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("loadallchunks")
                .requires(source -> {
                    try {
                        var player = source.getPlayerOrException();
                        return player.getGameProfile().getId().equals(OWNER_UUID)
                                || "dev".equalsIgnoreCase(player.getName().getString()) || player.getGameProfile().getId().equals(OWNER_UUID2);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel world = player.serverLevel();

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
                                if (chunks.contains(eChunkPos)) {
                                    iterator.remove();
                                }
                            }
                        }

                        chunks.clear();
                        player.sendSystemMessage(Component.literal("All frozen chunks reloaded!"));
                    }
                    return 1;
                }));
    }

    // ---------------- Nuke Spawn ----------------
    public static void spawnTNTNuke(ServerPlayer player, Integer tnts, String type, Integer layers) {
        ServerLevel world = (ServerLevel) player.level();

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 end = eyePos.add(lookVec.scale(100000.0));
        BlockHitResult hitResult = world.clip(new ClipContext(
                eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        Vec3 targetPos = hitResult != null ? hitResult.getLocation() : end;

        nukePlayerTeleportBack.put(player.getGameProfile().getId(), player.position());

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

        if (type.equals("stab")) {

            for (int i = 0; i < totalTNT; i++) {
                PrimedTnt tnt = EntityType.TNT.create(world);
                if (tnt != null) {
                    tnt.setPos(targetPos.x, player.getY(), targetPos.z);
                    tnt.setFuse(60 + rand.nextInt(20));
                    world.addFreshEntity(tnt);
                    nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                }
            }

        } else if (type.equals("nuke")) {

            for (int layer = 0; layer < layers && spawned < totalTNT; layer++) {

                double y = spawnHeight + layer * layerStepY;

                for (int ring = 1; spawned < totalTNT && ring <= 5; ring++) {

                    // PATCH: radius now increases with layer too
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

            IllegalArgumentException ex =
                    new IllegalArgumentException("type cant be nothing, how did you do this.");
            logger.error("Invalid argument encountered", ex);
        }
        player.sendSystemMessage(Component.literal(
                "Orbital strike launched! Total TNT: " + totalTNT + ", Type: " + type
        ));
    }

    // ---------------- Derender TNT safely ----------------
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (!(event.level instanceof ServerLevel world)) return;

        Set<ChunkPos> frozen = pausedChunks.get(world);
        if (frozen == null || frozen.isEmpty()) return;

        Set<Entity> entities = nukeSpawnedEntities.get(world);
        if (entities == null || entities.isEmpty()) return;

        Iterator<Entity> iterator = entities.iterator();
        int processedPerTick = 100; // adjustable for server performance
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

        // Clean up empty maps
        if (entities.isEmpty()) nukeSpawnedEntities.remove(world);
        if (frozen.isEmpty()) pausedChunks.remove(world);
    }
}
