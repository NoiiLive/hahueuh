package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.noiilive.hahueuh.GuiltywhipItem;
import net.noiilive.hahueuh.GuiltywhipPhysics;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.client.model.WhipSegmentModel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class GuiltywhipClient {

    private static final Map<UUID, GuiltywhipPhysics.State> STATES = new ConcurrentHashMap<>();
    private static ModelPart segmentPart;
    private static boolean bakeFailed;

    private GuiltywhipClient() {}

    public static void reset() {
        STATES.clear();
    }

    private static boolean holding(Player player) {
        return player.getMainHandItem().getItem() instanceof GuiltywhipItem && !player.isSpectator();
    }

    public static void applyRemoteCrack(UUID owner, boolean sweep) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Player player = mc.level.getPlayerByUUID(owner);
        GuiltywhipPhysics.State state = STATES.get(owner);
        if (player == null || state == null) return;
        GuiltywhipPhysics.crack(state, player, sweep, HandTracker.resolve(player, 1.0f));
    }

    @SubscribeEvent
    public static void onClickInput(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !holding(player)) return;
        GuiltywhipPhysics.State state = STATES.get(player.getUUID());
        if (state == null || !GuiltywhipPhysics.canCrack(state)) return;
        boolean sweep = !player.isShiftKeyDown();
        GuiltywhipPhysics.crack(state, player, sweep, HandTracker.resolve(player, 1.0f));
        net.noiilive.hahueuh.network.ModNetworking.CHANNEL.sendToServer(
                new net.noiilive.hahueuh.network.GuiltywhipCrackPacket(sweep));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (!holding(player)) {
                STATES.remove(player.getUUID());
                continue;
            }
            Vec3 anchor = HandTracker.resolve(player, 1.0f);
            GuiltywhipPhysics.State state = STATES.computeIfAbsent(player.getUUID(),
                    k -> new GuiltywhipPhysics.State(anchor));
            GuiltywhipPhysics.step(state, anchor, player);
        }
        STATES.keySet().removeIf(uuid -> mc.level.getPlayerByUUID(uuid) == null);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || STATES.isEmpty()) return;
        if (!bakeModel()) return;

        float pt = event.getPartialTick();
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (AbstractClientPlayer player : mc.level.players()) {
            GuiltywhipPhysics.State state = STATES.get(player.getUUID());
            if (state == null || !holding(player)) continue;

            boolean foil = player.getMainHandItem().hasFoil();
            VertexConsumer buffer = foil
                    ? net.minecraft.client.renderer.entity.ItemRenderer.getFoilBufferDirect(buffers,
                            RenderType.entityCutoutNoCull(WhipSegmentModel.TEXTURE), false, true)
                    : buffers.getBuffer(RenderType.entityCutoutNoCull(WhipSegmentModel.TEXTURE));

            Vec3 anchor = HandTracker.resolve(player, pt);
            List<Vec3> curve = new java.util.ArrayList<>(GuiltywhipPhysics.NODES);
            curve.add(anchor);
            for (int i = 1; i < GuiltywhipPhysics.NODES; i++) {
                curve.add(new Vec3(
                        Mth.lerp(pt, state.prev[i].x, state.pos[i].x),
                        Mth.lerp(pt, state.prev[i].y, state.pos[i].y),
                        Mth.lerp(pt, state.prev[i].z, state.pos[i].z)));
            }

            Vec3 delta = curve.get(curve.size() - 1).subtract(anchor);
            if (delta.length() < 0.01) continue;
            Vec3 fallback = delta.normalize();

            List<RopeCurve.Placement> segments =
                    RopeCurve.layout(curve, WhipSegmentModel.SEGMENT_LENGTH, fallback);

            for (RopeCurve.Placement placement : segments) {
                Vec3 centre = placement.centre();
                int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(centre));
                pose.pushPose();
                pose.translate(centre.x - camPos.x, centre.y - camPos.y, centre.z - camPos.z);
                pose.mulPose(RopeCurve.rotationFromUp(placement.direction()));
                pose.scale(-1.0f, -1.0f, 1.0f);
                segmentPart.render(pose, buffer, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        buffers.endBatch();
    }

    private static boolean bakeModel() {
        if (bakeFailed) return false;
        if (segmentPart != null) return true;
        try {
            segmentPart = Minecraft.getInstance().getEntityModels().bakeLayer(WhipSegmentModel.LAYER);
            return true;
        } catch (Exception e) {
            bakeFailed = true;
            return false;
        }
    }
}
