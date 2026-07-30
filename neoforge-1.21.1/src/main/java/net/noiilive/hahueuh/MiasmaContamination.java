package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class MiasmaContamination {

    public static boolean isContaminated(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.MIASMA_CONTAMINATED.get()));
    }

    public static void carryContamination(RecipeInput input, ItemStack result) {
        if (result.isEmpty()) return;
        for (int i = 0; i < input.size(); i++) {
            if (isContaminated(input.getItem(i))) {
                result.set(ModDataComponents.MIASMA_CONTAMINATED.get(), true);
                return;
            }
        }
    }

    private static boolean isHighMiasma(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return ChunkMiasmaData.get(chunk) >= Miasma.effectThreshold();
    }

    @SubscribeEvent
    public void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return;
        if (!isHighMiasma(event.getLevel(), event.getPos())) return;
        for (ItemEntity drop : event.getDrops()) {
            drop.getItem().set(ModDataComponents.MIASMA_CONTAMINATED.get(), true);
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!isHighMiasma(level, event.getEntity().blockPosition())) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.has(DataComponents.FOOD)) {
                stack.set(ModDataComponents.MIASMA_CONTAMINATED.get(), true);
            }
        }
    }

    @SubscribeEvent
    public void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide) return;
        if (isContaminated(event.getItem())) {
            HahUeuh.INSANITY.addContaminatedMeal(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (isContaminated(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("hahueuh.tooltip.miasma_contaminated")
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
