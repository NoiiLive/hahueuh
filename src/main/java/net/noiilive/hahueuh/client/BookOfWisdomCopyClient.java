package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModDataComponents;
import net.noiilive.hahueuh.ModItems;
import net.noiilive.hahueuh.ModMenus;
import net.noiilive.hahueuh.client.gui.BookOfWisdomBindScreen;
import net.noiilive.hahueuh.client.gui.VisionOfInformationScreen;
import net.noiilive.hahueuh.network.ActivateBookOfWisdomVisionPayload;
import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.noiilive.hahueuh.network.OpenBookOfWisdomBindPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class BookOfWisdomCopyClient {
    private BookOfWisdomCopyClient() {}

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BOOK_OF_WISDOM_BIND.get(), BookOfWisdomBindScreen::new);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.BOOK_OF_WISDOM_COPY.get())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        event.setCanceled(true);

        Integer bound = stack.get(ModDataComponents.BOUND_VISION_ABILITY.get());
        if (bound == null) {
            PacketDistributor.sendToServer(OpenBookOfWisdomBindPayload.INSTANCE);
            return;
        }

        BoundVisionAbility ability = BoundVisionAbility.byOrdinal(bound);
        if (ability == BoundVisionAbility.VISION_OF_INFORMATION) {
            mc.setScreen(new VisionOfInformationScreen());
        } else {
            PacketDistributor.sendToServer(ActivateBookOfWisdomVisionPayload.INSTANCE);
        }
    }
}
