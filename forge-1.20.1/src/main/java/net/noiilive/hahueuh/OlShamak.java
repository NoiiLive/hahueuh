package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.noiilive.hahueuh.capability.ModCapabilities;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.noiilive.hahueuh.network.WitchFactorAuthority;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OlShamak {
    private final Map<UUID, UUID> sealedToSeal = new ConcurrentHashMap<>();

    public boolean isSealed(Entity entity) {
        return entity != null && sealedToSeal.containsKey(entity.getUUID());
    }

    public void cast(ServerPlayer caster) {
        LivingEntity target = lookedAtLiving(caster);
        if (target == null) {
            actionBar(caster, "hahueuh.message.ol_shamak_no_target", ChatFormatting.RED);
            return;
        }

        if (isSealed(target)) {
            releaseSealed(target, true);
            actionBar(caster, "hahueuh.message.ol_shamak_released", ChatFormatting.GRAY);
            return;
        }
        seal(caster, target);
    }

    private void seal(ServerPlayer caster, LivingEntity target) {
        ServerLevel level = caster.serverLevel();
        YinSealEntity seal = ModEntities.YIN_SEAL.get().create(level);
        if (seal == null) {
            actionBar(caster, "hahueuh.message.ol_shamak_failed", ChatFormatting.RED);
            return;
        }

        seal.bind(target, caster.getUUID());
        level.addFreshEntity(seal);
        sealedToSeal.put(target.getUUID(), seal.getUUID());
        applySealedState(target, true);

        level.playSound(null, target.blockPosition(), ModSounds.DOMAIN_OPEN.get(),
                SoundSource.PLAYERS, 1.0f, 0.5f);

        actionBar(caster, seal.unbreakable()
                ? "hahueuh.message.ol_shamak_sealed_unbreakable" : "hahueuh.message.ol_shamak_sealed",
                ChatFormatting.DARK_PURPLE);
    }

    public void ensureTracked(YinSealEntity seal) {
        UUID sealedUuid = seal.sealedUuid();
        if (sealedUuid == null) return;
        if (sealedToSeal.putIfAbsent(sealedUuid, seal.getUUID()) == null) {
            LivingEntity sealed = resolveSealed(seal);
            if (sealed != null) applySealedState(sealed, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onIncomingDamage(LivingAttackEvent event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEnderTeleport(net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onChorusFruitTeleport(net.minecraftforge.event.entity.EntityTeleportEvent.ChorusFruit event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isSealed(event.getEntity())) event.setCanceled(true);
    }

    public LivingEntity resolveSealed(YinSealEntity seal) {
        if (seal.sealedUuid() == null || !(seal.level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(seal.sealedUuid());
        return entity instanceof LivingEntity living ? living : null;
    }

    public void release(YinSealEntity seal, boolean burst) {
        LivingEntity sealed = resolveSealed(seal);
        if (seal.sealedUuid() != null) sealedToSeal.remove(seal.sealedUuid());
        if (sealed != null) applySealedState(sealed, false);
        if (burst) seal.burst();
        seal.discard();
    }

    private void releaseSealed(LivingEntity target, boolean burst) {
        UUID sealUuid = sealedToSeal.remove(target.getUUID());
        applySealedState(target, false);
        if (sealUuid == null || !(target.level() instanceof ServerLevel level)) return;
        if (level.getEntity(sealUuid) instanceof YinSealEntity seal) {
            if (burst) seal.burst();
            seal.discard();
        }
    }

    private static void applySealedState(LivingEntity target, boolean sealed) {
        if (target instanceof ServerPlayer player) {
            PlayerData.get(player).setSealed(sealed);
            PlayerDataEvents.sync(player);
        }
        target.setNoGravity(sealed);
        if (!sealed) target.fallDistance = 0f;
    }

    private static LivingEntity lookedAtLiving(ServerPlayer caster) {
        double range = ConfigMagicYin.OL_SHAMAK_RANGE.get();
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(caster,
                e -> e != caster && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity, range);
        return hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le ? le : null;
    }

    public static boolean hasWitchFactor(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().hasAnyWitchFactor(player.getUUID());
        }
        return entity.getCapability(ModCapabilities.MOB_WITCH_FACTOR)
                .map(data -> data.getAuthority() != WitchFactorAuthority.NONE)
                .orElse(false);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applySealedState(player, isSealed(player));
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applySealedState(player, isSealed(player));
        }
    }

    @SubscribeEvent
    public void onSealedDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        LivingEntity dying = event.getEntity();
        if (!isSealed(dying)) return;
        releaseSealed(dying, false);
    }

    public void reconcileAfterRollback(MinecraftServer server) {
        sealedToSeal.clear();
        for (ServerLevel level : server.getAllLevels()) {
            for (YinSealEntity seal : level.getEntities(ModEntities.YIN_SEAL.get(), e -> e.isAlive())) {
                UUID sealedUuid = seal.sealedUuid();
                if (sealedUuid != null) sealedToSeal.put(sealedUuid, seal.getUUID());
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) {
                    applySealedState(living, sealedToSeal.containsKey(living.getUUID()));
                }
            }
        }
    }

    public void forgetAll(MinecraftServer server) {
        for (UUID sealedUuid : sealedToSeal.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(sealedUuid);
            if (player != null) applySealedState(player, false);
        }
        sealedToSeal.clear();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        sealedToSeal.clear();
    }

    private static void actionBar(ServerPlayer player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }
}
