package com.example.hahueuh;

import com.example.hahueuh.client.AbilitySlots;
import com.example.hahueuh.network.DeathFadeState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
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
                com.example.hahueuh.client.AbilityHud::render);
    }

    @SubscribeEvent
    static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        DeathFadeState.reset();
        com.example.hahueuh.network.DomainRenderState.clear();
        com.example.hahueuh.network.RemoteUnseenHands.clear();
        com.example.hahueuh.network.ClientLionsHeartState.clear();
        com.example.hahueuh.api.AbilityCooldowns.reset();
        AbilitySlots.reload(currentWorldKey());
    }

    private static String currentWorldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isLocalServer() && mc.getSingleplayerServer() != null) {
            return "sp-" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        ServerData serverData = mc.getCurrentServer();
        return serverData != null ? "mp-" + serverData.ip : "unknown";
    }

    private static void renderDeathFade(GuiGraphics graphics, DeltaTracker deltaTracker) {
        float alpha = DeathFadeState.advanceAndGetAlpha();
        if (alpha <= 0.001f) return;
        int a = (int) (alpha * 255f) & 0xFF;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), a << 24);
    }
}
