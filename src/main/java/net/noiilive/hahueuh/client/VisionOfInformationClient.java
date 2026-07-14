package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModDataComponents;
import net.noiilive.hahueuh.ModItems;
import net.noiilive.hahueuh.client.gui.VisionOfInformationScreen;
import net.noiilive.hahueuh.network.ClientGreedState;
import net.noiilive.hahueuh.network.GreedVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class VisionOfInformationClient {
    private VisionOfInformationClient() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.MEMORIES_OF_THE_WORLD.get())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (ClientGreedState.greedVariant() != GreedVariant.ECHIDNA || !ClientGreedState.canGreed()) return;
        if (!mc.player.getUUID().equals(stack.get(ModDataComponents.BOOK_OWNER.get()))) return;

        event.setCanceled(true);
        mc.setScreen(new VisionOfInformationScreen());
    }
}
