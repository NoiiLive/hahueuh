package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.WitchFactorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EndermiteModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class WitchFactorRenderer extends EntityRenderer<WitchFactorEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/endermite.png");

    private final EndermiteModel<WitchFactorEntity> model;

    public WitchFactorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EndermiteModel<>(context.bakeLayer(ModelLayers.ENDERMITE));
    }

    @Override
    public ResourceLocation getTextureLocation(WitchFactorEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(WitchFactorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
        if (!(buffer instanceof OutlineBufferSource outline)) return;

        poseStack.pushPose();

        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
        float netHeadYaw = headYaw - bodyYaw;
        float xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float ageInTicks = entity.tickCount + partialTicks;
        float limbSwingAmount = Math.min(1.0f, entity.walkAnimation.speed(partialTicks));
        float limbSwing = entity.walkAnimation.position(partialTicks);

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);

        model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, xRot);

        VertexConsumer vertexConsumer = outline.getBuffer(RenderType.outline(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }
}
