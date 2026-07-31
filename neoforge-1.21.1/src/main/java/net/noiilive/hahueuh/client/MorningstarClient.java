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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.MorningstarItem;
import net.noiilive.hahueuh.MorningstarPhysics;
import net.noiilive.hahueuh.client.model.ChainLinkModel;
import net.noiilive.hahueuh.client.model.MorningstarHeadModel;
import net.noiilive.hahueuh.network.MorningstarSwingPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class MorningstarClient {

    private static final Map<UUID, MorningstarPhysics.State> STATES = new ConcurrentHashMap<>();
    private static ModelPart linkPart;
    private static ModelPart headPart;
    private static boolean bakeFailed;

    private MorningstarClient() {}

    public static void reset() {
        STATES.clear();
    }

    public static void applyRemoteSwing(UUID owner, boolean spin) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Player player = mc.level.getPlayerByUUID(owner);
        MorningstarPhysics.State state = STATES.get(owner);
        if (player == null || state == null) return;
        if (spin && MorningstarPhysics.tryQueueSpin(state)) return;
        MorningstarPhysics.applySwing(state, player, spin);
    }

    private static boolean holding(Player player) {
        return player.getMainHandItem().getItem() instanceof MorningstarItem && !player.isSpectator();
    }

    public static void applyHeadSync(UUID owner, Vec3 pos, Vec3 vel) {
        MorningstarPhysics.State state = STATES.get(owner);
        if (state == null || state.pendingThrow != null) return;
        state.vel = vel;
        state.pos = pos;
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ChainLinkModel.LAYER, ChainLinkModel::createBodyLayer);
        event.registerLayerDefinition(MorningstarHeadModel.LAYER, MorningstarHeadModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (!holding(player)) {
                STATES.remove(player.getUUID());
                continue;
            }
            Vec3 anchor = HandTracker.resolve(player, 1.0f);
            MorningstarPhysics.State state = STATES.computeIfAbsent(player.getUUID(),
                    k -> new MorningstarPhysics.State(anchor.add(0.0, -1.0, 0.0)));
            MorningstarPhysics.step(state, anchor, player);
        }
        STATES.keySet().removeIf(uuid -> mc.level.getPlayerByUUID(uuid) == null);
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !holding(player)) return;
        MorningstarPhysics.State state = STATES.get(player.getUUID());
        if (state == null) return;
        boolean spin = player.isShiftKeyDown();
        if (spin && MorningstarPhysics.tryQueueSpin(state)) {
            PacketDistributor.sendToServer(new MorningstarSwingPayload(true));
            return;
        }
        if (!MorningstarPhysics.canSwing(state)) return;
        MorningstarPhysics.applySwing(state, player, spin);
        PacketDistributor.sendToServer(new MorningstarSwingPayload(spin));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || STATES.isEmpty()) return;
        if (!bakeModels()) return;

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (AbstractClientPlayer player : mc.level.players()) {
            MorningstarPhysics.State state = STATES.get(player.getUUID());
            if (state == null || !holding(player)) continue;

            boolean foil = player.getMainHandItem().hasFoil();
            VertexConsumer chainBuffer = foil
                    ? net.minecraft.client.renderer.entity.ItemRenderer.getFoilBufferDirect(buffers,
                            RenderType.entityCutoutNoCull(ChainLinkModel.TEXTURE), false, true)
                    : buffers.getBuffer(RenderType.entityCutoutNoCull(ChainLinkModel.TEXTURE));

            Vec3 anchor = HandTracker.resolve(player, pt);
            Vec3 head = new Vec3(
                    Mth.lerp(pt, state.prevPos.x, state.pos.x),
                    Mth.lerp(pt, state.prevPos.y, state.pos.y),
                    Mth.lerp(pt, state.prevPos.z, state.pos.z));

            Vec3 delta = head.subtract(anchor);
            if (delta.length() < 0.01) continue;

            Vec3 fallback = delta.normalize();
            java.util.List<Vec3> curve = new java.util.ArrayList<>(MorningstarPhysics.CHAIN_NODES);
            curve.add(anchor);
            for (int i = 1; i < MorningstarPhysics.CHAIN_NODES - 1; i++) {
                curve.add(new Vec3(
                        Mth.lerp(pt, state.chainPrev[i].x, state.chain[i].x),
                        Mth.lerp(pt, state.chainPrev[i].y, state.chain[i].y),
                        Mth.lerp(pt, state.chainPrev[i].z, state.chain[i].z)));
            }
            curve.add(head);
            java.util.List<RopeCurve.Placement> links =
                    RopeCurve.layout(curve, ChainLinkModel.LINK_LENGTH, fallback);
            if (links.isEmpty()) continue;

            for (RopeCurve.Placement placement : links) {
                Vec3 centre = placement.centre();
                Vec3 linkDir = placement.direction();
                int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(centre));
                pose.pushPose();
                pose.translate(centre.x - camPos.x, centre.y - camPos.y, centre.z - camPos.z);
                pose.mulPose(RopeCurve.rotationFromUp(linkDir));
                pose.scale(-1.0f, -1.0f, 1.0f);
                linkPart.render(pose, chainBuffer, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }

            Vec3 tailRef = curve.get(Math.max(0, curve.size() - 4));
            Vec3 lastDir = head.subtract(tailRef);
            lastDir = lastDir.lengthSqr() < 1.0e-8 ? fallback : lastDir.normalize();

            VertexConsumer headBuffer = foil
                    ? net.minecraft.client.renderer.entity.ItemRenderer.getFoilBufferDirect(buffers,
                            RenderType.entityCutoutNoCull(MorningstarHeadModel.TEXTURE), false, true)
                    : buffers.getBuffer(RenderType.entityCutoutNoCull(MorningstarHeadModel.TEXTURE));
            Vec3 up = lastDir.scale(-1.0);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(head));
            pose.pushPose();
            pose.translate(head.x - camPos.x, head.y - camPos.y, head.z - camPos.z);
            pose.mulPose(RopeCurve.rotationFromUp(up));
            pose.translate(0.0, -(MorningstarHeadModel.TOP_SPIKE_TIP - 3.0F / 16.0F), 0.0);
            pose.scale(-1.0f, -1.0f, 1.0f);
            headPart.render(pose, headBuffer, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch();
    }

    private static boolean bakeModels() {
        if (bakeFailed) return false;
        if (linkPart != null && headPart != null) return true;
        try {
            linkPart = Minecraft.getInstance().getEntityModels().bakeLayer(ChainLinkModel.LAYER);
            headPart = Minecraft.getInstance().getEntityModels().bakeLayer(MorningstarHeadModel.LAYER);
            return true;
        } catch (Exception e) {
            bakeFailed = true;
            return false;
        }
    }
}
