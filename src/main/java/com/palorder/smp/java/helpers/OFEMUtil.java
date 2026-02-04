package com.palorder.smp.java.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Utility class for OFEM
 */
public class OFEMUtil {

    /**
     * Creates a new OFEMContext for the given entity.
     */
    public static OFEMContext createContext(Level world, Entity entity, Vec3 movement) {
        return new OFEMContext(world, entity);
    }

    /**
     * Creates a new OFEMContext. Always returns context for "always-on" behavior.
     */
    public static OFEMContext checkAndCreateContext(Level world, Entity entity, AABB movement) {
        return new OFEMContext(world, entity);
    }

    /**
     * Returns only the block collisions along the movement axis.
     * Vanilla block collision shapes are filtered to the axis the entity is moving on.
     */
    public static List<VoxelShape> getAxisOnlyBlockCollision(OFEMContext ctx) {
        if (ctx == null || ctx.axis == null) return Collections.emptyList();

        List<VoxelShape> collisions = new ArrayList<>();
        AABB box = ctx.entityBoundingBox;

        // expand slightly in case entity is touching blocks
        AABB searchBox = box.inflate(0.001);

        BlockPos min = new BlockPos((int) searchBox.minX, (int) searchBox.minY, (int) searchBox.minZ);
        BlockPos max = new BlockPos((int) searchBox.maxX, (int) searchBox.maxY, (int) searchBox.maxZ);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = ctx.world.getBlockState(pos);
                    if (!state.isAir()) {
                        VoxelShape shape = state.getCollisionShape(ctx.world, pos);
                        if (!shape.isEmpty()) {
                            // Filter shape along axis
                            if (ctx.axis == Direction.Axis.X && shape.max(Direction.Axis.X) <= box.minX) continue;
                            if (ctx.axis == Direction.Axis.Y && shape.max(Direction.Axis.Y) <= box.minY) continue;
                            if (ctx.axis == Direction.Axis.Z && shape.max(Direction.Axis.Z) <= box.minZ) continue;
                            collisions.add(shape);
                        }
                    }
                }
            }
        }

        return collisions;
    }

    /**
     * For Lithium compat: fetch block collisions via Lithium API but filter for axis.
     */
    public static Iterable<VoxelShape> getAxisOnlyBlockCollision(OFEMContext ctx,
                                                                 BiFunction<Level, Entity, Iterable<VoxelShape>> vanillaGetter) {
        Iterable<VoxelShape> shapes = vanillaGetter.apply(ctx.world, ctx.entity);
        List<VoxelShape> filtered = new ArrayList<>();

        for (VoxelShape shape : shapes) {
            if (ctx.axis == Direction.Axis.X && shape.max(Direction.Axis.X) <= ctx.entityBoundingBox.minX) continue;
            if (ctx.axis == Direction.Axis.Y && shape.max(Direction.Axis.Y) <= ctx.entityBoundingBox.minY) continue;
            if (ctx.axis == Direction.Axis.Z && shape.max(Direction.Axis.Z) <= ctx.entityBoundingBox.minZ) continue;
            filtered.add(shape);
        }

        return filtered;
    }
}