package net.noiilive.hahueuh;

import net.noiilive.hahueuh.client.AbilitySlots;
import net.noiilive.hahueuh.network.DeathFadeState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = HahUeuh.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public class HahUeuhClient {
    public HahUeuhClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        HahUeuh.LOGGER.info("HELLO FROM CLIENT SETUP");
        HahUeuh.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "death_fade"),
                HahUeuhClient::renderDeathFade);

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ability_hud"),
                net.noiilive.hahueuh.client.AbilityHud::render);
    }

    @SubscribeEvent
    static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        DeathFadeState.reset();
        net.noiilive.hahueuh.network.DomainRenderState.clear();
        net.noiilive.hahueuh.network.RemoteUnseenHands.clear();
        net.noiilive.hahueuh.network.ClientLionsHeartState.clear();
        net.noiilive.hahueuh.network.ClientLittleKingState.clear();
        net.noiilive.hahueuh.api.AbilityCooldowns.reset();
        AbilitySlots.reset();
    }

    private static void renderDeathFade(GuiGraphics graphics, DeltaTracker deltaTracker) {
        float alpha = DeathFadeState.advanceAndGetAlpha();
        if (alpha <= 0.001f) return;
        int a = (int) (alpha * 255f) & 0xFF;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), a << 24);
    }
}
