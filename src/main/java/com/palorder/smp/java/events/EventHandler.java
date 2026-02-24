package com.palorder.smp.java.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.palorder.smp.java.Implementations.wolfArmy.summonWolves;
import static com.palorder.smp.java.tick.runLater.runLater;
import static com.palorder.smp.java.Implementations.spawnTNTNuke.spawnTNTNuke;
import static com.palorder.smp.java.Implementations.spawnArrowNuke.spawnArrowTNTNuke;

public class EventHandler {
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
            switch (type) {
                case "ArrowNuke", "ArrowStab" -> spawnArrowTNTNuke(p, amount, type);
                case "Wolf" -> summonWolves(p, amount);
                case "nuke_2" -> spawnTNTNuke(p, amount, "nuke", layers);
                default -> spawnTNTNuke(p, amount, type, layers);
            }
            t.putInt("RodUse", 0);
        });
    }
}
