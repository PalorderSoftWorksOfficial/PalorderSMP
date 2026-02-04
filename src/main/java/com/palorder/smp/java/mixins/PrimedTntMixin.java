package com.palorder.smp.java.mixins;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {

    @Unique private float palorder$damage = 4.0f;
    @Unique private double palorder$explosionRadius = 10.0;
    @Unique private float palorder$downForce = 0.04f;
    @Unique private final Map<EntityType<?>, Float> palorder$entitySpecificDamage = new HashMap<>();

    @Unique private boolean palorder$mergeBool = false;
    @Unique private int palorder$mergedTNT = 1;

    @Shadow public abstract int getFuse();

    @Inject(method = "tick", at = @At("HEAD"))
    private void palorder$applyDownForce(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt)(Object)this;

        if (!self.level().isClientSide) {
            Vec3 motion = self.getDeltaMovement();
            self.setDeltaMovement(motion.x, motion.y - palorder$downForce, motion.z);
        }

        Vec3 velocity = self.getDeltaMovement();
        if (!self.level().isClientSide && (velocity.x != 0.0 || velocity.y != 0.0 || velocity.z != 0.0)) {
            palorder$mergeBool = true;
        }

        if (!self.level().isClientSide && palorder$mergeBool && velocity.x == 0.0 && velocity.y == 0.0 && velocity.z == 0.0) {
            palorder$mergeBool = false;
            AABB bounds = self.getBoundingBox().inflate(0.5);
            for (Entity entity : self.level().getEntities(self, bounds)) {
                if (entity instanceof PrimedTnt && entity != self) {
                    PrimedTnt otherTNT = (PrimedTnt) entity;
                    Vec3 otherVelocity = otherTNT.getDeltaMovement();
                    if (otherVelocity.x == 0.0 && otherVelocity.y == 0.0 && otherVelocity.z == 0.0
                            && self.blockPosition().equals(otherTNT.blockPosition())
                            && self.getFuse() == otherTNT.getFuse()) {
                        palorder$mergedTNT += 1;
                        otherTNT.discard();
                    }
                }
            }
        }
    }

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void palorder$customExplosion(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        Level world = self.level();

        if (!world.isClientSide) {
            world.explode(self, self.getX(), self.getY(), self.getZ(), 4.0F, Level.ExplosionInteraction.TNT);

            Holder<DamageType> explosionType = world.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.EXPLOSION);

            for (Entity entity : world.getEntities(self, self.getBoundingBox().inflate(palorder$explosionRadius))) {
                if (entity != self) {
                    float appliedDamage = palorder$entitySpecificDamage.getOrDefault(entity.getType(), palorder$damage);
                    entity.hurt(new DamageSource(explosionType, self), appliedDamage);
                }
            }

            for (int i = 0; i < palorder$mergedTNT - 1; i++) {
                world.addFreshEntity(new PrimedTnt(world, self.getX(), self.getY(), self.getZ(), null));
            }
        }

        self.discard();
        ci.cancel();
    }

    @Unique
    public void palorder$setDamage(float damage) {
        this.palorder$damage = damage;
    }

    @Unique
    public void palorder$setExplosionRadius(double radius) {
        this.palorder$explosionRadius = radius;
    }

    @Unique
    public void palorder$setDownForce(float force) {
        this.palorder$downForce = force;
    }

    @Unique
    public void palorder$setDamageForEntityType(EntityType<?> type, float damage) {
        palorder$entitySpecificDamage.put(type, damage);
    }

    @Unique
    public int palorder$getMergedTNT() {
        return palorder$mergedTNT;
    }
}
