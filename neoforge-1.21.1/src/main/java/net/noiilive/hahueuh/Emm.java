package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Emm {
    private static final ResourceLocation STILLNESS_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "emm_stillness");
    private static final int TICKS_PER_SECOND = 20;

    private final Map<UUID, Integer> upkeepTicks = new HashMap<>();

    public boolean isActive(LivingEntity entity) {
        return entity instanceof ServerPlayer player
                && player.getData(ModAttachments.PLAYER_EMM_ACTIVE.get());
    }

    public void tryCast(ServerPlayer caster) {
        if (caster.getData(ModAttachments.PLAYER_EMM_ACTIVE.get())) {
            deactivate(caster, true);
            return;
        }
        net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.EMM)
                .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(caster, spell));
    }

    public void cast(ServerPlayer caster) {
        if (caster.getData(ModAttachments.PLAYER_EMM_ACTIVE.get())) {
            deactivate(caster, true);
            return;
        }
        HahUeuh.SPELL_CASTING.overrideNextCooldown(caster, 0);
        activate(caster);
    }

    private void activate(ServerPlayer caster) {
        caster.setData(ModAttachments.PLAYER_EMM_ACTIVE.get(), true);
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
        if (!caster.getData(ModAttachments.PLAYER_EMM_ACTIVE.get())) return;
        caster.setData(ModAttachments.PLAYER_EMM_ACTIVE.get(), false);
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
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                    STILLNESS_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.addOrUpdateTransientModifier(new AttributeModifier(
                    STILLNESS_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeStillness(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(STILLNESS_ID);
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) jump.removeModifier(STILLNESS_ID);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getData(ModAttachments.PLAYER_EMM_ACTIVE.get())) continue;

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

        int mana = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        if (mana < perSecond) {
            actionBar(player, "hahueuh.message.emm_exhausted", ChatFormatting.RED);
            return false;
        }
        player.setData(ModAttachments.PLAYER_MANA_CURRENT.get(), mana - perSecond);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (net.noiilive.hahueuh.snapshot.PlayerSnapshot.isRestoringEffects()) return;
        if (!isActive(event.getEntity())) return;
        if (event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
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
    public void onAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isActive(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getData(ModAttachments.PLAYER_EMM_ACTIVE.get())) {
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
