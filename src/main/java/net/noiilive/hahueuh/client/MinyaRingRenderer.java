package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.noiilive.hahueuh.MinyaRingEntity;
import net.noiilive.hahueuh.client.model.MinyaRingModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class MinyaRingRenderer extends EntityRenderer<MinyaRingEntity> {
    private static final float MODEL_PIVOT_Y = 1.5F;

    private static final int BEAM_COLOR = 0xFFE874CB;
    private static final int BEAM_TOP_REACH = 320;
    private static final float BEAM_RADIUS = 1.2F;
    private static final float BEAM_GLOW_RADIUS = 1.8F;

    private final ModelPart ring;

    public MinyaRingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.ring = context.bakeLayer(MinyaRingModel.LAYER);
    }

    @Override
    public ResourceLocation getTextureLocation(MinyaRingEntity entity) {
        return MinyaRingModel.TEXTURE;
    }

    @Override
    public void render(MinyaRingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float formScale = entity.renderScale(partialTicks);
        if (formScale > 0.001f) {
            renderRing(entity, formScale, poseStack, buffer, packedLight);
        }
        if (entity.isBeaming()) {
            renderBeam(entity, partialTicks, poseStack, buffer);
        }
    }

    private void renderRing(MinyaRingEntity entity, float formScale, PoseStack poseStack,
                            MultiBufferSource buffer, int packedLight) {
        float scale = entity.radius() * 2.0f * formScale;

        poseStack.pushPose();
        poseStack.translate(0.0F, MODEL_PIVOT_Y * scale, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(scale, scale, scale);

        var vc = buffer.getBuffer(RenderType.entityTranslucent(MinyaRingModel.TEXTURE));
        ring.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }

    private void renderBeam(MinyaRingEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        long gameTime = entity.level().getGameTime();
        int bottom = -Math.round(entity.anchorDrop());
        int height = BEAM_TOP_REACH - bottom;

        poseStack.pushPose();
        poseStack.translate(-0.5, 0.0, -0.5);
        BeaconRenderer.renderBeaconBeam(poseStack, buffer, BeaconRenderer.BEAM_LOCATION,
                partialTicks, 1.0F, gameTime, bottom, height, BEAM_COLOR,
                BEAM_RADIUS, BEAM_GLOW_RADIUS);
        poseStack.popPose();
    }
}
