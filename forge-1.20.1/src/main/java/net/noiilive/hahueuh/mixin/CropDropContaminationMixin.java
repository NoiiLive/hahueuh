package net.noiilive.hahueuh.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.noiilive.hahueuh.MiasmaContamination;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public abstract class CropDropContaminationMixin {
    @Inject(
            method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void hahueuh$contaminateCropDrops(BlockState state, ServerLevel level, BlockPos pos,
                                                     BlockEntity blockEntity,
                                                     CallbackInfoReturnable<List<ItemStack>> cir) {
        hahueuh$apply(state, level, pos, cir.getReturnValue());
    }

    @Inject(
            method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void hahueuh$contaminateCropDropsWithBreaker(BlockState state, ServerLevel level, BlockPos pos,
                                                                BlockEntity blockEntity, Entity breaker, ItemStack tool,
                                                                CallbackInfoReturnable<List<ItemStack>> cir) {
        hahueuh$apply(state, level, pos, cir.getReturnValue());
    }

    private static void hahueuh$apply(BlockState state, ServerLevel level, BlockPos pos, List<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) return;
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return;
        MiasmaContamination.contaminateBlockDrops(level, pos, drops);
    }
}
