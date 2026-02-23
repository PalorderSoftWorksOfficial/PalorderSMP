package com.palorder.smp.java.Implementations;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class wolfArmy {
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
            ItemStack wolfArmor = new ItemStack(Items.NETHERITE_CHESTPLATE);
            wolf.canHoldItem(wolfArmor);
            wolf.equipItemIfPossible(wolfArmor);
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
}
