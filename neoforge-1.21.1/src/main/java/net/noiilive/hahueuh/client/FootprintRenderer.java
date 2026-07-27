package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.FootprintTracker;
import net.noiilive.hahueuh.client.model.FootprintModel;
import net.noiilive.hahueuh.network.ClientFootprintState;
import net.noiilive.hahueuh.network.FootprintSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class FootprintRenderer {
    private FootprintRenderer() {}

    private static final double PICK_MAX_DISTANCE = 12.0;
    private static final double PICK_RADIUS = 0.45;
    private static final float MIN_ALPHA = 0.1f;

    private static ModelPart model;
    private static volatile String hoveredName;
    private static volatile long hoveredTimestamp;

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FootprintModel.LAYER, FootprintModel::createBodyLayer);
    }

    private static ModelPart model() {
        if (model == null) {
            try {
                model = Minecraft.getInstance().getEntityModels().bakeLayer(FootprintModel.LAYER);
            } catch (Exception e) {
                HahUeuh.LOGGER.error("Failed to bake footprint layer", e);
            }
        }
        return model;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        List<FootprintSyncPayload.Footprint> footprints = ClientFootprintState.footprints();
        if (footprints.isEmpty()) {
            hoveredName = null;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            hoveredName = null;
            return;
        }

        ModelPart part = model();
        if (part == null) return;

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        long nowGameTime = level.getGameTime();
        int maxAge = Math.max(1, ClientFootprintState.maxAgeTicks());

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (FootprintSyncPayload.Footprint f : footprints) {
            float ageFrac = (float) (nowGameTime - f.timestamp()) / maxAge;
            float alpha = Mth.clamp(1.0f - ageFrac, MIN_ALPHA, 1.0f);
            int color = ((int) (alpha * 255.0f) & 0xFF) << 24 | 0x00FFFFFF;

            var buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(textureFor(f.category())));

            pose.pushPose();
            pose.translate(f.x() - cam.x, f.y() - cam.y, f.z() - cam.z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0f - f.yaw()));
            pose.scale(-1.0f, -1.0f, 1.0f);
            pose.translate(0.0f, -1.501f, 0.0f);
            part.render(pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
            pose.popPose();
        }

        buffers.endBatch();

        updateHover(mc, pt, footprints);
    }

    private static void updateHover(Minecraft mc, float pt, List<FootprintSyncPayload.Footprint> footprints) {
        Vec3 eye = mc.player.getEyePosition(pt);
        Vec3 look = mc.player.getViewVector(pt);

        FootprintSyncPayload.Footprint best = null;
        double bestDist = PICK_MAX_DISTANCE;
        for (FootprintSyncPayload.Footprint f : footprints) {
            Vec3 center = new Vec3(f.x(), f.y() + 0.15, f.z());
            Vec3 toCenter = center.subtract(eye);
            double proj = toCenter.dot(look);
            if (proj <= 0.0 || proj >= bestDist) continue;
            Vec3 onRay = eye.add(look.scale(proj));
            if (center.distanceTo(onRay) > PICK_RADIUS) continue;
            best = f;
            bestDist = proj;
        }
        hoveredName = best == null ? null : best.name();
        hoveredTimestamp = best == null ? 0L : best.timestamp();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        String name = hoveredName;
        if (name == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        int seconds = (int) (Math.max(0L, mc.level.getGameTime() - hoveredTimestamp) / 20L);

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int x = g.guiWidth() / 2;
        int y = g.guiHeight() / 2 + 10;
        g.drawCenteredString(font, Component.literal(name), x, y, 0xFFE0E0E0);
        g.drawCenteredString(font, Component.translatable("hahueuh.footprint.seconds_ago", seconds), x, y + 10, 0xFFA0A0A0);
    }

    private static ResourceLocation textureFor(int category) {
        return switch (category) {
            case FootprintTracker.CATEGORY_HOSTILE -> FootprintModel.TEXTURE_RED;
            case FootprintTracker.CATEGORY_PLAYER -> FootprintModel.TEXTURE_WHITE;
            case FootprintTracker.CATEGORY_WITCH_FACTOR -> FootprintModel.TEXTURE_PURPLE;
            default -> FootprintModel.TEXTURE_GREEN;
        };
    }
}
