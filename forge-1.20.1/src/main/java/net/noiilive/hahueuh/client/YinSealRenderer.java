package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.YinSealEntity;
import net.noiilive.hahueuh.client.model.YinSealModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class YinSealRenderer extends EntityRenderer<YinSealEntity> {
    private final YinSealModel model;

    public YinSealRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new YinSealModel(context.bakeLayer(YinSealModel.LAYER));
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(YinSealEntity entity) {
        return YinSealModel.TEXTURE;
    }

    @Override
    public void render(YinSealEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float scale = entity.renderScale(partialTicks);
        if (scale <= 0.001f) return;

        poseStack.pushPose();
        poseStack.translate(0.0F, scale + entity.getBbHeight() * 0.5F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(scale, scale, scale);

        float ageInTicks = entity.tickCount + partialTicks;
        model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(YinSealModel.TEXTURE));
        model.renderToBuffer(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.popPose();
    }
}
