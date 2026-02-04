package com.palorder.smp.java.mixins.optimizedFastEntityMovement;

import com.palorder.smp.java.helpers.OFEMContext;
import com.palorder.smp.java.helpers.OFEMUtil;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Unique
    private static final ThreadLocal<OFEMContext> ofemContext = ThreadLocal.withInitial(() -> null);

    @WrapOperation(
            method = "collideBoundingBox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/lang/Iterable;"
            )
    )
    private static Iterable<VoxelShape> dontUseThatLargeBlockCollisions(Level world, Entity entity, AABB box,
                                                                        Operation<Iterable<VoxelShape>> original,
                                                                        @Local(argsOnly = true) Vec3 movement) {
        OFEMContext ctx = OFEMUtil.createContext(world, entity, movement);
        ofemContext.set(ctx);
        if (ctx != null) return Collections.emptyList();
        return original.call(world, entity, box);
    }

    @ModifyExpressionValue(
            method = "collideWithShapes",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;isEmpty()Z",
                    ordinal = 0
            )
    )
    private static boolean theCollisionsListParameterIsIncomplete(boolean isEmpty) {
        if (ofemContext.get() != null) isEmpty = false;
        return isEmpty;
    }

    @ModifyArgs(
            method = "collideWithShapes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/shapes/Shapes;collide(Lnet/minecraft/core/Direction$Axis;Lnet/minecraft/world/phys/AABB;Ljava/lang/Iterable;D)D"
            ),
            require = 4
    )
    private static void useTheAxisOnlyBlockCollisions(Args args) {
        OFEMContext ctx = ofemContext.get();
        if (ctx != null) {
            Direction.Axis axis = (Direction.Axis) args.get(0);
            AABB entityBoundingBox = (AABB) args.get(1);
            List<VoxelShape> entityAndBorderCollisions = (List<VoxelShape>) args.get(2);
            double maxDist = (Double) args.get(3);

            ctx.axis = axis;
            ctx.movementOnAxis = maxDist;
            ctx.entityBoundingBox = entityBoundingBox;

            Iterable<VoxelShape> blockCollisions = OFEMUtil.getAxisOnlyBlockCollision(ctx);
            List<VoxelShape> voxelShapeList = Lists.newArrayList(entityAndBorderCollisions);
            Objects.requireNonNull(voxelShapeList);
            blockCollisions.forEach(voxelShapeList::add);

            args.set(2, voxelShapeList);
        }
    }
}
