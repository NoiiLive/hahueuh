package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModDataComponents;
import net.noiilive.hahueuh.ModItems;
import net.noiilive.hahueuh.ModMenus;
import net.noiilive.hahueuh.client.gui.EfficientEnchantTab;
import net.noiilive.hahueuh.client.gui.EfficientEnchantingScreen;
import net.noiilive.hahueuh.network.ClientGreedState;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.OpenEfficientEnchantingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class EfficientEnchantingClient {
    private EfficientEnchantingClient() {}

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.EFFICIENT_ENCHANTING.get(), EfficientEnchantingScreen::new);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof EnchantmentScreen screen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (ClientGreedState.greedVariant() != GreedVariant.ECHIDNA || !ClientGreedState.canGreed()) return;
        if (!hasSummonedBook(mc.player)) return;

        AbstractContainerScreen<?> container = screen;
        int x = container.getGuiLeft() - 27;
        int y = container.getGuiTop() + 8;
        event.addListener(new EfficientEnchantTab(x, y, 28, 28,
                b -> PacketDistributor.sendToServer(OpenEfficientEnchantingPayload.INSTANCE)));
    }

    private static boolean hasSummonedBook(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isOwnBook(stack, player)) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isOwnBook(stack, player)) return true;
        }
        return false;
    }

    private static boolean isOwnBook(ItemStack stack, Player player) {
        return stack.is(ModItems.MEMORIES_OF_THE_WORLD.get())
                && player.getUUID().equals(stack.get(ModDataComponents.BOOK_OWNER.get()));
    }
}
