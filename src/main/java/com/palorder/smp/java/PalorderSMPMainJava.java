package com.palorder.smp.java;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSource;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

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
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "palordersmp_tweaked");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "palordersmp_tweaked");
    public static final Map<Integer, List<Runnable>> scheduled = new HashMap<>();

    public static void runLater(ServerLevel world, int ticks, Runnable r) {
        int targetTick = (int) (world.getGameTime() + ticks);
        scheduled.computeIfAbsent(targetTick, k -> new ArrayList<>()).add(r);
    }


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
    public static Random rand = new Random();
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
        if (ModList.get().isLoaded("computercraft")) {
            logger.info("ComputerCraft is installed, Registering addon stuff. (non-existent lmao)");
        } else {
            logger.warn("ComputerCraft is NOT present!");
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
    }
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        scheduler.shutdown();
    }
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        long time = e.getServer().overworld().getGameTime();
        List<Runnable> list = scheduled.remove((int) time);
        if (list != null) for (Runnable r : list) r.run();
    }

    // ---------------- Commands ----------------
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("orbital")
                .requires(source -> {
                    try {
                        ServerPlayer player = source.getPlayer();
                        if (player != null) {
                            return player.getGameProfile().getId().equals(OWNER_UUID)
                                    || player.getGameProfile().getId().equals(OWNER_UUID2)
                                    || "dev".equalsIgnoreCase(player.getName().getString());
                        }
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                            if (target == null) return 0;
                            UUID id = target.getGameProfile().getId();
                            if (!(id.equals(OWNER_UUID) || id.equals(DEV_UUID) || id.equals(OWNER_UUID2))) return 0;
                            if (nukePendingConfirmation.contains(id)) {
                                target.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"));
                            } else {
                                nukePendingConfirmation.add(id);
                                target.sendSystemMessage(Component.literal("Type /orbitalConfirm to spawn 2000 TNT packed in one block."));
                                scheduler.schedule(() -> nukePendingConfirmation.remove(id), 30, TimeUnit.SECONDS);
                            }
                            return 1;
                        }))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                    if (target != null) {
                        UUID id = target.getGameProfile().getId();
                        if (nukePendingConfirmation.contains(id)) {
                            target.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"));
                        } else {
                            nukePendingConfirmation.add(id);
                            target.sendSystemMessage(Component.literal("Type /orbitalConfirm <ARGS HERE> \n have fun dominating the server :3"));
                            scheduler.schedule(() -> nukePendingConfirmation.remove(id), 30, TimeUnit.SECONDS);
                        }
                    } else {
                        source.sendSuccess(() -> Component.literal("Command executed."), false);
                    }
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("orbitalConfirm")
                .requires(source -> {
                    try {
                        ServerPlayer player = source.getPlayer();
                        if (player != null) {
                            return player.getGameProfile().getId().equals(OWNER_UUID)
                                    || player.getGameProfile().getId().equals(OWNER_UUID2)
                                    || "dev".equalsIgnoreCase(player.getName().getString());
                        }
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests((ctx, builder) ->
                                                net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("nuke", "stab"), builder))
                                        .then(Commands.argument("layers", IntegerArgumentType.integer(1, 5000))
                                                .executes(context -> {
                                                    CommandSourceStack source = context.getSource();
                                                    ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
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
                        ServerPlayer player = source.getPlayer();
                        if (player != null) {
                            return player.getGameProfile().getId().equals(OWNER_UUID)
                                    || player.getGameProfile().getId().equals(OWNER_UUID2)
                                    || "dev".equalsIgnoreCase(player.getName().getString());
                        }
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                        if (player != null) {
                            spawnTNTNuke(player, 500, "nuke", 5000);
                            player.sendSystemMessage(Component.literal("Fastorbitaled be ready lmao"));
                        }
                        else {
                            context.getSource().sendSuccess(() -> Component.literal("Fastorbitaled be ready lmao."), false);
                        }
                        return 1;
                    }))
            );
        dispatcher.register(Commands.literal("faststab")
                .requires(source -> {
                    try {
                        ServerPlayer player = source.getPlayer();
                        if (player != null) {
                            return player.getGameProfile().getId().equals(OWNER_UUID)
                                    || player.getGameProfile().getId().equals(OWNER_UUID2)
                                    || "dev".equalsIgnoreCase(player.getName().getString());
                        }
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                            if (player != null) {
                                spawnTNTNuke(player, 900, "stab", 1);
                                player.sendSystemMessage(Component.literal("Faststabbed be ready lmao"));
                            }
                            else {
                                context.getSource().sendSuccess(() -> Component.literal("Faststabbed be ready lmao."), false);
                            }
                            return 1;
                        }))
        );
        dispatcher.register(Commands.literal("loadallchunks")
                .requires(source -> {
                    try {
                        ServerPlayer player = source.getPlayer();
                        if (player != null) {
                            return player.getGameProfile().getId().equals(OWNER_UUID)
                                    || player.getGameProfile().getId().equals(OWNER_UUID2)
                                    || "dev".equalsIgnoreCase(player.getName().getString());
                        }
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer player = source.getPlayer();
                    ServerLevel world;
                    if (player != null) world = player.serverLevel();
                    else world = source.getLevel();

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
                                ChunkPos cp = new ChunkPos(e.blockPosition());
                                if (chunks.contains(cp)) iterator.remove();
                            }
                        }

                        chunks.clear();

                        if (player != null)
                            player.sendSystemMessage(Component.literal("All frozen chunks reloaded!"));
                        else
                            source.sendSuccess(() -> Component.literal("All frozen chunks reloaded!"), false);
                    }
                    return 1;
                })
        );
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
        int extraExplosions = 0;
        double spawnHeight = targetPos.y + 30;
        double layerStepY = 1.0;
        double spacing = 1.5;

        int totalTNT = (tnts != null && tnts > 0) ? tnts : 300;
        int layersFinal = layers;

        world.getServer().execute(() -> {
            int spawned = 0;

            if ("stab".equals(type)) {
                for (int i = 0; i < totalTNT; i++) {
                    PrimedTnt tnt = EntityType.TNT.create(world);
                    if (tnt != null) {
                        tnt.setPos(targetPos.x, targetPos.y, targetPos.z);
                        tnt.setFuse(100 + rand.nextInt(5));
                        world.addFreshEntity(tnt);
                        nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                    }
                }

            } else if ("nuke".equals(type)) {

                for (int layer = 0; layer < layersFinal && spawned < totalTNT; layer++) {
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
                                tnt.setFuse(100);
                                world.addFreshEntity(tnt);
                                nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                                spawned++;
                            }
                        }
                    }
                }

                int extraCount = totalTNT * extraExplosions;
                int extraSpawned = 0;
                for (int layer = 0; layer < layersFinal && extraSpawned < extraCount; layer++) {
                    double y = spawnHeight + layer * layerStepY;

                    for (int ring = 1; extraSpawned < extraCount && ring <= 5; ring++) {
                        double radius = ring * 3.0 * (layer + 1);
                        int explosionsInRing = (int) Math.floor((2 * Math.PI * radius) / spacing);

                        for (int i = 0; i < explosionsInRing && extraSpawned < extraCount; i++) {
                            double angle = 2 * Math.PI * i / explosionsInRing;
                            double x = targetPos.x + Math.cos(angle) * radius;
                            double z = targetPos.z + Math.sin(angle) * radius;

                            double fx = x;
                            double fz = z;
                            double fy = y;

                            runLater(world, 0, () -> {
                                world.explode(null, fx, fy, fz, 5.0f, Level.ExplosionInteraction.TNT);
                            });

                            extraSpawned++;
                        }
                    }
                }

            } else if (type == null) {
                IllegalArgumentException ex = new IllegalArgumentException("type cant be nothing, how did you do this.");
                logger.error("Invalid argument encountered", ex);
            }

            player.sendSystemMessage(Component.literal(
                    "Orbital strike launched! Total TNT: " + totalTNT + ", Type: " + type +
                            ", Extra explosions: " + (totalTNT * extraExplosions)
            ));
        });
    }

    // ---------------- Derender TNT safely ----------------
    /**
     * @deprecated This method is unsafe in the main thread, separate it from the main thread or just don't use it, DO NOT USE!
     */
    @Deprecated(since = "UNSAFE", forRemoval = true)
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
