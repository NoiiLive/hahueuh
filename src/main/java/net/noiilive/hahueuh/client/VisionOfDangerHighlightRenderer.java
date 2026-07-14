package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.ClientVisionOfDangerHighlightState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class VisionOfDangerHighlightRenderer {
    private VisionOfDangerHighlightRenderer() {}

    private static final int R = 255;
    private static final int G = 225;
    private static final int B = 25;
    private static final int A = 255;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (ClientVisionOfDangerHighlightState.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        mc.levelRenderer.requestOutlineEffect();
        OutlineBufferSource outlineSource = mc.renderBuffers().outlineBufferSource();
        outlineSource.setColor(R, G, B, A);

        boolean anyDrawn = false;
        for (int id : ClientVisionOfDangerHighlightState.highlighted()) {
            Entity entity = level.getEntity(id);
            if (entity == null || !entity.isAlive() || entity == mc.player) continue;

            double ex = Mth.lerp(pt, entity.xOld, entity.getX()) - cam.x;
            double ey = Mth.lerp(pt, entity.yOld, entity.getY()) - cam.y;
            double ez = Mth.lerp(pt, entity.zOld, entity.getZ()) - cam.z;
            float yaw = Mth.lerp(pt, entity.yRotO, entity.getYRot());
            int light = dispatcher.getPackedLightCoords(entity, pt);

            pose.pushPose();
            dispatcher.render(entity, ex, ey, ez, yaw, pt, pose, outlineSource, light);
            pose.popPose();
            anyDrawn = true;
        }

        if (anyDrawn) {
            outlineSource.endOutlineBatch();
        }
    }
}
