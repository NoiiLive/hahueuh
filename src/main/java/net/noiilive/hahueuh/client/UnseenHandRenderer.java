package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.client.animation.UnseenHandAnimations;
import net.noiilive.hahueuh.client.model.UnseenHandHierModel;
import net.noiilive.hahueuh.client.model.UnseenHandModel;
import net.noiilive.hahueuh.client.model.UnseenTendrilModel;
import net.noiilive.hahueuh.network.ClientSlothState;
import net.noiilive.hahueuh.network.RemoteUnseenHands;
import net.noiilive.hahueuh.network.SlothVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class UnseenHandRenderer {
    private UnseenHandRenderer() {}

    private static final int MAX_SEGMENTS = 200;

    private static final float HAND_RATE = 12f;
    private static final float BEND_RATE = 5f;
    private static final float ANIM_BLEND_RATE = 14f;
    private static final float TIP_TRANSITION_RATE = 10f;
    private static final float MAX_TIP_LAG_FRACTION = 0.2f;

    private static final float EXTEND_SPEED = 8f;
    private static final float RETRACT_SPEED = 24f;
    private static final float FADE_SPEED = 4f;

    private static final float SEKHMET_SPEED_MUL = 3.0f;

    private static final float STEP_CYCLES_PER_SEC = 1f;
    private static final float STEP_FORWARD_AMPLITUDE_RATIO = 2.8f / 3f;
    private static final float STEP_LIFT_HEIGHT_RATIO = 2.5f / 3f;
    private static final float STEP_DUTY = 0.55f;

    private static final float WIGGLE_SPATIAL_FREQ = 3.2f;
    private static final float WIGGLE_SPEED = 9f;
    private static final float WIGGLE_MAX_AMPLITUDE = 0.45f;

    private static final float IP_WIGGLE_SPATIAL_FREQ = 2.2f;
    private static final float IP_WIGGLE_SPEED = 9f;
    private static final float IP_WIGGLE_MAX_AMPLITUDE = 0.5f;

    private static UnseenHandHierModel handModel;
    private static ModelPart tendrilPart;
    private static final Map<VisualKey, HandVisual> VISUALS = new HashMap<>();
    private static long lastFrameNanos;
    private static final Vector3f ANIM_VEC = new Vector3f();

    private record VisualKey(UUID owner, int slot) {}

    private record Instance(Player owner, double target, int modeId, float scale, float speedMul, boolean mirror,
                            HandShape shape, boolean mobility) {}

    private record HandShape(float heightFrac, float lateral, float back, float distBias, float tipSplay,
                             float tipRise, float bowConst, float bowPerDist, float controlBack, float stepPhase,
                             int handIndex) {
        static final HandShape CHEST = new HandShape(0.55f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(UnseenHandModel.LAYER, UnseenHandModel::createBodyLayer);
        event.registerLayerDefinition(UnseenTendrilModel.LAYER, UnseenTendrilModel::createBodyLayer);
    }

    private static UnseenHandHierModel handModel() {
        if (handModel == null) {
            try { handModel = new UnseenHandHierModel(Minecraft.getInstance().getEntityModels().bakeLayer(UnseenHandModel.LAYER)); }
            catch (Exception e) { HahUeuh.LOGGER.error("Failed to bake unseen_hand layer", e); }
        }
        return handModel;
    }

    private static ModelPart tendrilPart() {
        if (tendrilPart == null) {
            try { tendrilPart = Minecraft.getInstance().getEntityModels().bakeLayer(UnseenTendrilModel.LAYER); }
            catch (Exception e) { HahUeuh.LOGGER.error("Failed to bake unseen_tendril layer", e); }
        }
        return tendrilPart;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { VISUALS.clear(); return; }
        if (!ClientSlothState.canSloth()) { VISUALS.clear(); return; }

        UnseenHandHierModel hand = handModel();
        ModelPart tendril = tendrilPart();
        if (hand == null || tendril == null) return;

        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0f : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        dt = Math.min(dt, 0.1f);

        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camPos = event.getCamera().getPosition();

        UUID self = mc.player.getUUID();

        Map<VisualKey, Instance> instances = new HashMap<>();
        for (Map.Entry<UUID, RemoteUnseenHands.Remote> e : RemoteUnseenHands.active().entrySet()) {
            Player owner = e.getKey().equals(self) ? mc.player : mc.level.getPlayerByUUID(e.getKey());
            if (owner == null) continue;
            addInstances(instances, e.getKey(), owner, e.getValue().distance(),
                    e.getValue().mode(), SlothVariant.byOrdinal(e.getValue().variant()), e.getValue().mobility(), 1f);
        }
        if (UnseenHandState.isActive()) {
            addInstances(instances, self, mc.player, (float) UnseenHandState.maxRange(),
                    UnseenHandState.mode().ordinal(), ClientSlothState.slothVariant(), UnseenHandState.isMobility(),
                    UnseenHandState.speedBoost());
        }

        Set<VisualKey> all = new HashSet<>(instances.keySet());
        all.addAll(VISUALS.keySet());
        if (all.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucent(UnseenHandModel.TEXTURE);
        VertexConsumer main = buffers.getBuffer(renderType);

        mc.levelRenderer.requestOutlineEffect();
        OutlineBufferSource outlineSource = mc.renderBuffers().outlineBufferSource();

        boolean anyDrawn = false;
        for (VisualKey key : all) {
            Instance inst = instances.get(key);
            Player owner = key.owner().equals(self) ? mc.player : mc.level.getPlayerByUUID(key.owner());
            boolean isHeld = inst != null && owner != null;
            double target = isHeld ? inst.target() : 0.0;

            HandVisual visual = VISUALS.computeIfAbsent(key, k -> new HandVisual());
            if (isHeld) {
                visual.scale = inst.scale();
                visual.shape = inst.shape();
                visual.speedMul = inst.speedMul();
                visual.heldModeId = inst.modeId();
                visual.mirror = inst.mirror();
                visual.mobility = inst.mobility();
                visual.targetDistance = target;
            }
            visual.advance(isHeld, target, dt);

            List<Integer> grabbedIds = RemoteUnseenHands.grabbedFor(key.owner());
            visual.grabbedEntityId = key.slot() < grabbedIds.size() ? grabbedIds.get(key.slot()) : -1;

            if (key.owner().equals(self) && key.slot() == 0) UnseenHandState.setLiveDistance(visual.distance);

            if (owner == null || (!isHeld && visual.fade <= 0.01f)) {
                VISUALS.remove(key);
                continue;
            }
            if (visual.fade <= 0.01f) continue;

            visual.render(owner, visual.heldModeId, dt, pt, pose, camPos, hand, tendril, main, outlineSource, mc.level);
            anyDrawn = true;
        }

        if (anyDrawn) buffers.endBatch(renderType);
    }

    private static void addInstances(Map<VisualKey, Instance> out, UUID id, Player owner,
                                     float target, int modeId, SlothVariant variant, boolean mobility, float extraSpeedMul) {
        if (variant == SlothVariant.SEKHMET) {
            float size = SlothVariant.sekhmetSize(id);
            float off = SlothVariant.sekhmetShoulderOffset(size);
            float splay = SlothVariant.sekhmetHandSplay(size);
            float back = (float) SlothVariant.SEKHMET_BACK_OFFSET;
            float height = (float) SlothVariant.SEKHMET_SHOULDER_HEIGHT;
            out.put(new VisualKey(id, 0), new Instance(owner, target, modeId, size, SEKHMET_SPEED_MUL * extraSpeedMul, true,
                    new HandShape(height, -off, back, 0f, -splay, 0f, -(splay + 0.3f * size), -0.2f, 0f, 0f, 0), false));
            out.put(new VisualKey(id, 1), new Instance(owner, target, modeId, size, SEKHMET_SPEED_MUL * extraSpeedMul, false,
                    new HandShape(height, off, back, 0f, splay, 0f, splay + 0.3f * size, 0.2f, 0f, 0f, 0), false));
        } else if (variant == SlothVariant.UNSEEN_HANDS) {
            int count = SlothVariant.unseenHandCount(id);
            for (int i = 0; i < count; i++) {
                HandShape shape = new HandShape(
                        SlothVariant.UNSEEN_ANCHOR_HEIGHT,
                        0f,
                        (float) SlothVariant.UNSEEN_HAND_BACK,
                        SlothVariant.unseenHandDistBias(id, i),
                        SlothVariant.unseenHandSideOffset(id, i),
                        SlothVariant.unseenHandRise(id, i),
                        SlothVariant.unseenHandBow(id, i),
                        0f,
                        SlothVariant.UNSEEN_CONTROL_BACK,
                        SlothVariant.unseenHandStepPhase(id, i, count),
                        i);
                boolean mirror = SlothVariant.unseenHandSide(i) < 0f;
                out.put(new VisualKey(id, i), new Instance(owner, target, modeId, 1f, extraSpeedMul, mirror, shape, mobility));
            }
        } else {
            out.put(new VisualKey(id, 0), new Instance(owner, target, modeId, 1f, extraSpeedMul, false, HandShape.CHEST, false));
        }
    }

    private static final class HandVisual {
        private boolean initialized;
        private float smoothYaw, smoothPitch;
        private float bendYaw, bendPitch;
        private double distance;
        private float fade;
        private boolean wasHeld;
        private double releaseDistance;
        private float releaseFade;
        private float animTimeMs;
        private int lastModeId = -1;
        private int lastLagModeId = Integer.MIN_VALUE;
        private boolean lastLagMobility;
        private boolean settledSinceTransition = true;
        private final Map<ModelPart, float[]> blendPose = new java.util.IdentityHashMap<>();
        float scale = 1f;
        HandShape shape = HandShape.CHEST;
        float speedMul = 1f;
        int heldModeId = 0;
        boolean mirror = false;
        boolean mobility = false;
        int grabbedEntityId = -1;
        private Vec3 smoothedTip;
        double targetDistance;

        void advance(boolean held, double target, float dt) {
            float extend = EXTEND_SPEED * speedMul;
            float retract = RETRACT_SPEED * speedMul;
            if (held) {
                if (distance < target) distance = Math.min(target, distance + extend * dt);
                else distance = Math.max(target, distance - retract * dt);
                fade = Math.min(1f, fade + FADE_SPEED * dt);
                wasHeld = true;
            } else {
                if (wasHeld) { releaseDistance = distance; releaseFade = fade; wasHeld = false; }
                distance = Math.max(0.0, distance - retract * dt);
                fade = releaseDistance > 0.01 ? (float) Math.min(1f, releaseFade * (distance / releaseDistance)) : 0f;
            }
        }

        void render(Player owner, int modeId, float dt, float pt, PoseStack pose, Vec3 camPos,
                    UnseenHandHierModel hand, ModelPart tendril, VertexConsumer main, OutlineBufferSource outlineSource,
                    net.minecraft.client.multiplayer.ClientLevel level) {
            float targetYaw = owner.getViewYRot(pt);
            float targetPitch = owner.getViewXRot(pt);
            if (!initialized) {
                smoothYaw = bendYaw = targetYaw;
                smoothPitch = bendPitch = targetPitch;
                initialized = true;
            } else {
                smoothYaw = approachAngle(smoothYaw, targetYaw, HAND_RATE, dt);
                smoothPitch = approachAngle(smoothPitch, targetPitch, HAND_RATE, dt);
                bendYaw = approachAngle(bendYaw, targetYaw, BEND_RATE, dt);
                bendPitch = approachAngle(bendPitch, targetPitch, BEND_RATE, dt);
            }

            double ox = Mth.lerp(pt, owner.xo, owner.getX());
            double oy = Mth.lerp(pt, owner.yo, owner.getY());
            double oz = Mth.lerp(pt, owner.zo, owner.getZ());
            float yawRad = (float) Math.toRadians(smoothYaw);
            Vec3 rightVec = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
            Vec3 fwdFlat = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
            Vec3 anchor = new Vec3(ox, oy + owner.getBbHeight() * shape.heightFrac(), oz)
                    .add(rightVec.scale(shape.lateral()))
                    .subtract(fwdFlat.scale(shape.back()));

            Vec3 handDir = dirFromAngles(smoothYaw, smoothPitch);
            Vec3 bendDir = dirFromAngles(bendYaw, bendPitch);
            double dist = Math.max(0.05, distance + shape.distBias());
            Vec3 p0 = anchor;
            Vec3 p2;
            Entity grabbedEntity = grabbedEntityId >= 0 ? level.getEntity(grabbedEntityId) : null;
            if (grabbedEntity != null) {
                Vec3 center = grabbedEntity.getPosition(pt).add(0, grabbedEntity.getBbHeight() / 2.0, 0);
                double halfWidth = grabbedEntity.getBbWidth() / 2.0;
                double clampedSplay = Mth.clamp(shape.tipSplay(), -halfWidth, halfWidth);
                p2 = center.add(rightVec.scale(clampedSplay));
            } else if (mobility) {
                double tipX = anchor.x + rightVec.x * shape.tipSplay();
                double tipZ = anchor.z + rightVec.z * shape.tipSplay();

                float stepAmplitude = Math.min(5f, STEP_FORWARD_AMPLITUDE_RATIO * (float) distance);
                float stepLift = Math.min(5f, STEP_LIFT_HEIGHT_RATIO * (float) distance);

                float cyclePos = (animTimeMs / 1000f) * STEP_CYCLES_PER_SEC + shape.stepPhase();
                float u = cyclePos - (float) Math.floor(cyclePos);
                float stepFwd, lift;
                if (u < STEP_DUTY) {
                    float s = u / STEP_DUTY;
                    stepFwd = stepAmplitude * (1f - 2f * s);
                    lift = 0f;
                } else {
                    float s = (u - STEP_DUTY) / (1f - STEP_DUTY);
                    stepFwd = stepAmplitude * (2f * s - 1f);
                    lift = stepLift * (float) Math.sin(Math.PI * s);
                }
                tipX += fwdFlat.x * stepFwd;
                tipZ += fwdFlat.z * stepFwd;

                double groundY = SlothVariant.findGroundY(level, tipX, owner.getY(), tipZ,
                        SlothVariant.UNSEEN_MOBILITY_GROUND_SCAN);
                if (Double.isNaN(groundY)) groundY = owner.getY();
                p2 = new Vec3(tipX, groundY + lift, tipZ);
            } else if (modeId == 1 && shape.controlBack() > 0f) {
                double timeSec = (level.getGameTime() + pt) / 20.0;
                UUID ownerId = owner.getUUID();
                float flailYaw = smoothYaw + SlothVariant.unseenHandFlailYaw(ownerId, shape.handIndex(), timeSec);
                float flailPitch = smoothPitch + SlothVariant.unseenHandFlailPitch(ownerId, shape.handIndex(), timeSec);
                double flailDist = Math.max(0.3, dist * SlothVariant.unseenHandFlailReachMul(ownerId, shape.handIndex(), timeSec));
                Vec3 rawTip = anchor.add(dirFromAngles(flailYaw, flailPitch).scale(flailDist));
                double groundY = SlothVariant.findGroundY(level, rawTip.x, owner.getY(), rawTip.z,
                        SlothVariant.UNSEEN_MOBILITY_GROUND_SCAN);
                p2 = Double.isNaN(groundY) ? rawTip : new Vec3(rawTip.x, Math.max(rawTip.y, groundY), rawTip.z);
            } else {
                p2 = anchor.add(handDir.scale(dist))
                        .add(rightVec.scale(shape.tipSplay()))
                        .add(0, shape.tipRise(), 0);
            }

            if (modeId != lastLagModeId || mobility != lastLagMobility) {
                lastLagModeId = modeId;
                lastLagMobility = mobility;
                settledSinceTransition = false;
            }

            if (smoothedTip == null) {
                smoothedTip = p2;
            } else {
                float tipBlend = 1f - (float) Math.exp(-TIP_TRANSITION_RATE * dt);
                smoothedTip = new Vec3(
                        Mth.lerp(tipBlend, smoothedTip.x, p2.x),
                        Mth.lerp(tipBlend, smoothedTip.y, p2.y),
                        Mth.lerp(tipBlend, smoothedTip.z, p2.z));
                Vec3 lag = smoothedTip.subtract(p2);
                double maxLag = dist * MAX_TIP_LAG_FRACTION;
                double lagLen = lag.length();
                if (!settledSinceTransition && lagLen <= maxLag) {
                    settledSinceTransition = true;
                }
                if (settledSinceTransition && lagLen > maxLag && lagLen > 1.0e-6) {
                    smoothedTip = p2.add(lag.scale(maxLag / lagLen));
                }
            }
            p2 = smoothedTip;

            Vec3 p1;
            if (shape.controlBack() > 0f) {
                p1 = anchor.subtract(fwdFlat.scale(shape.controlBack())).add(rightVec.scale(shape.bowConst()));
            } else {
                p1 = anchor.add(bendDir.scale(dist * 0.5))
                        .add(rightVec.scale(shape.bowConst() + shape.bowPerDist() * (float) dist));
            }

            float wiggleAmp = 0f, wiggleFreq = 0f, wiggleSpeed = 0f;
            if (modeId == 1 && shape.controlBack() > 0f) {
                wiggleAmp = WIGGLE_MAX_AMPLITUDE;
                wiggleFreq = WIGGLE_SPATIAL_FREQ;
                wiggleSpeed = WIGGLE_SPEED;
            } else if (shape.equals(HandShape.CHEST)) {
                double progress = targetDistance > 0.01 ? Mth.clamp(distance / targetDistance, 0.0, 1.0) : 0.0;
                wiggleAmp = (float) ((1.0 - progress) * IP_WIGGLE_MAX_AMPLITUDE);
                wiggleFreq = IP_WIGGLE_SPATIAL_FREQ;
                wiggleSpeed = IP_WIGGLE_SPEED;
            }
            Vec3 wiggleAxisA = Vec3.ZERO, wiggleAxisB = Vec3.ZERO;
            if (wiggleAmp > 0.001f) {
                Vec3 curveDir = p2.subtract(p0);
                double curveLen = curveDir.length();
                Vec3 curveDirN = curveLen > 1.0e-6 ? curveDir.scale(1.0 / curveLen) : new Vec3(0, 1, 0);
                Vec3 upRef = Math.abs(curveDirN.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                wiggleAxisA = curveDirN.cross(upRef).normalize();
                wiggleAxisB = curveDirN.cross(wiggleAxisA);
            }

            int alpha = (int) (fade * 255f) & 0xFF;
            int mainColor = (alpha << 24) | 0xFFFFFF;
            outlineSource.setColor(0xB6, 0x29, 0xFC, alpha);
            VertexConsumer outline = outlineSource.getBuffer(RenderType.outline(UnseenHandModel.TEXTURE));

            final int SAMPLES = 64;
            double[] arc = new double[SAMPLES + 1];
            Vec3 prevPt = p0;
            for (int k = 1; k <= SAMPLES; k++) {
                Vec3 curPt = curvePoint(p0, p1, p2, (float) k / SAMPLES, wiggleAmp, wiggleFreq, wiggleSpeed, wiggleAxisA, wiggleAxisB);
                arc[k] = arc[k - 1] + curPt.distanceTo(prevPt);
                prevPt = curPt;
            }
            double arcLen = arc[SAMPLES];
            double segLen = UnseenTendrilModel.SEGMENT_LENGTH * scale;
            int nSlots = (int) Math.max(1, Math.min(MAX_SEGMENTS, Math.round(arcLen / segLen)));
            segLen = arcLen / nSlots;

            animTimeMs += dt * 1000f;
            if (modeId != lastModeId) { animTimeMs = 0f; lastModeId = modeId; }
            AnimationDefinition anim = switch (modeId) {
                case 1 -> UnseenHandAnimations.fist;
                case 2 -> UnseenHandAnimations.grip;
                default -> UnseenHandAnimations.idle;
            };
            hand.root().getAllParts().forEach(ModelPart::resetPose);
            KeyframeAnimations.animate(hand, anim, (long) animTimeMs, 1.0f, ANIM_VEC);
            float blend = 1f - (float) Math.exp(-ANIM_BLEND_RATE * dt);
            hand.root().getAllParts().forEach(p -> {
                float[] prev = blendPose.get(p);
                if (prev == null) {
                    blendPose.put(p, new float[]{p.x, p.y, p.z, p.xRot, p.yRot, p.zRot, p.xScale, p.yScale, p.zScale});
                } else {
                    p.x = prev[0] = Mth.lerp(blend, prev[0], p.x);
                    p.y = prev[1] = Mth.lerp(blend, prev[1], p.y);
                    p.z = prev[2] = Mth.lerp(blend, prev[2], p.z);
                    p.xRot = prev[3] = Mth.lerp(blend, prev[3], p.xRot);
                    p.yRot = prev[4] = Mth.lerp(blend, prev[4], p.yRot);
                    p.zRot = prev[5] = Mth.lerp(blend, prev[5], p.zRot);
                    p.xScale = prev[6] = Mth.lerp(blend, prev[6], p.xScale);
                    p.yScale = prev[7] = Mth.lerp(blend, prev[7], p.yScale);
                    p.zScale = prev[8] = Mth.lerp(blend, prev[8], p.zScale);
                }
            });

            Vec3 pBase = p0;
            for (int i = 0; i < nSlots; i++) {
                Vec3 pTip = curvePoint(p0, p1, p2, tAtArc(arc, (i + 1) * segLen), wiggleAmp, wiggleFreq, wiggleSpeed, wiggleAxisA, wiggleAxisB);
                Vec3 chord = pTip.subtract(pBase);
                double chordLen = chord.length();
                Vec3 chordDir = chordLen > 1.0e-6 ? chord.scale(1.0 / chordLen) : handDir;
                float zFill = (float) (chordLen / UnseenTendrilModel.SEGMENT_LENGTH);
                Vec3 origin = pBase.add(chordDir.scale(chordLen / 6.0));
                boolean isHand = i == nSlots - 1;
                float zScale = isHand ? (float) (segLen / UnseenTendrilModel.SEGMENT_LENGTH) : zFill;
                pose.pushPose();
                pose.translate(origin.x - camPos.x, origin.y - camPos.y, origin.z - camPos.z);
                pose.mulPose(chainOrientation(chordDir));
                pose.scale(isHand && mirror ? scale : -scale, -scale, zScale);
                drawPart(isHand ? hand.root() : tendril, pose, main, outline, mainColor);
                pose.popPose();
                pBase = pTip;
            }
        }

        private void drawPart(ModelPart part, PoseStack pose, VertexConsumer main, VertexConsumer outline, int color) {
            part.render(pose, main, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
            part.render(pose, outline, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }

        private static float approachAngle(float current, float target, float rate, float dt) {
            float diff = Mth.wrapDegrees(target - current);
            return current + diff * (1f - (float) Math.exp(-rate * dt));
        }

        private static float tAtArc(double[] arc, double target) {
            int samples = arc.length - 1;
            double total = arc[samples];
            if (target <= 0 || total <= 0) return 0f;
            if (target >= total) return 1f;
            int k = 1;
            while (k < samples && arc[k] < target) k++;
            double a0 = arc[k - 1], a1 = arc[k];
            double frac = a1 > a0 ? (target - a0) / (a1 - a0) : 0.0;
            return (float) ((k - 1 + frac) / samples);
        }

        private static Quaternionf chainOrientation(Vec3 fwd) {
            Vec3 upRef = Math.abs(fwd.y) > 0.99 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
            Vec3 r = fwd.cross(upRef).normalize();
            Vec3 u = r.cross(fwd);
            Matrix3f m = new Matrix3f().set(
                    (float) r.x, (float) r.y, (float) r.z,
                    (float) u.x, (float) u.y, (float) u.z,
                    (float) -fwd.x, (float) -fwd.y, (float) -fwd.z);
            return new Quaternionf().setFromNormalized(m);
        }

        private static Vec3 dirFromAngles(float yawDeg, float pitchDeg) {
            float yaw = (float) Math.toRadians(yawDeg);
            float pitch = (float) Math.toRadians(pitchDeg);
            double cp = Math.cos(pitch);
            return new Vec3(-Math.sin(yaw) * cp, -Math.sin(pitch), Math.cos(yaw) * cp);
        }

        private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
            float u = 1f - t;
            return p0.scale(u * u).add(p1.scale(2f * u * t)).add(p2.scale(t * t));
        }

        private Vec3 curvePoint(Vec3 p0, Vec3 p1, Vec3 p2, float t, float wiggleAmp, float spatialFreq, float speed,
                                Vec3 axisA, Vec3 axisB) {
            Vec3 base = bezier(p0, p1, p2, t);
            if (wiggleAmp <= 0.001f) return base;
            double phase = t * spatialFreq + (animTimeMs / 1000.0) * speed;
            double amp = wiggleAmp * t;
            double a = Math.sin(phase);
            double b = Math.sin(phase * 1.37 + 2.1);
            return base.add(axisA.scale(amp * a)).add(axisB.scale(amp * b));
        }
    }
}
