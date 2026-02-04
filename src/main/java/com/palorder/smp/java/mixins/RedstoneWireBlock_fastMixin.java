package com.palorder.smp.java.mixins;

import com.palorder.smp.java.fakes.RedstoneWireBlockInterface;
import com.palorder.smp.java.helpers.RedstoneWireTurbo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlock_fastMixin implements RedstoneWireBlockInterface {

    private RedstoneWireTurbo wireTurbo = null;

    @Shadow
    private void updatePowerStrength(Level world, BlockPos pos, BlockState state) {
    }

    @Unique
    private int palorderSMP$getSignal(Level world, BlockPos pos) {
        return 0;
    }

    @Accessor("shouldSignal")
    public abstract void setWiresGivePower(boolean var1);

    @Accessor("shouldSignal")
    public abstract boolean getWiresGivePower();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRedstoneWireBlockCTOR(BlockBehaviour.Properties p_55511_, CallbackInfo ci) {
        this.wireTurbo = new RedstoneWireTurbo((RedStoneWireBlock) (Object) this);
    }

    // Always use fast redstone updates
    public void fastUpdate(Level world, BlockPos pos, BlockState state, BlockPos source) {
        this.wireTurbo.updateSurroundingRedstone(world, pos, state, source);
    }

    @Inject(method = "updatePowerStrength", at = @At("HEAD"), cancellable = true)
    private void updateLogicAlternative(Level world, BlockPos pos, BlockState state, CallbackInfo cir) {
        this.updateLogicPublic(world, pos, state);
        cir.cancel();
    }

    public BlockState updateLogicPublic(Level world, BlockPos pos, BlockState state) {
        int power = palorderSMP$getSignal(world, pos);
        BlockState oldState = state;
        if (state.getValue(RedStoneWireBlock.POWER) != power) {
            state = state.setValue(RedStoneWireBlock.POWER, power);
            if (world.getBlockState(pos) == oldState && world.setBlock(pos, state, 18)) {
                wireTurbo.updateNeighborShapes(world, pos, state);
            }
        }
        return state;
    }

    @Redirect(
            method = "onPlace",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
            )
    )
    private void redirectOnBlockAddedUpdate(RedStoneWireBlock self, Level world, BlockPos pos, BlockState state) {
        fastUpdate(world, pos, state, null);
    }

    @Redirect(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
            )
    )
    private void redirectOnStateReplacedUpdate(RedStoneWireBlock self, Level world, BlockPos pos, BlockState state) {
        fastUpdate(world, pos, state, null);
    }

    @Redirect(
            method = "neighborChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
            )
    )
    private void redirectNeighborUpdateUpdate(RedStoneWireBlock self, Level world, BlockPos pos, BlockState state,
                                              BlockState state2, Level world2, BlockPos pos2,
                                              net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos,
                                              boolean flag) {
        fastUpdate(world, pos, state, neighborPos);
    }
}
