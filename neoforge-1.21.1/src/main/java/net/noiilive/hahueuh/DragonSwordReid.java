package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DragonSwordReid {
    private static final double MAX_UNSHEATH_DISTANCE = 32.0;
    private static final int TICK_INTERVAL = 10;
    private final Map<UUID, UUID> unlockedAgainst = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onSweepAttack(SweepAttackEvent event) {
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof DragonSwordReidItem && DragonSwordReidItem.isSheathed(main)) {
            event.setSweeping(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        Entity attacker = event.getSource().getEntity();
        if (victim instanceof ServerPlayer holder && attacker != null && attacker != victim
                && !heldDragonSword(holder).isEmpty() && isWorthyOpponent(attacker)) {
            grantUnlock(holder, attacker.getUUID());
        }

        if (!isBlockingWithDragonSword(victim)) return;
        if (event.getSource().getEntity() == null) return;
        event.setCanceled(true);
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.SHIELD_BLOCK, victim.getSoundSource(), 1.0f,
                0.8f + victim.level().getRandom().nextFloat() * 0.4f);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (unlockedAgainst.isEmpty()) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % TICK_INTERVAL != 0) return;

        for (Map.Entry<UUID, UUID> entry : new ArrayList<>(unlockedAgainst.entrySet())) {
            ServerPlayer holder = server.getPlayerList().getPlayer(entry.getKey());
            if (holder == null) {
                unlockedAgainst.remove(entry.getKey());
                continue;
            }
            ItemStack sword = heldDragonSword(holder);
            Entity opponent = holder.serverLevel().getEntity(entry.getValue());
            boolean stillValid = !sword.isEmpty() && opponent != null && opponent.isAlive()
                    && opponent.distanceTo(holder) <= MAX_UNSHEATH_DISTANCE;
            if (!stillValid) {
                unlockedAgainst.remove(entry.getKey());
                if (!sword.isEmpty() && !DragonSwordReidItem.isSheathed(sword)) {
                    DragonSwordReidItem.setSheathed(sword, true);
                    holder.displayClientMessage(Component.translatable("hahueuh.message.dragon_sword_resheath")
                            .withStyle(ChatFormatting.GRAY), true);
                }
            }
        }
    }

    public boolean canUnsheath(Player player) {
        return unlockedAgainst.containsKey(player.getUUID());
    }
    public void clearUnlock(UUID playerUuid) {
        unlockedAgainst.remove(playerUuid);
    }

    private void grantUnlock(ServerPlayer holder, UUID opponentUuid) {
        UUID previous = unlockedAgainst.put(holder.getUUID(), opponentUuid);
        if (!opponentUuid.equals(previous)) {
            holder.displayClientMessage(Component.translatable("hahueuh.message.dragon_sword_can_unsheath")
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    public static boolean isWorthyOpponent(Entity entity) {
        if (entity instanceof Player player) {
            PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
            UUID id = player.getUUID();
            return am.canUseDomain(id) || am.canUseSloth(id) || am.canUseGreed(id) || am.hasAnyWitchFactor(id);
        }
        if (entity instanceof LivingEntity mob) {
            return mob.getData(ModAttachments.MOB_WITCH_FACTOR.get()) != WitchFactorAuthority.NONE;
        }
        return false;
    }

    private static ItemStack heldDragonSword(Player player) {
        if (player.getMainHandItem().getItem() instanceof DragonSwordReidItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof DragonSwordReidItem) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public static boolean isBlockingWithDragonSword(LivingEntity entity) {
        if (!entity.isUsingItem()) return false;
        return entity.getUseItem().getItem() instanceof DragonSwordReidItem;
    }
}
