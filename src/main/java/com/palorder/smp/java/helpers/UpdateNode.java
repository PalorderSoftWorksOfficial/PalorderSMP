package com.palorder.smp.java.helpers;

import com.palorder.smp.java.helpers.UpdateNodeType.UpdateNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class UpdateNode {
    public BlockState currentState;
    public UpdateNode[] neighborNodes;
    public BlockPos self;
    public BlockPos parent;
    public UpdateNodeType.UpdateNodeTypes type = UpdateNodeType.UpdateNodeTypes.UNKNOWN;
    public int layer;
    public boolean visited;
    public int xbias;
    public int zbias;
}