package com.example.hahueuh.client;

import com.example.hahueuh.Config;
import com.example.hahueuh.HahUeuh;
import com.example.hahueuh.HahUeuhAbilities;
import com.example.hahueuh.api.AbilityCooldowns;
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

/**
 * Coordinates all 5 Sloth abilities (Summon Hand, Quick Strike, Quick Grasp, Hidden Interaction,
 * Self Propel) into one unified hand state, since they all drive the same underlying
 * {@link UnseenHandState} / server hand. Each ability's behavior just reports raw input intent
 * here; {@link #tick(LocalPlayer)} resolves it once per client tick.
 */
final class SlothHandController {
    static final SlothHandController INSTANCE = new SlothHandController();

    private static final float QUICK_SPEED_BOOST = 1.5f;
    private static final double RETRACT_DONE_DISTANCE = 0.35;

    private boolean summonHeld;
    private boolean selfPropelHeld;
    private boolean pendingQuickRequested;
    private HandMode pendingQuickMode = HandMode.NONE;

    private boolean quickSequenceActive;
    private HandMode quickMode = HandMode.NONE;
    private boolean quickRetracting;
    private boolean summonSuppressedUntilRelease;

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

    private SlothHandController() {}

    void reportSummonHeld(boolean down) {
        summonHeld = down;
    }

    void reportSelfPropelHeld(boolean down) {
        selfPropelHeld = down;
    }

    void requestQuickAction(HandMode mode) {
        if (quickSequenceActive) return;
        pendingQuickRequested = true;
        pendingQuickMode = mode;
    }

    void requestHiddenInteraction() {
        if (ClientSlothState.slothVariant() != SlothVariant.INVISIBLE_PROVIDENCE) return;
        requestQuickAction(HandMode.NONE);
    }

    void tick(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();

        boolean variantIsUnseenHands = ClientSlothState.slothVariant() == SlothVariant.UNSEEN_HANDS;
        boolean canSloth = ClientSlothState.canSloth();
        boolean onCooldown = AbilityCooldowns.secondsRemaining(HahUeuhAbilities.SLOTH_COOLDOWN_KEY) > 0 && !player.isCreative();

        if (!summonHeld) summonSuppressedUntilRelease = false;

        boolean wantsSummon = summonHeld && canSloth && !onCooldown && !summonSuppressedUntilRelease;
        boolean wantsSelfPropel = selfPropelHeld && canSloth && !onCooldown && variantIsUnseenHands;

        if ((summonHeld || selfPropelHeld) && onCooldown) {
            player.displayClientMessage(Component.translatable("hahueuh.message.sloth_cooldown",
                            AbilityCooldowns.secondsRemaining(HahUeuhAbilities.SLOTH_COOLDOWN_KEY))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }

        if (pendingQuickRequested) {
            pendingQuickRequested = false;
            if (onCooldown) {
                player.displayClientMessage(Component.translatable("hahueuh.message.sloth_cooldown",
                                AbilityCooldowns.secondsRemaining(HahUeuhAbilities.SLOTH_COOLDOWN_KEY))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            } else if (canSloth) {
                quickSequenceActive = true;
                quickMode = pendingQuickMode;
                quickRetracting = wantsSummon;
                if (wantsSummon) {
                    // Summon Hand was actively held — cancel it outright rather than letting it
                    // resume on this same continuous hold once the quick action's fast retract
                    // finishes; the player must release and re-press Summon Hand to reactivate it.
                    summonSuppressedUntilRelease = true;
                    retractingGrab = false;
                    lastModeWhileHeld = HandMode.NONE;
                }
            }
        }

        boolean quickHeldPhase = quickSequenceActive && !quickRetracting;

        boolean mobilityMode = (wantsSummon && player.isShiftKeyDown() && variantIsUnseenHands) || wantsSelfPropel;
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

        HandMode mode;
        if (quickSequenceActive) {
            mode = quickMode;
        } else if (wantsSummon && !mobilityMode) {
            if (mc.options.keyAttack.isDown()) mode = HandMode.ATTACK;
            else if (mc.options.keyUse.isDown()) mode = HandMode.GRAB;
            else mode = HandMode.NONE;
        } else {
            mode = HandMode.NONE;
        }

        boolean held = quickHeldPhase || (wantsSummon && !quickSequenceActive) || wantsSelfPropel;

        if (!quickSequenceActive) {
            if (held) {
                retractingGrab = false;
                lastModeWhileHeld = mode;
            } else {
                if (wasHeld && lastModeWhileHeld == HandMode.GRAB) retractingGrab = true;
                if (retractingGrab) {
                    mode = HandMode.GRAB;
                    if (UnseenHandState.liveDistance() < RETRACT_DONE_DISTANCE) retractingGrab = false;
                }
            }
        }
        wasHeld = held;

        if (quickSequenceActive && !quickRetracting && UnseenHandState.liveDistance() >= UnseenHandState.maxRange() - 0.05) {
            quickRetracting = true;
        }
        if (quickSequenceActive && quickRetracting && UnseenHandState.liveDistance() < RETRACT_DONE_DISTANCE) {
            quickSequenceActive = false;
            quickMode = HandMode.NONE;
        }

        boolean serverActive = held || retractingGrab || (quickSequenceActive && quickRetracting);
        if (wasServerActive && !serverActive && !player.isCreative()) {
            AbilityCooldowns.startCooldown(HahUeuhAbilities.SLOTH_COOLDOWN_KEY, Config.SLOTH_COOLDOWN_SECONDS.getAsInt());
        }
        wasServerActive = serverActive;

        UnseenHandState.setActive(held);
        UnseenHandState.setServerActive(serverActive);
        UnseenHandState.setMode(mode);
        UnseenHandState.setMobility(mobilityMode);
        UnseenHandState.setSpeedBoost(quickSequenceActive ? QUICK_SPEED_BOOST : 1f);

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
