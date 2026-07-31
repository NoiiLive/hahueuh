package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.network.PlayerStat;
import net.noiilive.hahueuh.network.StatEntry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatEffects {
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private static final UUID HEALTH_ID = UUID.fromString("2f1c0a6e-2f7a-4f0f-9a55-0c1c1e6b0a01");
    private static final UUID SPEED_ID = UUID.fromString("2f1c0a6e-2f7a-4f0f-9a55-0c1c1e6b0a02");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("2f1c0a6e-2f7a-4f0f-9a55-0c1c1e6b0a03");

    private final Map<String, Double> progressRemainder = new ConcurrentHashMap<>();

    public static void refresh(ServerPlayer player) {
        applyAttributes(player);
    }

    private static void applyAttributes(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);

        setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, "hahueuh:stat_tenacity_health",
                StatBonuses.bonus(data, PlayerStat.TENACITY, ConfigPlayer.TENACITY_MAX_HEALTH_BONUS.get()));
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "hahueuh:stat_reflexes_speed",
                StatBonuses.bonus(data, PlayerStat.REFLEXES, ConfigPlayer.REFLEXES_SPEED_BONUS.get()));

        double attackSpeed = isWeapon(player.getMainHandItem())
                ? StatBonuses.bonus(data, PlayerStat.COMBAT, ConfigPlayer.COMBAT_ATTACK_SPEED_BONUS.get())
                : StatBonuses.bonus(data, PlayerStat.REFLEXES, ConfigPlayer.REFLEXES_ATTACK_SPEED_BONUS.get());
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "hahueuh:stat_attack_speed", attackSpeed);

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(ServerPlayer player, Attribute attribute, UUID modifierId,
                                    String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(modifierId);
        if (Math.abs(amount) < 1.0E-6) return;
        instance.addTransientModifier(new AttributeModifier(
                modifierId, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof SpikedClubItem
                || stack.getItem() instanceof GuiltywhipItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    private static boolean isArmored(LivingEntity entity) {
        return entity.getArmorValue() > 0;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % REFRESH_INTERVAL_TICKS != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyAttributes(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!player.isSprinting() || player.isPassenger()) return;

        double moved = Math.sqrt(
                Math.pow(player.getX() - player.xo, 2) + Math.pow(player.getZ() - player.zo, 2));
        if (moved <= 1.0E-4) return;

        award(player, PlayerStat.REFLEXES, moved * ConfigPlayer.STAT_XP_PER_SPRINT_BLOCK.get());
    }

    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double bonus = StatBonuses.bonus(PlayerData.get(player), PlayerStat.STRENGTH,
                ConfigPlayer.STRENGTH_JUMP_BONUS.get());
        if (bonus <= 0.0) return;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, motion.y * (1.0 + bonus), motion.z);
        player.hasImpulse = true;
    }

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double reduction = StatBonuses.bonus(PlayerData.get(player), PlayerStat.STRENGTH,
                ConfigPlayer.STRENGTH_FALL_REDUCTION.get());
        if (reduction <= 0.0) return;
        event.setDamageMultiplier((float) (event.getDamageMultiplier() * (1.0 - reduction)));
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();

        if (victim instanceof ServerPlayer defender) {
            double reduction = StatBonuses.bonus(PlayerData.get(defender), PlayerStat.FORTITUDE,
                    ConfigPlayer.FORTITUDE_DAMAGE_REDUCTION.get());
            if (reduction > 0.0) event.setAmount((float) (event.getAmount() * (1.0 - reduction)));
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer striker && isDirectAttack(event.getSource())) {
            boolean weapon = isWeapon(striker.getMainHandItem());
            PlayerData data = PlayerData.get(striker);
            double bonus = weapon
                    ? StatBonuses.bonus(data, PlayerStat.COMBAT, ConfigPlayer.COMBAT_DAMAGE_BONUS.get())
                    : StatBonuses.bonus(data, PlayerStat.STRENGTH, ConfigPlayer.STRENGTH_FIST_DAMAGE_BONUS.get());
            if (bonus > 0.0) event.setAmount((float) (event.getAmount() * (1.0 + bonus)));

            award(striker, weapon ? PlayerStat.COMBAT : PlayerStat.STRENGTH,
                    event.getAmount() * ConfigPlayer.STAT_XP_PER_DAMAGE.get());
        }

        if (victim instanceof ServerPlayer defender && event.getAmount() > 0.0f) {
            award(defender, isArmored(defender) ? PlayerStat.FORTITUDE : PlayerStat.TENACITY,
                    event.getAmount() * ConfigPlayer.STAT_XP_PER_DAMAGE.get());
        }
    }

    private static boolean isDirectAttack(DamageSource source) {
        return source.getDirectEntity() == source.getEntity();
    }

    @SubscribeEvent
    public void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isCanceled()) return;
        award(player, PlayerStat.FORTITUDE,
                event.getBlockedDamage() * ConfigPlayer.STAT_XP_PER_SHIELD_BLOCK.get());
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double bonus = StatBonuses.bonus(PlayerData.get(player), PlayerStat.TENACITY,
                ConfigPlayer.TENACITY_REGEN_BONUS.get());
        if (bonus > 0.0) event.setAmount((float) (event.getAmount() * (1.0 + bonus)));
        award(player, PlayerStat.TENACITY, event.getAmount() * ConfigPlayer.STAT_XP_PER_HEAL.get());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!player.getMainHandItem().isEmpty()) return;
        award(player, PlayerStat.STRENGTH, ConfigPlayer.STAT_XP_PER_HAND_BREAK.get());
    }

    public void awardManaSpent(ServerPlayer player, int mana) {
        if (mana <= 0) return;
        award(player, PlayerStat.MAGIC, mana / 100.0 * ConfigPlayer.STAT_XP_PER_100_MANA_SPENT.get());
    }

    public void awardManaCharged(ServerPlayer player, int mana) {
        if (mana <= 0) return;
        award(player, PlayerStat.MAGIC, mana / 100.0 * ConfigPlayer.STAT_XP_PER_100_MANA_CHARGED.get());
    }

    private void award(ServerPlayer player, PlayerStat stat, double amount) {
        if (amount <= 0.0 || player.isCreative()) return;
        if (HahUeuh.LIONS_HEART.isActive(player.getUUID())) return;
        StatEntry entry = StatBonuses.entry(PlayerData.get(player), stat);
        if (entry.rolled() && StatBonuses.atCap(entry)) return;

        String key = player.getUUID() + "|" + stat.id;
        double carried = progressRemainder.merge(key, amount, Double::sum);
        int whole = (int) carried;
        if (whole <= 0) return;
        progressRemainder.put(key, carried - whole);
        PlayerStats.addProgress(player, stat, whole);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) applyAttributes(player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) applyAttributes(player);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        progressRemainder.keySet().removeIf(k -> k.startsWith(uuid.toString()));
    }
}
