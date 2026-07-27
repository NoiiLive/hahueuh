package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModEffects;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class SensoryDeprivationClient {
    private static final float FADE_SECONDS = 0.5f;

    private static float overlayAlpha;
    private static long lastFrameNanos;

    private SensoryDeprivationClient() {}

    public static boolean isDeprived() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(ModEffects.SENSORY_DEPRIVATION.get());
    }

    public static void renderOverlay(GuiGraphics graphics) {
        boolean active = isDeprived() || BodilyDisconnectClient.disconnected();

        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0f : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        dt = Math.min(dt, 0.1f);

        float rate = 1f / FADE_SECONDS;
        float target = active ? 1f : 0f;
        if (overlayAlpha < target) overlayAlpha = Math.min(target, overlayAlpha + rate * dt);
        else if (overlayAlpha > target) overlayAlpha = Math.max(target, overlayAlpha - rate * dt);

        if (overlayAlpha <= 0.001f) return;

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
        buffer.vertex(matrix, 0.0f, height, 0.0f).color(0.0f, 0.0f, 0.0f, overlayAlpha).endVertex();
        buffer.vertex(matrix, width, height, 0.0f).color(0.0f, 0.0f, 0.0f, overlayAlpha).endVertex();
        buffer.vertex(matrix, width, 0.0f, 0.0f).color(0.0f, 0.0f, 0.0f, overlayAlpha).endVertex();
        buffer.vertex(matrix, 0.0f, 0.0f, 0.0f).color(0.0f, 0.0f, 0.0f, overlayAlpha).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!isDeprived()) return;
        Input input = event.getInput();
        input.forwardImpulse = -input.forwardImpulse;
        input.leftImpulse = -input.leftImpulse;

        boolean up = input.up;
        input.up = input.down;
        input.down = up;
        boolean left = input.left;
        input.left = input.right;
        input.right = left;
    }
}
