package com.palorder.smp.java.mixins;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
        return downForce;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickInject(CallbackInfo ci) {
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(motion.x, motion.y - downForce, motion.z));
    }

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void explodeInject(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        Level level = self.level();

        if (!level.isClientSide) {
            level.explode(self, self.getX(), self.getY(), self.getZ(), 4.0F, Level.ExplosionInteraction.TNT);
            Holder<DamageType> explosionType = level.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.EXPLOSION);

            AABB box = self.getBoundingBox().inflate(explosionRadius);
            level.getEntities(self, box).forEach(entity -> {
                if (entity != self) {
                    float appliedDamage = entitySpecificDamage.getOrDefault(entity.getType(), damage);
                    entity.hurt(new DamageSource(explosionType, self), appliedDamage);
                }
            });
        }

        self.discard();
        ci.cancel();
    }
}

