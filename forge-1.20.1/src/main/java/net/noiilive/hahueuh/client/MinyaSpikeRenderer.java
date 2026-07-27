package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.noiilive.hahueuh.MinyaSpikeEntity;
import net.noiilive.hahueuh.client.model.MinyaSpikeModel;
import org.joml.Quaternionf;

public final class MinyaSpikeRenderer extends EntityRenderer<MinyaSpikeEntity> {
    private static final float AXIS_CENTER = 1.15f;
    private static final float STAKE_SCALE = 0.8f;
    private static final float SHARD_SCALE = 0.5f;

    private final ModelPart spike;

    public MinyaSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.spike = context.bakeLayer(MinyaSpikeModel.LAYER);
    }

    @Override
    public ResourceLocation getTextureLocation(MinyaSpikeEntity entity) {
        return MinyaSpikeModel.TEXTURE;
    }

    @Override
    public void render(MinyaSpikeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 dir = flightDirection(entity, partialTicks);
        float scale = entity.isShard() ? SHARD_SCALE : STAKE_SCALE;

        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() * 0.5, 0.0);

        Quaternionf orient = new Quaternionf().rotationTo(
                0.0f, 1.0f, 0.0f, (float) dir.x, (float) dir.y, (float) dir.z);
        poseStack.mulPose(orient);

        poseStack.mulPose(new Quaternionf().rotateY((entity.tickCount + partialTicks) * 0.4f));

        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0f, -AXIS_CENTER, 0.0f);

        var vc = buffer.getBuffer(RenderType.entityTranslucent(MinyaSpikeModel.TEXTURE));
        spike.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.popPose();
    }

    private static Vec3 flightDirection(MinyaSpikeEntity entity, float partialTicks) {
        if (entity.isCharging()) {
            return Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
        }
        Vec3 delta = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        if (delta.lengthSqr() < 1.0e-6) {
            delta = entity.getDeltaMovement();
        }
        if (delta.lengthSqr() < 1.0e-6) {
            return Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
        }
        return delta.normalize();
    }
}
