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

    override fun explode() {
        val world = this.level()
        if (!world.isClientSide) {
            world.explode(this, this.x, this.y, this.z, 4.0f, Level.ExplosionInteraction.TNT)

            val explosionType: Holder<DamageType> = world.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.EXPLOSION)

            val radius = 10.0
            world.getEntities(
                this,
                this.boundingBox.inflate(radius)
            ).forEach(Consumer { entity: Entity ->
                if (entity !== this) {
                    entity.hurt(DamageSource(explosionType, this), damage)
                }
            })
        }
        this.discard()
    }
}