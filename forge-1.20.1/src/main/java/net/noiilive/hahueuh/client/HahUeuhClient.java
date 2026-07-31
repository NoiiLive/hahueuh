package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.api.AbilityCooldowns;
import net.noiilive.hahueuh.network.ClientPlayerData;
import net.noiilive.hahueuh.network.DeathFadeState;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class HahUeuhClient {
    private HahUeuhClient() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        AbilityHud.render(event.getGuiGraphics());
        ManaOdBarHud.render(event.getGuiGraphics());
        SensoryDeprivationClient.renderOverlay(event.getGuiGraphics());
        renderFade(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        renderFade(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (!ClientPlayerData.of(player).isOdDepleted()) return;
        event.getInput().jumping = false;
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DeathFadeState.reset();
        AbilitySlots.reset();
        AbilityCooldowns.reset();
        net.noiilive.hahueuh.network.ClientConfigValues.clear();
        net.noiilive.hahueuh.client.MorningstarClient.reset();
        net.noiilive.hahueuh.client.GuiltywhipClient.reset();
        AbilityClient.resetChargeManaState();
        net.noiilive.hahueuh.network.DomainRenderState.clear();
        net.noiilive.hahueuh.network.ClientPlayerData.clear();
        net.noiilive.hahueuh.network.RemoteUnseenHands.clear();
        net.noiilive.hahueuh.network.ClientLionsHeartState.clear();
        net.noiilive.hahueuh.network.ClientLittleKingState.clear();
        net.noiilive.hahueuh.network.ClientFingerHighlightState.clear();
        net.noiilive.hahueuh.network.ClientMaterialPhaseState.clear();
        net.noiilive.hahueuh.network.ClientDualWieldState.clear();
        net.noiilive.hahueuh.network.ClientVisionOfDangerHighlightState.clear();
        net.noiilive.hahueuh.network.ClientVisionOfLifeGlowState.clear();
        net.noiilive.hahueuh.network.ClientFootprintState.clear();
        net.noiilive.hahueuh.network.ClientMurakState.clear();
        net.noiilive.hahueuh.network.EmtRenderState.clear();
        net.noiilive.hahueuh.client.MurakClient.reset();
    }

    private static void renderFade(GuiGraphics graphics) {
        float alpha = DeathFadeState.advanceAndGetAlpha();
        if (alpha <= 0.001f) return;

        graphics.flush();

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        Matrix4f matrix = graphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, 0.0f, height, 0.0f).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        buffer.vertex(matrix, width, height, 0.0f).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        buffer.vertex(matrix, width, 0.0f, 0.0f).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        buffer.vertex(matrix, 0.0f, 0.0f, 0.0f).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
