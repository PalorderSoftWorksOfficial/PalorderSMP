package com.palorder.smp.java.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Context object for Optimized Fast Entity Movement.
 * Stores the axis, movement delta, bounding box, entity, and world.
 */
public class OFEMContext {

    public Direction.Axis axis;
    public double movementOnAxis;
    public AABB entityBoundingBox;
    public Entity entity;
    public Level world;

    public OFEMContext(Level world, Entity entity) {
        this.world = world;
        this.entity = entity;
        this.entityBoundingBox = entity.getBoundingBox();
    }

    public OFEMContext(Level world, Entity entity, AABB boundingBox) {
        this.world = world;
        this.entity = entity;
        this.entityBoundingBox = boundingBox;
    }
}