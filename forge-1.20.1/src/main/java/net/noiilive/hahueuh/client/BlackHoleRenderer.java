package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.BlackHoleEntity;
import net.noiilive.hahueuh.client.model.BlackHoleModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    private static final float Y_OFFSET = 2.5F;

    private final BlackHoleModel model;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BlackHoleModel(context.bakeLayer(BlackHoleModel.LAYER));
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return BlackHoleModel.TEXTURE;
    }

    @Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float scale = entity.renderScale(partialTicks);
        if (scale <= 0.001f) return;

        poseStack.pushPose();
        poseStack.translate(0.0F, Y_OFFSET, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(scale, scale, scale);

        float ageInTicks = entity.tickCount + partialTicks;
        model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(BlackHoleModel.TEXTURE));
        model.renderToBuffer(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.popPose();
    }
}
