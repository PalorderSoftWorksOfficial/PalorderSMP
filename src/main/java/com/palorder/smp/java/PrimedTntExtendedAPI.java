package com.palorder.smp.java;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class PrimedTntExtendedAPI extends PrimedTnt {

    private float damage = 4.0f;
    private double explosionRadius = 10.0;
    private final Map<EntityType<?>, Float> entitySpecificDamage = new HashMap<>();

    public PrimedTntExtendedAPI(EntityType<? extends PrimedTnt> type, Level level) {
        super(type, level);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setExplosionRadius(double radius) {
        this.explosionRadius = radius;
    }

    public void setDamageForEntityType(EntityType<?> entityType, float damage) {
        entitySpecificDamage.put(entityType, damage);
    }

    public float getDamage() {
        return this.damage;
    }

    public double getExplosionRadius() {
        return this.explosionRadius;
    }

    @Override
    protected void explode() {
        Level world = this.level();
        if (!world.isClientSide) {
            world.explode(this, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.TNT);

            Holder<DamageType> explosionType = world.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.EXPLOSION);

            world.getEntities(this, this.getBoundingBox().inflate(explosionRadius)).forEach(entity -> {
                if (entity != this) {
                    float appliedDamage = damage;
                    if (entitySpecificDamage.containsKey(entity.getType())) {
                        appliedDamage = entitySpecificDamage.get(entity.getType());
                    }
                    entity.hurt(new DamageSource(explosionType, this), appliedDamage);
                }
            });
        }
        this.discard();
    }
}
