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

class PrimedTntExtendedAPI(type: EntityType<out PrimedTnt?>, level: Level) :
    PrimedTnt(type, level) {
    var damage: Float = 4.0f
    var explosionRadius: Double = 10.0
    private val entitySpecificDamage: MutableMap<EntityType<*>, Float> = HashMap()

    fun setDamageForEntityType(entityType: EntityType<*>, damage: Float) {
        entitySpecificDamage[entityType] = damage
    }

    override fun explode() {
        val world = this.level()
        if (!world.isClientSide) {
            world.explode(this, this.x, this.y, this.z, 4.0f, Level.ExplosionInteraction.TNT)

            val explosionType: Holder<DamageType> = world.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.EXPLOSION)

            world.getEntities(this, this.boundingBox.inflate(explosionRadius)).forEach(
                Consumer { entity: Entity ->
                    if (entity !== this) {
                        var appliedDamage = damage
                        if (entitySpecificDamage.containsKey(entity.type)) {
                            appliedDamage = entitySpecificDamage[entity.type]!!
                        }
                        entity.hurt(DamageSource(explosionType, this), appliedDamage)
                    }
                })
        }
        this.discard()
    }
}