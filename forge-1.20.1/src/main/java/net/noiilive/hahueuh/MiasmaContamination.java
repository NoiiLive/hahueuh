package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public final class MiasmaContamination {
    private static final int EATEN_SEVERITY = 20;
    private static final String TAG = "HahueuhMiasmaContaminated";

    public static boolean isContaminated(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG);
    }

    public static void setContaminated(ItemStack stack) {
        if (stack.isEmpty()) return;
        stack.getOrCreateTag().putBoolean(TAG, true);
    }

    public static void carryContamination(Container input, ItemStack result) {
        if (result.isEmpty()) return;
        for (int i = 0; i < input.getContainerSize(); i++) {
            if (isContaminated(input.getItem(i))) {
                setContaminated(result);
                return;
            }
        }
    }

    public static void contaminateBlockDrops(ServerLevel level, BlockPos pos, List<ItemStack> drops) {
        if (!isHighMiasma(level, pos)) return;
        for (ItemStack drop : drops) {
            setContaminated(drop);
        }
    }

    private static boolean isHighMiasma(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return ChunkMiasmaData.get(chunk) >= Miasma.effectThreshold();
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!isHighMiasma(level, event.getEntity().blockPosition())) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEdible()) {
                setContaminated(stack);
            }
        }
    }

    @SubscribeEvent
    public void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide) return;
        if (isContaminated(event.getItem())) {
            Miasma.applySickness(event.getEntity(), EATEN_SEVERITY);
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
