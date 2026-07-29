package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Emm {
    private static final UUID STILLNESS_ID = UUID.fromString("5b8e2f41-3c7a-4d19-9e60-2a4c8b1d7e33");
    private static final int TICKS_PER_SECOND = 20;

    private final Map<UUID, Integer> upkeepTicks = new HashMap<>();

    public boolean isActive(LivingEntity entity) {
        return entity instanceof ServerPlayer player && PlayerData.get(player).isEmmActive();
    }

    public void tryCast(ServerPlayer caster) {
        if (PlayerData.get(caster).isEmmActive()) {
            deactivate(caster, true);
            return;
        }
        net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.EMM)
                .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(caster, spell));
    }

    public void cast(ServerPlayer caster) {
        if (PlayerData.get(caster).isEmmActive()) {
            deactivate(caster, true);
            return;
        }
        HahUeuh.SPELL_CASTING.overrideNextCooldown(caster, 0);
        activate(caster);
    }

    private void activate(ServerPlayer caster) {
        PlayerData.get(caster).setEmmActive(true);
        PlayerDataEvents.sync(caster);
        upkeepTicks.put(caster.getUUID(), 0);
        applyStillness(caster);

        ServerLevel level = caster.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD,
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.5, caster.getZ(),
                40, 0.35, caster.getBbHeight() * 0.45, 0.35, 0.01);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), ModSounds.EMM_ACTIVATE.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        actionBar(caster, "hahueuh.message.emm_started", ChatFormatting.DARK_PURPLE);
    }

    public void deactivate(ServerPlayer caster, boolean startCooldown) {
        if (!PlayerData.get(caster).isEmmActive()) return;
        PlayerData.get(caster).setEmmActive(false);
        PlayerDataEvents.sync(caster);
        upkeepTicks.remove(caster.getUUID());
        removeStillness(caster);

        if (startCooldown) {
            net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.EMM)
                    .ifPresent(spell -> HahUeuh.SPELL_CASTING.startCooldown(caster, spell));
        }

        ServerLevel level = caster.serverLevel();
        level.sendParticles(ParticleTypes.SMOKE,
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.5, caster.getZ(),
                24, 0.3, caster.getBbHeight() * 0.4, 0.3, 0.01);
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.8f, 1.2f);
        actionBar(caster, "hahueuh.message.emm_ended", ChatFormatting.GRAY);
    }

    private static void applyStillness(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(STILLNESS_ID);
        speed.addTransientModifier(new AttributeModifier(STILLNESS_ID, "hahueuh:emm_stillness",
                -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeStillness(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(STILLNESS_ID);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!PlayerData.get(player).isEmmActive()) continue;

            if (!chargeUpkeep(player)) {
                deactivate(player, true);
            }
        }
    }

    private boolean chargeUpkeep(ServerPlayer player) {
        int perSecond = ConfigMagicYin.EMM_UPKEEP_PER_SECOND.get();
        if (perSecond <= 0 || player.isCreative()) return true;

        UUID uuid = player.getUUID();
        int ticks = upkeepTicks.merge(uuid, 1, Integer::sum);
        if (ticks < TICKS_PER_SECOND) return true;
        upkeepTicks.put(uuid, 0);

        PlayerData data = PlayerData.get(player);
        int mana = data.getManaCurrent();
        if (mana < perSecond) {
            actionBar(player, "hahueuh.message.emm_exhausted", ChatFormatting.RED);
            return false;
        }
        data.setManaCurrent(mana - perSecond);
        PlayerDataEvents.sync(player);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onIncomingDamage(LivingAttackEvent event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!isActive(event.getEntity())) return;
        if (event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!isActive(event.getEntity())) return;
        LivingEntity entity = event.getEntity();
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, 0.0, motion.z);
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerData.get(player).isEmmActive()) {
            upkeepTicks.put(player.getUUID(), 0);
            applyStillness(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        upkeepTicks.remove(event.getEntity().getUUID());
    }

    private static void actionBar(ServerPlayer player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }
}
