package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.noiilive.hahueuh.PocketVoidBlockEntity;
import org.joml.Matrix4f;

public final class PocketVoidRenderer implements BlockEntityRenderer<PocketVoidBlockEntity> {
    public PocketVoidRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(PocketVoidBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.endPortal());
        face(pose, consumer, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f);
        face(pose, consumer, 0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f);
        face(pose, consumer, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 0f);
        face(pose, consumer, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f);
        face(pose, consumer, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 1f);
        face(pose, consumer, 0f, 1f, 1f, 1f, 1f, 1f, 0f, 0f);
    }

    private static void face(Matrix4f pose, VertexConsumer consumer,
                             float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        consumer.vertex(pose, x0, y0, z0).endVertex();
        consumer.vertex(pose, x1, y0, z1).endVertex();
        consumer.vertex(pose, x1, y1, z2).endVertex();
        consumer.vertex(pose, x0, y1, z3).endVertex();
    }
}
