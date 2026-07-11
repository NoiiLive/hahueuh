package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.ConfigSloth;
import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.api.AbilityCooldowns;
import net.noiilive.hahueuh.network.ClientSlothState;
import net.noiilive.hahueuh.network.HandMode;
import net.noiilive.hahueuh.network.RemoteUnseenHands;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.UnseenHandPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;

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
    private boolean sessionIsSummon;
    private boolean retractingGrab;
    private boolean wasMobility;
    private HandMode lastModeWhileHeld = HandMode.NONE;
    private boolean lastSentHandActive;
    private float lastSentHandDistance;
    private HandMode lastSentHandMode = HandMode.NONE;
    private boolean lastSentMobility;
    private int handSyncCounter;

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
        boolean summonOnCooldown = AbilityCooldowns.secondsRemaining(HahUeuhAbilities.SLOTH_COOLDOWN_KEY) > 0 && !player.isCreative();
        boolean quickOnCooldown = AbilityCooldowns.secondsRemaining(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY) > 0 && !player.isCreative();

        if (!summonHeld) summonSuppressedUntilRelease = false;

        boolean wantsSummon = summonHeld && canSloth && !summonOnCooldown && !summonSuppressedUntilRelease;
        boolean wantsSelfPropel = selfPropelHeld && canSloth && !quickOnCooldown && variantIsUnseenHands;

        if (summonHeld && summonOnCooldown) {
            player.displayClientMessage(Component.translatable("hahueuh.message.sloth_cooldown",
                            AbilityCooldowns.secondsRemaining(HahUeuhAbilities.SLOTH_COOLDOWN_KEY))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        if (selfPropelHeld && quickOnCooldown) {
            player.displayClientMessage(Component.translatable("hahueuh.message.quick_action_cooldown",
                            AbilityCooldowns.secondsRemaining(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }

        boolean cancelingActiveSummon = wasServerActive && sessionIsSummon;
        if (pendingQuickRequested) {
            pendingQuickRequested = false;
            if (quickOnCooldown && !cancelingActiveSummon) {
                player.displayClientMessage(Component.translatable("hahueuh.message.quick_action_cooldown",
                                AbilityCooldowns.secondsRemaining(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            } else if (canSloth) {
                quickSequenceActive = true;
                quickMode = pendingQuickMode;
                quickRetracting = wantsSummon || cancelingActiveSummon;
                if (wantsSummon || cancelingActiveSummon) {
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
        if (serverActive && !wasServerActive) {
            sessionIsSummon = wantsSummon || cancelingActiveSummon;
        }
        if (wasServerActive && !serverActive && !player.isCreative()) {
            if (sessionIsSummon) {
                AbilityCooldowns.startCooldown(HahUeuhAbilities.SLOTH_COOLDOWN_KEY, ConfigSloth.SLOTH_COOLDOWN_SECONDS.getAsInt());
            } else {
                AbilityCooldowns.startCooldown(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY, ConfigSloth.QUICK_ACTION_COOLDOWN_SECONDS.getAsInt());
            }
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

    private void forceLocalNotCrouching(LocalPlayer player) {
        player.crouching = false;
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
