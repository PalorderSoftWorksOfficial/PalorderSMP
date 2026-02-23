package com.palorder.smp.java.Implementations;

import com.palorder.smp.java.PrimedTntExtendedAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class spawnTNTNuke {
    /*
    Notice:
    Please use PrimedTntExtendedAPI tnt = new PrimedTntExtendedAPI(EntityType.TNT, world);
    Instead of PrimedTnt tnt = EntityType.TNT.create(world); when adding a new type as It has ExtendedAPI like setDamage, setDamageForEntityType

     */
    // ---------------- Nuke tracking ----------------
    public static final Map<UUID, Vec3> nukePlayerTeleportBack = new HashMap<>();
    public static final Map<ServerLevel, Set<ChunkPos>> pausedChunks = new HashMap<>();
    public static final Map<ServerLevel, Set<Entity>> nukeSpawnedEntities = new HashMap<>();
    public static Random rand = new Random();
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
                int step = (int) Math.floor(spacing);
                if (step < 1) {
                    step = 1;
                }

                for (int y = minY; y < maxY; y += spacing) {
                    for (int cx = (chunkX << 4); cx < (chunkX << 4) + 16; cx = (int) (cx + spacing)) {
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
}
