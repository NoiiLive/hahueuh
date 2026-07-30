package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class Insanity {

    private final Map<UUID, Double> drawProgress = new ConcurrentHashMap<>();
    private final Map<UUID, Double> foodProgress = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> nextEventDelay = new ConcurrentHashMap<>();
    private final List<StepDown> pendingStepDown = new ArrayList<>();

    private record StepDown(LivingEntity entity, int tier) {}

    public static int maxLevel() {
        return Math.max(1, ConfigPlayer.INSANITY_MAX_LEVEL.get());
    }

    private static int durationTicks() {
        return Math.max(1, ConfigPlayer.INSANITY_DURATION_SECONDS.get()) * 20;
    }

    public int level(LivingEntity entity) {
        MobEffectInstance existing = entity.getEffect(ModEffects.INSANITY.get());
        return existing == null ? 0 : existing.getAmplifier() + 1;
    }

    public void applyLevel(LivingEntity entity, int level) {
        applyLevel(entity, level, true);
    }

    private void applyLevel(LivingEntity entity, int level, boolean announce) {
        if (isExempt(entity)) return;
        int clamped = Math.max(1, Math.min(maxLevel(), level));
        entity.forceAddEffect(new MobEffectInstance(ModEffects.INSANITY.get(),
                durationTicks(), clamped - 1, false, true, true), null);
        MobEffectInstance applied = entity.getEffect(ModEffects.INSANITY.get());
        boolean landed = applied != null && applied.getAmplifier() == clamped - 1
                && applied.getDuration() >= durationTicks() - 1;
        if (announce && landed && entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("hahueuh.message.insanity_rising", clamped)
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    public void raiseBy(LivingEntity entity, int tiers) {
        if (tiers <= 0 || isExempt(entity)) return;
        applyLevel(entity, level(entity) + tiers);
    }

    public void raiseToMax(LivingEntity entity) {
        if (isExempt(entity)) return;
        applyLevel(entity, maxLevel());
    }

    public void addDrawExposure(ServerPlayer player, double amount) {
        if (amount <= 0.0 || isExempt(player)) return;
        double perTier = Math.max(1.0, ConfigPlayer.INSANITY_MIASMA_DRAW_PER_TIER.get());
        double carried = drawProgress.merge(player.getUUID(), amount, Double::sum);
        if (carried < perTier) return;
        drawProgress.put(player.getUUID(), carried - perTier);
        raiseBy(player, 1);
    }

    public void addContaminatedMeal(LivingEntity entity) {
        if (isExempt(entity)) return;
        double perTier = Math.max(1.0, ConfigPlayer.INSANITY_FOOD_MEALS_PER_TIER.get());
        double carried = foodProgress.merge(entity.getUUID(), 1.0, Double::sum);
        if (carried < perTier) return;
        foodProgress.put(entity.getUUID(), carried - perTier);
        raiseBy(entity, 1);
    }

    private static boolean isExempt(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.isCreative() || player.isSpectator();
        }
        return false;
    }

    public void tickAfflicted(LivingEntity entity, int amplifier) {
        if (isExempt(entity)) return;
        int tier = amplifier + 1;
        UUID id = entity.getUUID();
        int delay = nextEventDelay.getOrDefault(id, 0);
        if (delay > 0) {
            nextEventDelay.put(id, delay - 1);
            return;
        }
        nextEventDelay.put(id, rollInterval(tier));
        fireEpisode(entity, tier);
    }

    private static int rollInterval(int tier) {
        int base = Math.max(20, ConfigPlayer.INSANITY_EPISODE_BASE_SECONDS.get() * 20);
        int floor = Math.max(10, base / (tier + 1));
        int span = Math.max(10, base - floor);
        return floor + ThreadLocalRandom.current().nextInt(span + 1);
    }

    private void fireEpisode(LivingEntity entity, int tier) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int duration = (40 + rng.nextInt(120)) * Math.max(1, tier / 2 + 1);
        int amp = Math.max(0, (tier - 1) / 2);

        switch (rng.nextInt(5)) {
            case 0 -> nudge(entity, MobEffects.CONFUSION, duration, 0);
            case 1 -> nudge(entity, MobEffects.HUNGER, duration, amp);
            case 2 -> nudge(entity, MobEffects.WEAKNESS, duration, amp);
            case 3 -> nudge(entity, MobEffects.MOVEMENT_SLOWDOWN, duration, amp);
            default -> nudge(entity, MobEffects.DIG_SLOWDOWN, duration, amp);
        }

        if (tier >= ConfigPlayer.INSANITY_DAMAGE_TIER.get()) {
            bleed(entity, tier);
        }
        if (tier >= ConfigPlayer.INSANITY_CONTROL_LOSS_TIER.get()
                && rng.nextInt(100) < ConfigPlayer.INSANITY_CONTROL_LOSS_CHANCE.get()) {
            int lossTicks = 30 + rng.nextInt(50) * Math.max(1, tier - 2);
            entity.addEffect(new MobEffectInstance(ModEffects.BODILY_DISCONNECT.get(),
                    lossTicks, 0, false, true, true));
        }
    }

    private static void nudge(LivingEntity entity, net.minecraft.world.effect.MobEffect effect,
                              int duration, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
    }

    private static void bleed(LivingEntity entity, int tier) {
        float damage = tier - ConfigPlayer.INSANITY_DAMAGE_TIER.get() + 1;
        if (damage <= 0f) return;
        if (entity.getHealth() - damage <= 1.0f) return;
        entity.hurt(entity.damageSources().magic(), damage);
    }

    public void clear(LivingEntity entity) {
        entity.removeEffect(ModEffects.INSANITY.get());
        forget(entity.getUUID());
    }

    private void forget(UUID id) {
        drawProgress.remove(id);
        foodProgress.remove(id);
        nextEventDelay.remove(id);
    }

    @SubscribeEvent
    public void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect() != ModEffects.INSANITY.get()) return;
        if (instance.getAmplifier() <= 0) return;
        if (event.getEntity().level().isClientSide) return;
        pendingStepDown.add(new StepDown(event.getEntity(), instance.getAmplifier()));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingStepDown.isEmpty()) return;
        List<StepDown> drain = new ArrayList<>(pendingStepDown);
        pendingStepDown.clear();
        for (StepDown sd : drain) {
            if (sd.entity().isAlive() && !sd.entity().isRemoved()) {
                applyLevel(sd.entity(), sd.tier(), false);
            }
        }
    }

    @SubscribeEvent
    public void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        net.minecraft.world.level.GameType mode = event.getNewGameMode();
        if (mode != net.minecraft.world.level.GameType.CREATIVE
                && mode != net.minecraft.world.level.GameType.SPECTATOR) return;
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isExempt(player)) clear(player);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        drawProgress.clear();
        foodProgress.clear();
        nextEventDelay.clear();
        pendingStepDown.clear();
    }
}
