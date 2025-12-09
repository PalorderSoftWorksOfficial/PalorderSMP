package com.palorder.smp.java;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;

public class PrimedTntExtendedAPI extends PrimedTnt {

    private float damage = 4.0f;

    public PrimedTntExtendedAPI(EntityType<? extends PrimedTnt> type, Level level) {
        super(type, level);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    @Override
    protected void explode() {
        Level world = this.level();
        if (!world.isClientSide) {
            world.explode(this, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.TNT);

            Holder<DamageType> explosionType = world.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.EXPLOSION);

            double radius = 10;
            world.getEntities(this, this.getBoundingBox().inflate(radius)).forEach(entity -> {
                if (entity != this) {
                    entity.hurt(new DamageSource(explosionType, this), damage);
                }
            });
        }
        this.discard();
    }
}
