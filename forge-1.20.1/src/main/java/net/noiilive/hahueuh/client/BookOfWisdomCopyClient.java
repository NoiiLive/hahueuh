package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.BookOfWisdomCopyItem;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModItems;
import net.noiilive.hahueuh.client.gui.VisionOfInformationScreen;
import net.noiilive.hahueuh.network.ActivateBookOfWisdomVisionPacket;
import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.network.OpenBookOfWisdomBindPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class BookOfWisdomCopyClient {
    private BookOfWisdomCopyClient() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.BOOK_OF_WISDOM_COPY.get())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        event.setCanceled(true);

        Integer bound = BookOfWisdomCopyItem.boundAbility(stack);
        if (bound == null) {
            ModNetworking.CHANNEL.sendToServer(OpenBookOfWisdomBindPacket.INSTANCE);
            return;
        }

        BoundVisionAbility ability = BoundVisionAbility.byOrdinal(bound);
        if (ability == BoundVisionAbility.VISION_OF_INFORMATION) {
            mc.setScreen(new VisionOfInformationScreen());
        } else {
            ModNetworking.CHANNEL.sendToServer(ActivateBookOfWisdomVisionPacket.INSTANCE);
        }
    }
}
