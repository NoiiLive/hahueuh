package net.noiilive.hahueuh;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.noiilive.hahueuh.network.PlayerStat;
import net.noiilive.hahueuh.network.StatEntry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatEffects {
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private static final ResourceLocation HEALTH_ID = id("stat_tenacity_health");
    private static final ResourceLocation SPEED_ID = id("stat_reflexes_speed");
    private static final ResourceLocation ATTACK_SPEED_ID = id("stat_attack_speed");
    private static final ResourceLocation JUMP_ID = id("stat_strength_jump");
    private static final ResourceLocation FALL_ID = id("stat_strength_fall");

    private final Map<String, Double> progressRemainder = new ConcurrentHashMap<>();

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, path);
    }

    public static void refresh(ServerPlayer player) {
        applyAttributes(player);
    }

    private static void applyAttributes(ServerPlayer player) {
        setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID,
                StatBonuses.bonus(player, PlayerStat.TENACITY, ConfigPlayer.TENACITY_MAX_HEALTH_BONUS.get()));
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID,
                StatBonuses.bonus(player, PlayerStat.REFLEXES, ConfigPlayer.REFLEXES_SPEED_BONUS.get()));
        setModifier(player, Attributes.JUMP_STRENGTH, JUMP_ID,
                StatBonuses.bonus(player, PlayerStat.STRENGTH, ConfigPlayer.STRENGTH_JUMP_BONUS.get()));
        setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_ID,
                -StatBonuses.bonus(player, PlayerStat.STRENGTH, ConfigPlayer.STRENGTH_FALL_REDUCTION.get()));

        double attackSpeed = isWeapon(player.getMainHandItem())
                ? StatBonuses.bonus(player, PlayerStat.COMBAT, ConfigPlayer.COMBAT_ATTACK_SPEED_BONUS.get())
                : StatBonuses.bonus(player, PlayerStat.REFLEXES, ConfigPlayer.REFLEXES_ATTACK_SPEED_BONUS.get());
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, attackSpeed);

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(ServerPlayer player,
                                    net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                    ResourceLocation modifierId, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (Math.abs(amount) < 1.0E-6) {
            instance.removeModifier(modifierId);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                modifierId, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof net.minecraft.world.item.MaceItem
                || stack.getItem() instanceof net.minecraft.world.item.BowItem
                || stack.getItem() instanceof net.minecraft.world.item.CrossbowItem;
    }

    private static boolean isArmored(LivingEntity entity) {
        return entity.getArmorValue() > 0;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % REFRESH_INTERVAL_TICKS != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyAttributes(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isSprinting() || player.isPassenger()) return;

        double moved = Math.sqrt(
                Math.pow(player.getX() - player.xo, 2) + Math.pow(player.getZ() - player.zo, 2));
        if (moved <= 1.0E-4) return;

        double xp = moved * ConfigPlayer.STAT_XP_PER_SPRINT_BLOCK.get();
        award(player, PlayerStat.REFLEXES, xp);
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        if (victim instanceof ServerPlayer defender) {
            double reduction = StatBonuses.bonus(defender, PlayerStat.FORTITUDE,
                    ConfigPlayer.FORTITUDE_DAMAGE_REDUCTION.get());
            if (reduction > 0.0) event.setAmount((float) (event.getAmount() * (1.0 - reduction)));
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer striker && isDirectAttack(event.getSource())) {
            boolean weapon = isWeapon(striker.getMainHandItem());
            double bonus = weapon
                    ? StatBonuses.bonus(striker, PlayerStat.COMBAT, ConfigPlayer.COMBAT_DAMAGE_BONUS.get())
                    : StatBonuses.bonus(striker, PlayerStat.STRENGTH, ConfigPlayer.STRENGTH_FIST_DAMAGE_BONUS.get());
            if (bonus > 0.0) event.setAmount((float) (event.getAmount() * (1.0 + bonus)));

            double xp = event.getAmount() * ConfigPlayer.STAT_XP_PER_DAMAGE.get();
            award(striker, weapon ? PlayerStat.COMBAT : PlayerStat.STRENGTH, xp);
        }

        if (victim instanceof ServerPlayer defender && event.getAmount() > 0.0f) {
            double xp = event.getAmount() * ConfigPlayer.STAT_XP_PER_DAMAGE.get();
            award(defender, isArmored(defender) ? PlayerStat.FORTITUDE : PlayerStat.TENACITY, xp);
        }
    }

    private static boolean isDirectAttack(DamageSource source) {
        return source.getDirectEntity() == source.getEntity();
    }

    @SubscribeEvent
    public void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getBlocked()) return;
        award(player, PlayerStat.FORTITUDE,
                event.getBlockedDamage() * ConfigPlayer.STAT_XP_PER_SHIELD_BLOCK.get());
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double bonus = StatBonuses.bonus(player, PlayerStat.TENACITY, ConfigPlayer.TENACITY_REGEN_BONUS.get());
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
        StatEntry entry = StatBonuses.entry(player, stat);
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
