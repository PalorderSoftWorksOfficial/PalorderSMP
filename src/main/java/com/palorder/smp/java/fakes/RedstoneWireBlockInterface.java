package com.palorder.smp.java.fakes;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface RedstoneWireBlockInterface {
    BlockState updateLogicPublic(Level world, BlockPos pos, BlockState state);

    void setWiresGivePower(boolean givePower);

    boolean getWiresGivePower();
}
