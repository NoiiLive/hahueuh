package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.ModSounds;
import net.noiilive.hahueuh.api.AbilityCooldowns;
import net.noiilive.hahueuh.network.ClientFingerState;
import net.noiilive.hahueuh.network.ClientSlothState;
import net.noiilive.hahueuh.network.HandMode;
import net.noiilive.hahueuh.network.RemoteUnseenHands;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.UnseenHandPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.noiilive.hahueuh.network.ModNetworking;

import java.util.Map;
import java.util.UUID;

final class SlothHandController {
    static final SlothHandController INSTANCE = new SlothHandController();

    private static final float QUICK_SPEED_BOOST = 1.5f;
    private static final double RETRACT_DONE_DISTANCE = 0.35;

    private boolean summonToggled;
    private boolean pendingSummonToggle;
    private boolean selfPropelHeld;
    private boolean pendingQuickRequested;
    private HandMode pendingQuickMode = HandMode.NONE;

    private boolean quickSequenceActive;
    private HandMode quickMode = HandMode.NONE;
    private boolean quickRetracting;

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

    void requestSummonToggle() {
        pendingSummonToggle = true;
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

        boolean slothUnseenHands = ClientSlothState.canSloth()
                && ClientSlothState.slothVariant() == SlothVariant.UNSEEN_HANDS;
        boolean fingerRecipient = ClientFingerState.hasHands();
        boolean canSloth = ClientSlothState.canSloth() || fingerRecipient;
        boolean actsAsUnseenHands = slothUnseenHands || fingerRecipient;
        int effectiveHands = slothUnseenHands ? ClientSlothState.handCount()
                : fingerRecipient ? ClientFingerState.hands() : 0;
        boolean canMobility = actsAsUnseenHands && effectiveHands >= 2;
        boolean summonOnCooldown = AbilityCooldowns.secondsRemaining(HahUeuhAbilities.SLOTH_COOLDOWN_KEY) > 0 && !player.isCreative();
        boolean quickOnCooldown = AbilityCooldowns.secondsRemaining(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY) > 0 && !player.isCreative();

        if (!canSloth) summonToggled = false;

        if (pendingSummonToggle) {
            pendingSummonToggle = false;
            if (summonToggled) {
                summonToggled = false;
                playHandSound(player, resolveUseSound());
            } else if (canSloth && !summonOnCooldown) {
                summonToggled = true;
                playHandSound(player, resolveSummonSound());
            } else if (summonOnCooldown) {
                player.displayClientMessage(Component.translatable("hahueuh.message.sloth_cooldown",
                                AbilityCooldowns.secondsRemaining(HahUeuhAbilities.SLOTH_COOLDOWN_KEY))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }

        boolean wantsSummon = summonToggled && canSloth && !summonOnCooldown;
        boolean wantsSelfPropel = selfPropelHeld && canSloth && !quickOnCooldown && canMobility;

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
                    summonToggled = false;
                    retractingGrab = false;
                    lastModeWhileHeld = HandMode.NONE;
                }
                playHandSound(player, resolveUseSound());
            }
        }

        boolean quickHeldPhase = quickSequenceActive && !quickRetracting;

        boolean mobilityMode = (wantsSummon && player.isShiftKeyDown() && canMobility) || wantsSelfPropel;
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

    private static void setLocalCrouching(LocalPlayer player, boolean value) {
        ((net.noiilive.hahueuh.mixin.LocalPlayerAccessor) player).hahueuh$setCrouching(value);
    }

    private void forceLocalNotCrouching(LocalPlayer player) {
        setLocalCrouching(player, false);
    }

    private static void playHandSound(LocalPlayer player, SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(
                sound, SoundSource.PLAYERS, 1.0f, 1.0f, player, player.getRandom().nextLong()));
    }

    private static SoundEvent resolveSummonSound() {
        if (!ClientSlothState.canSloth()) return ModSounds.SLOTH_HAND_SUMMON.get();
        return switch (ClientSlothState.slothVariant()) {
            case UNSEEN_HANDS -> ModSounds.UNSEEN_HAND_SUMMON.get();
            case INVISIBLE_PROVIDENCE -> ModSounds.INVISIBLE_PROVIDENCE_SUMMON.get();
            case SEKHMET -> ModSounds.SEKHMET_SUMMON.get();
        };
    }

    private static SoundEvent resolveUseSound() {
        if (!ClientSlothState.canSloth()) return ModSounds.SLOTH_HAND_USE.get();
        return switch (ClientSlothState.slothVariant()) {
            case UNSEEN_HANDS -> ModSounds.UNSEEN_HAND_USE.get();
            case INVISIBLE_PROVIDENCE -> ModSounds.INVISIBLE_PROVIDENCE_USE.get();
            case SEKHMET -> ModSounds.SEKHMET_USE.get();
        };
    }

    private void syncUnseenHandToServer(Minecraft mc) {
        boolean active = UnseenHandState.isServerActive();
        float distance = (float) UnseenHandState.liveDistance();
        HandMode mode = UnseenHandState.mode();
        boolean mobility = UnseenHandState.isMobility();
        boolean quickSession = active && !sessionIsSummon;
        handSyncCounter++;
        boolean changed = active != lastSentHandActive || mode != lastSentHandMode || mobility != lastSentMobility
                || Math.abs(distance - lastSentHandDistance) > 0.05f;
        boolean periodic = active && (handSyncCounter % 2 == 0);
        if ((changed || periodic) && mc.getConnection() != null) {
            ModNetworking.CHANNEL.sendToServer(new UnseenHandPacket(active, distance, mode.ordinal(), mobility, quickSession));
            lastSentHandActive = active;
            lastSentHandDistance = distance;
            lastSentHandMode = mode;
            lastSentMobility = mobility;
        }
    }
}
