package com.palorder.smp.java

import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class PrimedTntExtendedAPI(type: EntityType<out PrimedTnt>, level: Level) : PrimedTnt(type, level) {

    private var damage: Float = 4f
    private var explosionRadius: Double = 10.0
    private var downForce: Float = 0.04f
    private val entitySpecificDamage: MutableMap<EntityType<*>, Float> = mutableMapOf()

    fun setDamage(damage: Float) {
        this.damage = damage
    }

    fun setExplosionRadius(radius: Double) {
        this.explosionRadius = radius
    }

    fun setDamageForEntityType(entityType: EntityType<*>, damage: Float) {
        entitySpecificDamage[entityType] = damage
    }

    fun setDownForce(force: Float) {
        this.downForce = force
    }

    fun getDownForce(): Float = downForce

    override fun tick() {
        val motion = deltaMovement
        deltaMovement = Vec3(motion.x, motion.y - downForce, motion.z)
        super.tick()
    }

    override fun explode() {
        val world = level
        if (!world.isClientSide) {
            world.explode(this, x, y, z, 4f, Level.ExplosionInteraction.TNT)

            val explosionType: Holder<DamageType> = world.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.EXPLOSION)

            world.getEntities(this, boundingBox.inflate(explosionRadius)).forEach { entity ->
                if (entity !== this) {
                    val appliedDamage = entitySpecificDamage.getOrDefault(entity.type, damage)
                    entity.hurt(DamageSource(explosionType, this), appliedDamage)
                }
            }
        }
        discard()
    }
}
