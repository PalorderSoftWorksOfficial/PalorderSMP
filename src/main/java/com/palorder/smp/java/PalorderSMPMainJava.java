package com.palorder.smp.java;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

@Mod("palordersmp_tweaked")
@Mod.EventBusSubscriber(modid = "palordersmp_tweaked", value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PalorderSMPMainJava {
    public static final Map<Integer, List<Runnable>> scheduled = new HashMap<>();

    public static void runLater(ServerLevel world, int ticks, Runnable r) {
        int targetTick = (int) (world.getGameTime() + ticks);
        scheduled.computeIfAbsent(targetTick, k -> new ArrayList<>()).add(r);
    }

    public static final Logger logger =
            LoggerFactory.getLogger(PalorderSMPMainJava.class);

    private static final Logger log =
            LoggerFactory.getLogger(PalorderSMPMainJava.class);

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
    private static final Integer winintlimit = Integer.MAX_VALUE;
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
    // client setup
    @OnlyIn(Dist.CLIENT)
    private void clientSetup(FMLClientSetupEvent event) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Admin panel");
            frame.setSize(400, 200);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JLabel label = new JLabel("Haiii, FUCK FUCVKAKOHJOEROIHHEAI9ORHEORNIO FUCK CHATGPT I CANT MAKE THIS SHIT BECAUSE THAT STUPID ASS NIGGER WONT DO SHIT FUCK YOU CHATGPT", SwingConstants.CENTER);
            frame.add(label);
            frame.setVisible(true);
        });
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
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (e.getLevel().isClientSide()) return;

        ItemStack s = e.getItemStack();
        if (!(s.getItem() instanceof FishingRodItem)) return;

        CompoundTag t = s.getOrCreateTag();
        if (!t.contains("RodType")) return;
        String type = t.getString("RodType");

        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ServerLevel world = p.serverLevel();

        int rodUse = t.getInt("RodUse") + 1;
        t.putInt("RodUse", rodUse);

        if (rodUse == 1) {
            runLater(world, 130, () -> {
                if (s.hasTag()) s.getTag().putInt("RodUse", 0);
            });
            return;
        }

        if (rodUse < 2) return;

        if ("void".equals(type)) {
            if (!t.contains("Voidrodowner")) {
                t.putInt("RodUse", 0);
                return;
            }

            if (!t.getString("Voidrodowner").equals(p.getUUID().toString())) {
                t.putInt("RodUse", 0);
                return;
            }

            FishingHook hook = p.fishing;
            if (hook == null) {
                t.putInt("RodUse", 0);
                return;
            }

            if (!(hook.getHookedIn() != null && hook.getHookedIn() instanceof ServerPlayer target)) {
                t.putInt("RodUse", 0);
                return;
            }

            runLater(world, 20, () -> {
                if (!target.isAlive()) return;
                target.teleportTo(
                        world,
                        target.getX(),
                        -64.0,
                        target.getZ(),
                        target.getYRot(),
                        target.getXRot()
                );
            });

            t.putInt("RodUse", 0);
            return;
        }

        int amount = switch (type) {
            case "stab" -> 1800;
            case "ArrowStab" -> 1000;
            case "nuke", "ArrowNuke" -> 775;
            case "nuke_2" -> 1000;
            case "chunklaser" -> 256;
            case "chunkdel" -> 49152;
            case "Wolf" -> 150;
            default -> 0;
        };

        int layers = switch (type) {
            case "stab", "chunklaser", "chunkdel", "ArrowStab" -> 1;
            case "nuke" -> 0;
            case "nuke_2" -> 0;
            case "Wolf" -> 150;
            default -> 0;
        };

        runLater(world, 10, () -> {
            if (!p.isAlive()) return;
            if ("ArrowNuke".equals(type) || "ArrowStab".equals(type)) {
                spawnArrowTNTNuke(p, amount, type);
            } else if ("Wolf".equals(type)) {
                summonWolves(p, amount);
            } else if ("nuke_2".equals(type)) {
                spawnTNTNuke(p, amount, "nuke", layers);
            } else {
                spawnTNTNuke(p, amount, type, layers);
            }
            t.putInt("RodUse", 0);
        });
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
                                                net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("nuke", "stab", "chunklaser", "chunkdel","ArrowNuke","ArrowStab","void","Wolf","nuke_2"), builder))
                                        .then(Commands.argument("layers", IntegerArgumentType.integer(1, 5000))
                                                .executes(context -> {
                                                    CommandSourceStack source = context.getSource();
                                                    ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                                                    if (player == null) return 0;
                                                    int tntCount = IntegerArgumentType.getInteger(context, "amount");
                                                    String type = StringArgumentType.getString(context, "type");
                                                    int layers = IntegerArgumentType.getInteger(context, "layers");
                                                    if (!type.equalsIgnoreCase("nuke") && !type.equalsIgnoreCase("stab") && !type.equalsIgnoreCase("chunklaser") && !type.equalsIgnoreCase("chunkdel")) type = "nuke";
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
                            spawnTNTNuke(player, 775, "nuke", 0);
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
        dispatcher.register(Commands.literal("linkfrod")
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
                        .then(Commands.argument("type", StringArgumentType.string())
                                .suggests((ctx, builder) ->
                                        net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("nuke", "stab","chunklaser","chunkdel","ArrowNuke","ArrowStab","void","Wolf","nuke_2"), builder))
                                .executes(context -> {
                                    ServerPlayer p = context.getSource().getPlayer();
                                    String type = StringArgumentType.getString(context, "type");
                                    assert p != null;
                                    ItemStack i = p.getMainHandItem();
                                    if (!(i.getItem() instanceof FishingRodItem)) return 0;
                                    i.getOrCreateTag().putString("RodType", type);
                                    i.setHoverName(Component.literal(type + " shot"));
                                    if (!type.equals("void")) {
                                        i.setDamageValue(i.getMaxDamage());
                                    } else {
                                        i.setHoverName(Component.literal("Stasis rod"));
                                        i.getOrCreateTag().putString("Voidrodowner",p.getStringUUID());
                                    }
                                    return 1;
                                })
                        )));
        dispatcher.register(Commands.literal("fastchunklaser")
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
                                spawnTNTNuke(player, 256, "chunklaser", 1);
                                player.sendSystemMessage(Component.literal("Fastchunklasered be ready lmao"));
                            }
                            else {
                                context.getSource().sendSuccess(() -> Component.literal("Fastchunklasered be ready lmao."), false);
                            }
                            return 1;
                        }))
        );
        dispatcher.register(Commands.literal("fastchunkdel")
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
                                spawnTNTNuke(player, 49152, "chunkdel", 1);
                                player.sendSystemMessage(Component.literal("fastchunkdeleted be ready lmao"));
                            }
                            else {
                                context.getSource().sendSuccess(() -> Component.literal("fastchunkdeleted be ready lmao."), false);
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
    /*
    Notice:
    Please use PrimedTntExtendedAPI tnt = new PrimedTntExtendedAPI(EntityType.TNT, world);
    Instead of PrimedTnt tnt = EntityType.TNT.create(world); when adding a new type as It has ExtendedAPI like setDamage, setDamageForEntityType

     */

    // ---------------- Nuke Spawn ----------------
    public static void spawnTNTNuke(ServerPlayer player, Integer tnts, String type, Integer layers) {
        ServerLevel world = (ServerLevel) player.level();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 end = eyePos.add(lookVec.scale(100000.0));
        BlockHitResult hitResult = world.clip(new ClipContext(eyePos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 targetPos = hitResult != null ? hitResult.getLocation() : end;
        nukePlayerTeleportBack.put(player.getGameProfile().getId(), player.position());
        int total = (tnts != null && tnts > 0) ? tnts : 1;
        int layersFinal = (layers != null) ? layers : 0;
        world.getServer().execute(() -> {
            if ("stab".equals(type)) {
                double y = targetPos.y + 30;
                double minY = world.getMinBuildHeight();
                int count = 0;
                while (y >= minY && count < total) {
                    PrimedTntExtendedAPI tnt = new PrimedTntExtendedAPI(EntityType.TNT, world);
                    PrimedTnt tnt2 = new PrimedTnt(EntityType.TNT, world);
                    if (tnt != null) {
                        tnt.setPos(targetPos.x, y, targetPos.z);
                        tnt.setFuse(0);
                        tnt.setNoGravity(true);
                        tnt.setDeltaMovement(0.0, 0.0, 0.0);
                        tnt.setDamage(100000);
                        tnt.setExplosionRadius(16);
                        world.addFreshEntity(tnt);
                        nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                    }
                    y -= 1.0;
                    count++;
                }

            }
            else if ("chunkdel".equals(type)) {
                int chunkX = ((int) targetPos.x) >> 4;
                int chunkZ = ((int) targetPos.z) >> 4;
                int minY = world.getMinBuildHeight();
                int maxY = world.getMaxBuildHeight();

                int placed = 0;
                double spacing = Math.max(1.0, Math.cbrt((16 * 16 * (maxY - minY)) / (double) total));

                for (int y = minY; y < maxY; y = (int) (y + spacing)) {
                    for (int cx = (chunkX << 4); cx < (chunkX << 4) + 16; cx += spacing) {
                        for (int cz = (chunkZ << 4); cz < (chunkZ << 4) + 16; cz += spacing) {
                            BlockState state = world.getBlockState(new BlockPos(cx, y, cz));
                            if (!state.isAir()) {
                                PrimedTntExtendedAPI tnt = new PrimedTntExtendedAPI(EntityType.TNT, world);
                                if (tnt != null) {
                                    tnt.setPos(cx + 0.5, y + 0.5, cz + 0.5);
                                    tnt.setFuse(0);
                                    tnt.setNoGravity(true);
                                    tnt.setDeltaMovement(0.0, 0.0, 0.0);
                                    tnt.setExplosionRadius(1);
                                    tnt.setDamage(1000f);
                                    world.addFreshEntity(tnt);
                                    nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                                    placed++;
                                }
                            }
                        }
                    }
                }
            }
            else if ("chunklaser".equals(type)) {
                int chunkX = ((int) targetPos.x) >> 4;
                int chunkZ = ((int) targetPos.z) >> 4;
                int y0 = (int) targetPos.y;

                int placed = 0;
                int spacing = Math.max(1, (int) Math.sqrt(16 * 16 / (double) total));

                for (int cx = (chunkX << 4); cx < (chunkX << 4) + 16; cx += spacing) {
                    for (int cz = (chunkZ << 4); cz < (chunkZ << 4) + 16; cz += spacing) {
                        if (placed >= total) break;
                        PrimedTntExtendedAPI tnt = new PrimedTntExtendedAPI(EntityType.TNT, world);
                        if (tnt != null) {
                            tnt.setPos(cx + 0.5, y0 + 0.5, cz + 0.5);
                            tnt.setFuse(0);
                            tnt.setNoGravity(true);
                            tnt.setDeltaMovement(0.0, 0.0, 0.0);
                            tnt.setExplosionRadius(1);
                            world.addFreshEntity(tnt);
                            nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                            placed++;
                        }
                    }
                }
            }
            else if ("nuke".equals(type)) {

                int baseFuse = 80;
                double gravity = -0.03D;
                double velocityMultiplier = 1.4D;
                int[] baseRadii = new int[]{12, 22, 32, 42, 52, 62, 72, 82, 92, 102};

                double fallHeight = 0.5D * -gravity * baseFuse * baseFuse;
                double spawnY = targetPos.y + fallHeight;

                int spawned = 0;

                PrimedTntExtendedAPI center = new PrimedTntExtendedAPI(EntityType.TNT, world);
                center.setPos(targetPos.x + 0.5D, spawnY, targetPos.z + 0.5D);
                center.setFuse(baseFuse);
                center.setDeltaMovement(0.0D, 0.0D, 0.0D);
                world.addFreshEntity(center);
                nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(center);
                spawned++;

                while (spawned < total) {
                    for (int r : baseRadii) {
                        if (spawned >= total) break;
                        int tntsInRing = Math.min(100, total - spawned);

                        for (int i = 0; i < tntsInRing && spawned < total; i++) {
                            double angle = Math.random() * 2.0 * Math.PI;
                            double dx = Math.cos(angle);
                            double dz = Math.sin(angle);

                            double vx = dx * (r / (double) baseFuse) * velocityMultiplier;
                            double vz = dz * (r / (double) baseFuse) * velocityMultiplier;

                            PrimedTntExtendedAPI tnt = new PrimedTntExtendedAPI(EntityType.TNT, world);
                            tnt.setPos(targetPos.x + 0.5D, spawnY, targetPos.z + 0.5D);
                            tnt.setFuse(baseFuse);
                            tnt.setDeltaMovement(vx, 0.0D, vz);
                            world.addFreshEntity(tnt);
                            nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                            spawned++;
                        }
                    }
                }
            }

            player.sendSystemMessage(Component.literal("Orbital strike launched! Type: " + type + ", Total: " + total));
        });
    }
    public static void spawnArrowTNTNuke(ServerPlayer player, Integer tnts, String type) {
        ServerLevel world = (ServerLevel) player.level();

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 end = eyePos.add(lookVec.scale(100000.0));

        BlockHitResult hit = world.clip(new ClipContext(
                eyePos, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 targetPos = hit != null ? hit.getLocation() : end;
        int total = (tnts != null && tnts > 0) ? tnts : 100;

        world.getServer().execute(() -> {

            if ("ArrowStab".equals(type)) {

                double spawnY = targetPos.y + 20.0;

                Arrow arrow = new Arrow(world, targetPos.x, spawnY, targetPos.z);
                arrow.setNoGravity(true);
                arrow.setDeltaMovement(Vec3.ZERO);
                arrow.setPierceLevel((byte)127);
                arrow.setCritArrow(true);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                world.addFreshEntity(arrow);

                for (int i = 0; i < total; i++) {
                    PrimedTntExtendedAPI tnt = new PrimedTntExtendedAPI(EntityType.TNT, world);
                    tnt.setPos(targetPos.x, spawnY + 1.0, targetPos.z);
                    tnt.setFuse(0);
                    tnt.setNoGravity(true);
                    tnt.setDeltaMovement(0.0, 0.0, 0.0);
                    tnt.setDamage(-1000);
                    tnt.setExplosionRadius(16);
                    world.addFreshEntity(tnt);
                    nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                }

            } else if ("ArrowNuke".equals(type)) {

                int[] baseRadii = new int[]{12,22,32,42,52,62,72,82,92,102};
                int spawned = 0;

                PrimedTntExtendedAPI center = new PrimedTntExtendedAPI(EntityType.TNT, world);
                center.setPos(targetPos.x, targetPos.y + 20, targetPos.z);
                center.setFuse(0);
                world.addFreshEntity(center);
                nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(center);
                spawned++;

                for (int ringIndex = 0; spawned < total; ringIndex++) {
                    int r = ringIndex < baseRadii.length ? baseRadii[ringIndex] : 98 + (ringIndex - baseRadii.length + 1) * 10;
                    int tntsInRing = Math.min(100, total - spawned);

                    for (int i = 0; i < tntsInRing && spawned < total; i++, spawned++) {
                        double angle = Math.random() * 2.0 * Math.PI;
                        double dx = Math.cos(angle);
                        double dz = Math.sin(angle);

                        double vx = dx * (r / 80.0) * 1.4;
                        double vz = dz * (r / 80.0) * 1.4;

                        PrimedTntExtendedAPI tnt = new PrimedTntExtendedAPI(EntityType.TNT, world);
                        tnt.setPos(targetPos.x, targetPos.y + 20, targetPos.z);
                        tnt.setFuse(0);
                        tnt.setDeltaMovement(vx, 0.0, vz);
                        tnt.setDamage(100000);
                        tnt.setExplosionRadius(16);
                        world.addFreshEntity(tnt);
                        nukeSpawnedEntities.computeIfAbsent(world, k -> new HashSet<>()).add(tnt);
                    }
                }
            }

            player.sendSystemMessage(Component.literal(
                    "Arrow TNT Nuke launched! Type: " + type + ", Count: " + total
            ));
        });
    }
    public static void summonWolves(ServerPlayer player, int amount) {
        ServerLevel level = player.serverLevel();

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        for (int i = 0; i < amount; i++) {
            Wolf wolf = new Wolf(EntityType.WOLF, level);

            wolf.setPos(x, y, z);
            wolf.tame(player);
            wolf.setOwnerUUID(player.getUUID());

            wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1, false, true));
            wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 1, false, true));
            wolf.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 9600, 0, false, true));

            double angle = Math.random() * Math.PI * 2.0;
            double pitch = (Math.random() - 0.5) * Math.PI * 0.5;
            double horizontalSpeed = 0.8;

            double vx = Math.cos(angle) * Math.cos(pitch) * horizontalSpeed;
            double vy = Math.sin(pitch) * 0.6 + 0.3;
            double vz = Math.sin(angle) * Math.cos(pitch) * horizontalSpeed;

            wolf.setDeltaMovement(vx, vy, vz);
            level.addFreshEntity(wolf);
        }
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

        if (entities.isEmpty()) nukeSpawnedEntities.remove(world);
        if (frozen.isEmpty()) pausedChunks.remove(world);
    }
}
