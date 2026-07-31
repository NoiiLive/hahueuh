package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.noiilive.hahueuh.GuiltywhipItem;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.MorningstarItem;
import net.noiilive.hahueuh.MorningstarPhysics;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class HandTracker {

    private static final Map<UUID, Vec3> HANDS = new ConcurrentHashMap<>();
    private static boolean worldPass;

    private HandTracker() {}

    public static Vec3 resolve(Player player, float partialTick) {
        Vec3 captured = HANDS.get(player.getUUID());
        return captured != null ? captured : MorningstarPhysics.handAnchor(player, partialTick);
    }

    public static void reset() {
        HANDS.clear();
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            HANDS.clear();
            worldPass = true;
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            worldPass = false;
        }
    }

    public static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                           AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!worldPass) return;
            Item item = player.getMainHandItem().getItem();
            if (!(item instanceof MorningstarItem) && !(item instanceof GuiltywhipItem)) return;

            HumanoidArm arm = player.getMainArm();
            poseStack.pushPose();
            getParentModel().translateToHand(arm, poseStack);
            poseStack.translate(arm == HumanoidArm.RIGHT ? -1.0 / 16.0 : 1.0 / 16.0, 10.0 / 16.0, -4.75 / 16.0);
            Matrix4f pose = poseStack.last().pose();
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            HANDS.put(player.getUUID(),
                    new Vec3(cam.x + pose.m30(), cam.y + pose.m31(), cam.z + pose.m32()));
            poseStack.popPose();
        }
    }
}
