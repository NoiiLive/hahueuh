package net.noiilive.hahueuh;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PocketVoidBlockEntity extends BlockEntity {
    public PocketVoidBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.POCKET_VOID_BE.get(), pos, state);
    }
}
