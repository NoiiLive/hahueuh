package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.GreedVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MentalOverload {
    private static final double RANGE = 16.0;
    private static final int EFFECT_DURATION_TICKS = 200; // 10 seconds

    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private MinecraftServer server;

    public void activate(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) != GreedVariant.ECHIDNA) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!HahUeuh.BOOK_OF_WISDOM.isHoldingOwnBook(player)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.echidna_needs_book")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = player.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            player.displayClientMessage(Component.translatable("hahueuh.message.mental_overload_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        LivingEntity target = raycastTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("hahueuh.message.mental_overload_no_target")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (isExemptEchidna(target)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.mental_overload_exempt")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        applyInsanity(target);
        startCooldown(player);
        player.displayClientMessage(Component.translatable("hahueuh.message.mental_overload_activated",
                target.getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private LivingEntity raycastTarget(ServerPlayer player) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player,
                e -> e != player && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity, RANGE);
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public static boolean isExemptEchidna(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return false;
        UUID uuid = player.getUUID();
        return HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                && HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) == GreedVariant.ECHIDNA;
    }

    public static void applyInsanity(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, EFFECT_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, EFFECT_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_DURATION_TICKS, 0, false, true, true));
    }

    private void startCooldown(ServerPlayer player) {
        if (server == null || player.isCreative()) return;
        int cooldownSeconds = ConfigGreed.MENTAL_OVERLOAD_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds <= 0) return;
        cooldownUntilTick.put(player.getUUID(), server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(player.getUUID(), cooldownSeconds * 20));
        PacketDistributor.sendToPlayer(player,
                new AbilityCooldownPayload(HahUeuhAbilities.MENTAL_OVERLOAD_ABILITY, HahUeuh.GREED_COMPAT.scaleCooldownTicks(player.getUUID(), cooldownSeconds * 20)));
    }

    private int cooldownRemainingTicks(UUID uuid) {
        Integer until = cooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    public Map<UUID, Integer> captureCooldownRemaining() {
        Map<UUID, Integer> result = new HashMap<>();
        if (server == null) return result;
        int tick = server.getTickCount();
        cooldownUntilTick.forEach((uuid, until) -> {
            int remaining = until - tick;
            if (remaining > 0) result.put(uuid, remaining);
        });
        return result;
    }

    public void restoreCooldownRemaining(Map<UUID, Integer> remainingByUuid) {
        if (server == null) return;
        cooldownUntilTick.clear();
        int tick = server.getTickCount();
        remainingByUuid.forEach((uuid, remaining) -> cooldownUntilTick.put(uuid, tick + remaining));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int remaining = remainingByUuid.getOrDefault(player.getUUID(), 0);
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.MENTAL_OVERLOAD_ABILITY, remaining));
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        cooldownUntilTick.clear();
        this.server = null;
    }
}
