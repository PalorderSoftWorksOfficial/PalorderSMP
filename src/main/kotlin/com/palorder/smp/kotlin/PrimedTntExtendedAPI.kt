package com.palorder.smp.kotlin

import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.level.Level
import java.util.function.Consumer

class PrimedTntExtendedAPI(type: EntityType<out PrimedTnt?>, level: Level) : PrimedTnt(type, level) {
    private var damage = 4.0f
    private var explosionRadius = 10.0
    var downForce: Float = 0.04f
    private val entitySpecificDamage: MutableMap<EntityType<*>?, Float?> = HashMap<EntityType<*>?, Float?>()

    fun setDamage(damage: Float) {
        this.damage = damage
    }

    fun setExplosionRadius(radius: Double) {
        this.explosionRadius = radius
    }

    fun setDamageForEntityType(entityType: EntityType<*>?, damage: Float) {
        entitySpecificDamage.put(entityType, damage)
    }

    override fun tick() {
        val motion = this.getDeltaMovement()
        this.setDeltaMovement(motion.x, motion.y - downForce, motion.z)
        super.tick()
    }

    override fun explode() {
        val world = this.level()
        if (!world.isClientSide) {
            world.explode(this, this.getX(), this.getY(), this.getZ(), 4.0f, Level.ExplosionInteraction.TNT)

            val explosionType: Holder<DamageType?> = world.registryAccess()
                .registryOrThrow<DamageType?>(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.EXPLOSION)

            world.getEntities(this, this.getBoundingBox().inflate(explosionRadius))
                .forEach(Consumer { entity: Entity? ->
                    if (entity !== this) {
                        val appliedDamage = entitySpecificDamage.getOrDefault(entity!!.getType(), damage)!!
                        entity.hurt(DamageSource(explosionType, this), appliedDamage)
                    }
                })
        }
        this.discard()
    }
}