package com.palorder.smp.java.Implementations;

import com.palorder.smp.java.PrimedTntExtendedAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

import static com.palorder.smp.java.Implementations.spawnTNTNuke.nukeSpawnedEntities;

public class spawnArrowNuke {
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
}
