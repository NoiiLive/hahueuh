package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.BookOfWisdom;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModItems;
import net.noiilive.hahueuh.client.gui.EfficientEnchantTab;
import net.noiilive.hahueuh.network.ClientGreedState;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.network.OpenEfficientEnchantingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class EfficientEnchantingClient {
    private EfficientEnchantingClient() {}

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
                b -> ModNetworking.CHANNEL.sendToServer(OpenEfficientEnchantingPacket.INSTANCE)));
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
                && player.getUUID().equals(BookOfWisdom.bookOwner(stack));
    }
}
