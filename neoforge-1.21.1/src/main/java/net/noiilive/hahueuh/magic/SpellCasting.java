package net.noiilive.hahueuh.magic;

import net.noiilive.hahueuh.ConfigMagic;
import net.noiilive.hahueuh.ConfigMagicYin;
import net.noiilive.hahueuh.GateStrain;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModAttachments;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.GateStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpellCasting {
    private static final int TICKS_PER_SECOND = 20;
    private static final ResourceLocation CASTING_SLOW_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "casting_slow");

    private MinecraftServer server;
    private final Map<UUID, ActiveCast> activeCasts = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Long>> cooldownEnd = new HashMap<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final Map<UUID, Integer> pendingBanishTarget = new HashMap<>();
    private final Map<UUID, Integer> pendingUlMinyaTarget = new HashMap<>();
    private final Map<UUID, Integer> pendingCooldownOverrideSeconds = new HashMap<>();
    private final Map<UUID, Integer> pendingTotalManaOverride = new HashMap<>();

    public void tryStart(ServerPlayer player, Spell spell) {
        startCast(player, spell, true, false);
    }

    public void overrideNextTotalMana(ServerPlayer player, int totalMana) {
        pendingTotalManaOverride.put(player.getUUID(), Math.max(1, totalMana));
    }

    private void startCast(ServerPlayer player, Spell spell, boolean checkCooldown, boolean forStorage) {
        UUID id = player.getUUID();
        Integer manaOverride = pendingTotalManaOverride.remove(id);
        if (activeCasts.containsKey(id)) {
            actionBar(player, "hahueuh.message.spell_already_casting", ChatFormatting.GRAY);
            return;
        }
        if (HahUeuh.OL_SHAMAK.isSealed(player)) {
            actionBar(player, "hahueuh.message.ol_shamak_silenced", ChatFormatting.RED);
            return;
        }
        if (HahUeuh.LIONS_HEART.isActive(id)) {
            actionBar(player, "hahueuh.message.lions_heart_frozen_gate", ChatFormatting.RED);
            return;
        }
        if (!spell.id().equals(Spells.EMM) && HahUeuh.EMM.isActive(player)) {
            actionBar(player, "hahueuh.message.emm_locked", ChatFormatting.RED);
            return;
        }
        if (!spell.id().equals(Spells.EMT) && HahUeuh.EMT.suppresses(player)) {
            actionBar(player, "hahueuh.message.emt_silenced", ChatFormatting.RED);
            return;
        }
        if (checkCooldown && isOnCooldown(player, spell)) return;
        if (!spell.canCast(player)) {
            actionBar(player, "hahueuh.message.spell_cannot_cast", ChatFormatting.RED);
            return;
        }

        GateStrain.ensureRolled(player);
        if (player.getData(ModAttachments.PLAYER_GATE_STATUS.get()) == GateStatus.DESTROYED) {
            actionBar(player, "hahueuh.message.spell_gate_destroyed", ChatFormatting.RED);
            return;
        }
        if (!net.noiilive.hahueuh.MagicSchool.canReleaseMagic(player)) {
            actionBar(player, "hahueuh.message.spell_gate_no_release", ChatFormatting.RED);
            return;
        }

        int output = net.noiilive.hahueuh.StatBonuses.effectiveGateOutput(player);
        int efficiency = net.noiilive.hahueuh.StatBonuses.effectiveGateEfficiency(player);
        int manaPerTick = spell.manaPerTick();

        boolean creative = player.isCreative();
        double ratio = (double) manaPerTick / output;
        if (!creative && ratio >= ConfigMagic.CAST_FAIL_RATIO.get()) {
            failCast(player);
            return;
        }

        int totalMana = manaOverride != null ? manaOverride : spell.totalMana();
        int effectiveCost = effectiveManaCost(totalMana, efficiency);
        int currentMana = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        int currentOd = player.getData(ModAttachments.PLAYER_OD_CURRENT.get());
        boolean willFizzle = !creative && currentMana + currentOd < effectiveCost;

        int duration = Math.max(1, (int) Math.ceil((double) totalMana / output));
        boolean straining = output < manaPerTick;
        activeCasts.put(id, new ActiveCast(spell, duration, effectiveCost, straining, willFizzle, forStorage));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 0.7f);
        if (!forStorage) broadcastChant(player, spell);
        applyCastingSlow(player);

        net.noiilive.hahueuh.SpellHeat.addHeat(player, net.noiilive.hahueuh.SpellHeat.heatFor(manaPerTick));
    }

    private static void broadcastChant(ServerPlayer player, Spell spell) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        Component spellName = Component.translatable("hahueuh.ability." + spell.id().getPath());
        Component message = Component.literal("<").append(player.getDisplayName()).append("> ")
                .append(spellName).append("!");
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void drainReservoir(ServerPlayer player, int amount) {
        if (player.isCreative()) return;
        int mana = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        int fromMana = Math.min(amount, mana);
        if (fromMana > 0) {
            player.setData(ModAttachments.PLAYER_MANA_CURRENT.get(), mana - fromMana);
        }
        int shortfall = amount - fromMana;
        if (shortfall <= 0) return;

        int od = player.getData(ModAttachments.PLAYER_OD_CURRENT.get());
        int newOd = Math.max(0, od - shortfall);
        player.setData(ModAttachments.PLAYER_OD_CURRENT.get(), newOd);
        if (od > 0 && newOd == 0) {
            HahUeuh.CRIPPLED_STATE.afflict(player);
            HahUeuh.INSANITY.raiseToMax(player);
        }
    }

    private static int effectiveManaCost(int rawTotal, int efficiency) {
        double baseline = ConfigMagic.CAST_EFFICIENCY_BASELINE.get();
        return Math.max(1, (int) Math.round(rawTotal * baseline / efficiency));
    }

    private static void failCast(ServerPlayer player) {
        int amp = ConfigMagic.CAST_FAIL_FATIGUE_AMPLIFIER.getAsInt();
        int dur = ConfigMagic.CAST_FAIL_FATIGUE_SECONDS.getAsInt() * TICKS_PER_SECOND;
        if (dur > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, amp, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, dur, amp, false, true, true));
        }
        GateStrain.addStrain(player, ConfigMagic.CAST_FAIL_STRAIN.getAsInt());
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 0.8f);
        actionBar(player, "hahueuh.message.spell_failed", ChatFormatting.DARK_RED);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        this.server = server;
        if (!activeCasts.isEmpty()) tickCasts(server);
        if (!clouds.isEmpty()) tickClouds();
        if (server.getTickCount() % TICKS_PER_SECOND == 0) {
            sweepCreativeStrainAndHeat(server);
            net.noiilive.hahueuh.SpellHeat.tickDecay(server);
            GateStrain.tickDecay(server);
        }
    }

    private static void sweepCreativeStrainAndHeat(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isCreative()) continue;
            if (player.getData(ModAttachments.PLAYER_GATE_STRAIN.get()) > 0) GateStrain.setStrain(player, 0);
            if (player.getData(ModAttachments.PLAYER_SPELL_HEAT.get()) > 0) net.noiilive.hahueuh.SpellHeat.clear(player);
        }
    }

    @SubscribeEvent
    public void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        activeCasts.clear();
        cooldownEnd.clear();
        clouds.clear();
        pendingBanishTarget.clear();
        pendingUlMinyaTarget.clear();
        pendingCooldownOverrideSeconds.clear();
        pendingTotalManaOverride.clear();
        this.server = null;
    }

    @SubscribeEvent
    public void onPlayerWakeUp(net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isSleepingLongEnough()) return;
        if (player.getData(ModAttachments.PLAYER_SPELL_HEAT.get()) <= 0) return;

        net.noiilive.hahueuh.SpellHeat.clear(player);
        actionBar(player, "hahueuh.message.spell_heat_rested", ChatFormatting.AQUA);
    }

    @SubscribeEvent
    public void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getNewGameMode() != net.minecraft.world.level.GameType.CREATIVE) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GateStrain.setStrain(player, 0);
        net.noiilive.hahueuh.SpellHeat.clear(player);
    }

    private void tickCasts(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ActiveCast>> it = activeCasts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveCast> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            ActiveCast cast = entry.getValue();
            if (player == null || !player.isAlive()) {
                if (player != null) removeCastingSlow(player);
                it.remove();
                continue;
            }
            cast.elapsed++;

            int targetDrain = (int) Math.round((double) cast.effectiveCost * cast.elapsed / cast.totalTicks);
            int drainNow = targetDrain - cast.drainedSoFar;
            if (drainNow > 0) {
                drainReservoir(player, drainNow);
                cast.drainedSoFar = targetDrain;
                HahUeuh.STAT_EFFECTS.awardManaSpent(player, drainNow);
            }

            if (cast.straining && ++cast.strainTicks >= TICKS_PER_SECOND) {
                cast.strainTicks -= TICKS_PER_SECOND;
                GateStrain.addStrain(player, ConfigMagic.CAST_STRAIN_PER_SECOND.getAsInt());
            }

            if (cast.elapsed >= cast.totalTicks) {
                it.remove();
                removeCastingSlow(player);
                if (cast.willFizzle) {
                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 0.9f);
                    actionBar(player, "hahueuh.message.spell_fizzled", ChatFormatting.DARK_AQUA);
                } else if (cast.forStorage) {
                    finishStorage(player, cast.spell);
                } else {
                    cast.spell.onComplete(player);
                    Integer overrideSeconds = pendingCooldownOverrideSeconds.remove(player.getUUID());
                    if (overrideSeconds != null) {
                        if (!player.isCreative() && overrideSeconds > 0) {
                            setCooldown(player, cast.spell.cooldownId(), overrideSeconds);
                        }
                    } else {
                        startCooldown(player, cast.spell);
                    }
                }
            }
        }
    }


    private boolean isOnCooldown(ServerPlayer player, Spell spell) {
        if (player.isCreative()) return false;
        Map<ResourceLocation, Long> map = cooldownEnd.get(player.getUUID());
        if (map == null) return false;
        Long end = map.get(spell.cooldownId());
        if (end == null) return false;
        long now = worldTime(player);
        if (end - now > (long) spell.cooldownSeconds() * TICKS_PER_SECOND) {
            map.remove(spell.cooldownId());
            return false;
        }
        return now < end;
    }

    public void startCooldown(ServerPlayer player, Spell spell) {
        if (player.isCreative()) return;
        int seconds = spell.cooldownSeconds();
        if (seconds <= 0) return;
        setCooldown(player, spell.cooldownId(), seconds);
    }

    public void overrideNextCooldown(ServerPlayer player, int seconds) {
        pendingCooldownOverrideSeconds.put(player.getUUID(), seconds);
    }

    public void lockOutAllSpells(ServerPlayer player, int seconds) {
        if (seconds <= 0) return;
        for (Spell spell : SpellRegistry.all()) {
            setCooldown(player, spell.cooldownId(), seconds);
        }
    }


    public void beginBanish(ServerPlayer player, int targetEntityId) {
        if (isAlShamakOccupied(player)) {
            actionBar(player, "hahueuh.message.al_shamak_occupied", ChatFormatting.RED);
            return;
        }
        SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.AL_SHAMAK).ifPresent(spell -> {
            pendingBanishTarget.put(player.getUUID(), targetEntityId);
            tryStart(player, spell);
        });
    }

    public int consumeBanishTarget(UUID uuid) {
        Integer id = pendingBanishTarget.remove(uuid);
        return id == null ? -1 : id;
    }

    private boolean isAlShamakOccupied(ServerPlayer player) {
        return !player.getData(ModAttachments.PLAYER_STORED_SPELL.get()).isEmpty()
                || HahUeuh.POCKET_DIMENSION.hasIndefiniteFor(player.getUUID());
    }

    public void releaseStoredSpell(ServerPlayer player, int targetEntityId) {
        UUID uuid = player.getUUID();
        if (HahUeuh.POCKET_DIMENSION.hasIndefiniteFor(uuid)) {
            int count = HahUeuh.POCKET_DIMENSION.releaseAllIndefiniteFor(player);
            if (count <= 0) return;
            player.setData(ModAttachments.PLAYER_HAS_TRAPPED_ENTITIES.get(), false);
            player.level().playSound(null, player.blockPosition(),
                    net.noiilive.hahueuh.ModSounds.AL_SHAMAK_RELEASE.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            SpellRegistry.get(Spells.AL_SHAMAK).ifPresent(alShamak -> startCooldown(player, alShamak));
            return;
        }

        String storedId = player.getData(ModAttachments.PLAYER_STORED_SPELL.get());
        if (storedId.isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryParse(storedId);
        if (id == null) {
            player.setData(ModAttachments.PLAYER_STORED_SPELL.get(), "");
            return;
        }

        if (id.equals(Spells.UL_MINYA)) {
            LivingEntity target = resolveUlMinyaTarget(player, targetEntityId);
            if (target == null) {
                actionBar(player, "hahueuh.message.ul_minya_no_target", ChatFormatting.RED);
                return;
            }
            pendingUlMinyaTarget.put(uuid, targetEntityId);
        }

        player.setData(ModAttachments.PLAYER_STORED_SPELL.get(), "");
        player.level().playSound(null, player.blockPosition(),
                net.noiilive.hahueuh.ModSounds.AL_SHAMAK_RELEASE.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        SpellRegistry.get(id).ifPresent(spell -> {
            spell.onComplete(player);
            broadcastChant(player, spell);
        });
        SpellRegistry.get(Spells.AL_SHAMAK).ifPresent(alShamak -> startCooldown(player, alShamak));
    }

    public void discardAlShamak(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (HahUeuh.POCKET_DIMENSION.hasIndefiniteFor(uuid)) {
            int count = HahUeuh.POCKET_DIMENSION.discardAllIndefiniteFor(player.getServer(), uuid);
            if (count <= 0) return;
            player.setData(ModAttachments.PLAYER_HAS_TRAPPED_ENTITIES.get(), false);
            player.level().playSound(null, player.blockPosition(),
                    net.noiilive.hahueuh.ModSounds.AL_SHAMAK_RELEASE.get(), SoundSource.PLAYERS, 1.0f, 0.7f);
            player.displayClientMessage(Component.translatable("hahueuh.message.al_shamak_discarded", count)
                    .withStyle(ChatFormatting.GRAY), true);
            SpellRegistry.get(Spells.AL_SHAMAK).ifPresent(alShamak -> startCooldown(player, alShamak));
            return;
        }

        String storedId = player.getData(ModAttachments.PLAYER_STORED_SPELL.get());
        if (storedId.isEmpty()) return;
        player.setData(ModAttachments.PLAYER_STORED_SPELL.get(), "");
        ResourceLocation id = ResourceLocation.tryParse(storedId);
        Component spellName = id != null
                ? Component.translatable("hahueuh.ability." + id.getPath())
                : Component.literal(storedId);
        player.level().playSound(null, player.blockPosition(),
                net.noiilive.hahueuh.ModSounds.AL_SHAMAK_RELEASE.get(), SoundSource.PLAYERS, 1.0f, 0.7f);
        player.displayClientMessage(Component.translatable("hahueuh.message.al_shamak_spell_discarded", spellName)
                .withStyle(ChatFormatting.GRAY), true);
        SpellRegistry.get(Spells.AL_SHAMAK).ifPresent(alShamak -> startCooldown(player, alShamak));
    }

    public void beginStoreCast(ServerPlayer player, ResourceLocation spellId) {
        if (spellId.equals(Spells.AL_SHAMAK)) return;
        if (!player.getData(ModAttachments.PLAYER_STORED_SPELL.get()).isEmpty()) {
            actionBar(player, "hahueuh.message.spell_store_occupied", ChatFormatting.RED);
            return;
        }
        if (HahUeuh.POCKET_DIMENSION.hasIndefiniteFor(player.getUUID())) {
            actionBar(player, "hahueuh.message.al_shamak_occupied", ChatFormatting.RED);
            return;
        }
        Spell alShamak = SpellRegistry.get(Spells.AL_SHAMAK).orElse(null);
        if (alShamak != null && isOnCooldown(player, alShamak)) return;

        Spell spell = SpellRegistry.get(spellId).orElse(null);
        if (spell == null) return;

        startCast(player, spell, false, true);
    }

    private void finishStorage(ServerPlayer player, Spell spell) {
        player.setData(ModAttachments.PLAYER_STORED_SPELL.get(), spell.id().toString());
        Component spellName = Component.translatable("hahueuh.ability." + spell.id().getPath());
        player.displayClientMessage(Component.translatable("hahueuh.message.spell_stored", spellName)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                40, 0.4, player.getBbHeight() * 0.4, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.6f);

        if (!player.isCreative()) {
            SpellRegistry.get(Spells.AL_SHAMAK).ifPresent(alShamak ->
                    setCooldown(player, alShamak.cooldownId(), ConfigMagicYin.AL_SHAMAK_STORE_COOLDOWN_SECONDS.getAsInt()));
        }
    }


    public void beginUlMinya(ServerPlayer player, int targetEntityId) {
        LivingEntity target = resolveUlMinyaTarget(player, targetEntityId);
        if (target == null) {
            actionBar(player, "hahueuh.message.ul_minya_no_target", ChatFormatting.RED);
            return;
        }

        SpellRegistry.get(Spells.UL_MINYA).ifPresent(spell -> {
            pendingUlMinyaTarget.put(player.getUUID(), targetEntityId);
            tryStart(player, spell);
        });
    }

    private LivingEntity resolveUlMinyaTarget(ServerPlayer player, int targetEntityId) {
        Entity targetEntity = targetEntityId >= 0 ? player.level().getEntity(targetEntityId) : null;
        double range = ConfigMagicYin.UL_MINYA_RANGE.get();
        boolean valid = targetEntity instanceof LivingEntity target && target != player && target.isAlive()
                && !target.isSpectator() && target.distanceToSqr(player) <= range * range;
        return valid ? (LivingEntity) targetEntity : null;
    }

    public int consumeUlMinyaTarget(UUID uuid) {
        Integer id = pendingUlMinyaTarget.remove(uuid);
        return id == null ? -1 : id;
    }

    private static long worldTime(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server == null ? 0L : server.overworld().getGameTime();
    }

    private void setCooldown(ServerPlayer player, ResourceLocation cooldownId, int seconds) {
        int ticks = seconds * TICKS_PER_SECOND;
        cooldownEnd.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(cooldownId, worldTime(player) + ticks);
        PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(cooldownId, ticks));
    }

    public Map<UUID, Map<ResourceLocation, Integer>> captureCooldownRemaining() {
        Map<UUID, Map<ResourceLocation, Integer>> result = new HashMap<>();
        if (server == null) return result;
        long tick = server.overworld().getGameTime();
        cooldownEnd.forEach((uuid, spellMap) -> {
            Map<ResourceLocation, Integer> remaining = new HashMap<>();
            spellMap.forEach((id, until) -> {
                int left = (int) (until - tick);
                if (left > 0) remaining.put(id, left);
            });
            if (!remaining.isEmpty()) result.put(uuid, remaining);
        });
        return result;
    }

    public void restoreCooldownRemaining(Map<UUID, Map<ResourceLocation, Integer>> remainingByUuid) {
        cooldownEnd.clear();
        if (server == null) return;
        long tick = server.overworld().getGameTime();
        remainingByUuid.forEach((uuid, spellMap) -> {
            Map<ResourceLocation, Long> map = new HashMap<>();
            spellMap.forEach((id, remaining) -> map.put(id, tick + remaining));
            if (!map.isEmpty()) cooldownEnd.put(uuid, map);
        });
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Map<ResourceLocation, Integer> spellMap = remainingByUuid.get(player.getUUID());
            for (Spell spell : SpellRegistry.all()) {
                int remaining = spellMap == null ? 0 : spellMap.getOrDefault(spell.cooldownId(), 0);
                PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(spell.cooldownId(), remaining));
            }
        }
    }


    public void spawnExpandingCloud(ServerLevel level, Vec3 center, ParticleOptions particle,
                                    double radius, int expandTicks) {
        clouds.add(new Cloud(level, center, particle, radius, Math.max(1, expandTicks)));
    }

    private void tickClouds() {
        Iterator<Cloud> it = clouds.iterator();
        while (it.hasNext()) {
            Cloud cloud = it.next();
            cloud.age++;
            double frac = Math.min(1.0, (double) cloud.age / cloud.maxAge);
            double spread = cloud.radius * frac;
            int count = 60 + (int) (spread * 18);
            cloud.level.sendParticles(cloud.particle, cloud.center.x, cloud.center.y + 1.0, cloud.center.z,
                    count, spread, spread * 0.6, spread, 0.0);
            if (cloud.age >= cloud.maxAge) it.remove();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        activeCasts.remove(event.getEntity().getUUID());
        pendingBanishTarget.remove(event.getEntity().getUUID());
        pendingUlMinyaTarget.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) removeCastingSlow(player);
    }

    @SubscribeEvent
    public void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancelCastSilently(player);
    }

    @SubscribeEvent
    public void onDamaged(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0f) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        interruptCast(player);
    }

    public void cancelCastSilently(ServerPlayer player) {
        UUID id = player.getUUID();
        activeCasts.remove(id);
        pendingBanishTarget.remove(id);
        pendingUlMinyaTarget.remove(id);
        pendingCooldownOverrideSeconds.remove(id);
        pendingTotalManaOverride.remove(id);
        removeCastingSlow(player);
        HahUeuh.TELEPORTATION.clearPending(id);
    }

    private void interruptCast(ServerPlayer player) {
        if (activeCasts.remove(player.getUUID()) == null) return;
        removeCastingSlow(player);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 0.9f);
        actionBar(player, "hahueuh.message.spell_interrupted", ChatFormatting.RED);
    }

    private static void applyCastingSlow(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.addOrUpdateTransientModifier(
                new AttributeModifier(CASTING_SLOW_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeCastingSlow(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(CASTING_SLOW_ID);
    }

    private static void actionBar(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }

    private static final class ActiveCast {
        final Spell spell;
        final int totalTicks;
        final int effectiveCost;
        final boolean straining;
        final boolean willFizzle;
        final boolean forStorage;
        int elapsed;
        int drainedSoFar;
        int strainTicks;

        ActiveCast(Spell spell, int totalTicks, int effectiveCost, boolean straining, boolean willFizzle, boolean forStorage) {
            this.spell = spell;
            this.totalTicks = totalTicks;
            this.effectiveCost = effectiveCost;
            this.straining = straining;
            this.willFizzle = willFizzle;
            this.forStorage = forStorage;
        }
    }

    private static final class Cloud {
        final ServerLevel level;
        final Vec3 center;
        final ParticleOptions particle;
        final double radius;
        final int maxAge;
        int age;

        Cloud(ServerLevel level, Vec3 center, ParticleOptions particle, double radius, int maxAge) {
            this.level = level;
            this.center = center;
            this.particle = particle;
            this.radius = radius;
            this.maxAge = maxAge;
        }
    }
}
