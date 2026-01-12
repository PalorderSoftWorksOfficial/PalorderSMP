package com.palorder.smp.java.mixins;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;
import java.util.Map;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract void setDeltaMovement(Vec3 motion);

    @Shadow
    protected abstract Level level();

    @Shadow
    protected abstract void discard();

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract net.minecraft.world.phys.AABB getBoundingBox();

    private float damage = 4.0f;
    private double explosionRadius = 10.0;
    private float downForce = 0.04f;
    private final Map<EntityType<?>, Float> entitySpecificDamage = new HashMap<>();

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setExplosionRadius(double radius) {
        this.explosionRadius = radius;
    }

    public void setDamageForEntityType(EntityType<?> entityType, float damage) {
        entitySpecificDamage.put(entityType, damage);
    }

    public void setDownForce(float force) {
        this.downForce = force;
    }

    public float getDownForce() {
        return this.downForce;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickInject(CallbackInfo ci) {
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(motion.x, motion.y - downForce, motion.z));
    }

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void explodeInject(CallbackInfo ci) {
        Level world = this.level();
        if (!world.isClientSide) {
            world.explode((PrimedTnt) (Object) this, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.TNT);

            Holder<DamageType> explosionType = world.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.EXPLOSION);

            world.getEntities((PrimedTnt) (Object) this, this.getBoundingBox().inflate(explosionRadius)).forEach(entity -> {
                if (entity != (PrimedTnt) (Object) this) {
                    float appliedDamage = entitySpecificDamage.getOrDefault(entity.getType(), damage);
                    entity.hurt(new DamageSource(explosionType, (PrimedTnt) (Object) this), appliedDamage);
                }
            });
        }
        this.discard();
        ci.cancel();
    }
}
