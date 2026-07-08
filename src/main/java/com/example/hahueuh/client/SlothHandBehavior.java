package com.example.hahueuh.client;

import com.example.hahueuh.Config;
import com.example.hahueuh.HahUeuh;
import com.example.hahueuh.api.AbilityBehavior;
import com.example.hahueuh.api.AbilityContext;
import com.example.hahueuh.network.ClientSlothState;
import com.example.hahueuh.network.HandMode;
import com.example.hahueuh.network.RemoteUnseenHands;
import com.example.hahueuh.network.SlothVariant;
import com.example.hahueuh.network.UnseenHandPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;

final class SlothHandBehavior implements AbilityBehavior.Held {
    private boolean wasHeld;
    private boolean wasServerActive;
    private boolean retractingGrab;
    private boolean wasMobility;
    private HandMode lastModeWhileHeld = HandMode.NONE;
    private boolean lastSentHandActive;
    private float lastSentHandDistance;
    private HandMode lastSentHandMode = HandMode.NONE;
    private boolean lastSentMobility;
    private int handSyncCounter;

    private static final java.lang.reflect.Field LOCAL_CROUCHING_FIELD = resolveLocalCrouchingField();

    @Override
    public void onHeldTick(AbilityContext ctx, boolean down) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            UnseenHandState.setActive(false);
            return;
        }

        boolean variantIsUnseenHands = ClientSlothState.slothVariant() == SlothVariant.UNSEEN_HANDS;

        boolean onCooldown = ctx.isOnCooldown() && !player.isCreative();
        boolean held = down && ClientSlothState.canSloth() && !onCooldown;

        if (down && onCooldown) {
            player.displayClientMessage(Component.translatable("hahueuh.message.sloth_cooldown", ctx.cooldownSecondsRemaining())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }

        boolean mobilityMode = held && player.isShiftKeyDown() && variantIsUnseenHands;
        if (mobilityMode != wasMobility) {
            player.displayClientMessage(Component.translatable(mobilityMode
                    ? "hahueuh.message.hands_anchored" : "hahueuh.message.hands_released")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            player.setForcedPose(mobilityMode ? Pose.STANDING : null);
        }
        wasMobility = mobilityMode;
        if (mobilityMode) {
            SlothVariant.freezeWalkAnimation(player);
            forceLocalNotCrouching(player);
        }

        HandMode mode = HandMode.NONE;
        if (held && !mobilityMode) {
            if (mc.options.keyAttack.isDown()) mode = HandMode.ATTACK;
            else if (mc.options.keyUse.isDown()) mode = HandMode.GRAB;
        }

        if (held) {
            retractingGrab = false;
            lastModeWhileHeld = mode;
        } else {
            if (wasHeld && lastModeWhileHeld == HandMode.GRAB) retractingGrab = true;
            if (retractingGrab) {
                mode = HandMode.GRAB;
                if (UnseenHandState.liveDistance() < 0.35) retractingGrab = false;
            }
        }
        wasHeld = held;

        boolean serverActive = held || retractingGrab;
        if (wasServerActive && !serverActive && !player.isCreative()) {
            ctx.startCooldown(Config.SLOTH_COOLDOWN_SECONDS.getAsInt());
        }
        wasServerActive = serverActive;

        UnseenHandState.setActive(held);
        UnseenHandState.setServerActive(serverActive);
        UnseenHandState.setMode(mode);
        UnseenHandState.setMobility(mobilityMode);

        syncUnseenHandToServer(mc);

        for (Map.Entry<UUID, RemoteUnseenHands.Remote> e : RemoteUnseenHands.active().entrySet()) {
            if (!e.getValue().mobility()) continue;
            Player remote = mc.level == null ? null : mc.level.getPlayerByUUID(e.getKey());
            if (remote != null) SlothVariant.freezeWalkAnimation(remote);
        }
    }

    private static java.lang.reflect.Field resolveLocalCrouchingField() {
        try {
            java.lang.reflect.Field f = LocalPlayer.class.getDeclaredField("crouching");
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            HahUeuh.LOGGER.warn("Could not resolve LocalPlayer.crouching field; mobility idle-pose will show a crouch bend", e);
            return null;
        }
    }

    private void forceLocalNotCrouching(LocalPlayer player) {
        if (LOCAL_CROUCHING_FIELD == null) return;
        try {
            LOCAL_CROUCHING_FIELD.setBoolean(player, false);
        } catch (Exception ignored) {
        }
    }

    private void syncUnseenHandToServer(Minecraft mc) {
        boolean active = UnseenHandState.isServerActive();
        float distance = (float) UnseenHandState.liveDistance();
        HandMode mode = UnseenHandState.mode();
        boolean mobility = UnseenHandState.isMobility();
        handSyncCounter++;
        boolean changed = active != lastSentHandActive || mode != lastSentHandMode || mobility != lastSentMobility
                || Math.abs(distance - lastSentHandDistance) > 0.05f;
        boolean periodic = active && (handSyncCounter % 2 == 0);
        if ((changed || periodic) && mc.getConnection() != null) {
            PacketDistributor.sendToServer(new UnseenHandPayload(active, distance, mode.ordinal(), mobility));
            lastSentHandActive = active;
            lastSentHandDistance = distance;
            lastSentHandMode = mode;
            lastSentMobility = mobility;
        }
    }
}
