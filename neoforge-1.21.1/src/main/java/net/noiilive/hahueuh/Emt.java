package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.hahueuh.network.EmtStatePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Emt {
    private static final int TICKS_PER_SECOND = 20;
    private static final float DEEPEN_PITCH = 0.6f;

    private final Map<UUID, Field> fields = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> upkeepTicks = new HashMap<>();

    public boolean isActive(UUID uuid) {
        return fields.containsKey(uuid);
    }

    public boolean suppresses(Entity entity) {
        if (fields.isEmpty() || entity == null) return false;
        ResourceKey<Level> dim = entity.level().dimension();
        Vec3 pos = entity.position();
        for (Field field : fields.values()) {
            if (!field.dimension.equals(dim)) continue;
            if (pos.distanceToSqr(field.centre) <= field.radius * field.radius) return true;
        }
        return false;
    }

    public void tryCast(ServerPlayer caster) {
        if (fields.containsKey(caster.getUUID())) {
            deactivate(caster, true);
            return;
        }
        net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.EMT)
                .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(caster, spell));
    }

    public void cast(ServerPlayer caster) {
        if (fields.containsKey(caster.getUUID())) {
            deactivate(caster, true);
            return;
        }
        HahUeuh.SPELL_CASTING.overrideNextCooldown(caster, 0);
        activate(caster);
    }

    private void activate(ServerPlayer caster) {
        double radius = ConfigMagicYin.EMT_RADIUS.get();
        fields.put(caster.getUUID(), new Field(caster.level().dimension(), caster.position(), radius));
        upkeepTicks.put(caster.getUUID(), 0);

        ServerLevel level = caster.serverLevel();
        level.sendParticles(ParticleTypes.WITCH,
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.5, caster.getZ(),
                60, 0.6, caster.getBbHeight() * 0.6, 0.6, 0.05);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), ModSounds.DOMAIN_OPEN.get(),
                SoundSource.PLAYERS, 1.0f, DEEPEN_PITCH);
        actionBar(caster, "hahueuh.message.emt_started", ChatFormatting.DARK_PURPLE);
        syncAll(caster.getServer());
    }

    public void deactivate(ServerPlayer caster, boolean startCooldown) {
        if (fields.remove(caster.getUUID()) == null) return;
        upkeepTicks.remove(caster.getUUID());

        if (startCooldown) {
            net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.EMT)
                    .ifPresent(spell -> HahUeuh.SPELL_CASTING.startCooldown(caster, spell));
        }

        ServerLevel level = caster.serverLevel();
        level.sendParticles(ParticleTypes.SMOKE,
                caster.getX(), caster.getY() + caster.getBbHeight() * 0.5, caster.getZ(),
                30, 0.4, caster.getBbHeight() * 0.5, 0.4, 0.02);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), ModSounds.DOMAIN_CLOSE.get(),
                SoundSource.PLAYERS, 1.0f, DEEPEN_PITCH);
        actionBar(caster, "hahueuh.message.emt_ended", ChatFormatting.GRAY);
        syncAll(caster.getServer());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (fields.isEmpty()) return;
        MinecraftServer server = event.getServer();

        for (UUID uuid : fields.keySet().toArray(UUID[]::new)) {
            ServerPlayer owner = server.getPlayerList().getPlayer(uuid);
            if (owner == null) {
                fields.remove(uuid);
                upkeepTicks.remove(uuid);
                syncAll(server);
                continue;
            }
            if (!chargeUpkeep(owner)) {
                deactivate(owner, true);
            }
        }

        if (server.getTickCount() % TICKS_PER_SECOND == 0) syncAll(server);
    }

    private boolean chargeUpkeep(ServerPlayer player) {
        int perSecond = ConfigMagicYin.EMT_UPKEEP_PER_SECOND.get();
        if (perSecond <= 0 || player.isCreative()) return true;

        UUID uuid = player.getUUID();
        int ticks = upkeepTicks.merge(uuid, 1, Integer::sum);
        if (ticks < TICKS_PER_SECOND) return true;
        upkeepTicks.put(uuid, 0);

        int mana = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        if (mana < perSecond) {
            actionBar(player, "hahueuh.message.emt_exhausted", ChatFormatting.RED);
            return false;
        }
        player.setData(ModAttachments.PLAYER_MANA_CURRENT.get(), mana - perSecond);
        return true;
    }

    private void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, nearestFieldFor(player));
        }
    }

    private EmtStatePayload nearestFieldFor(ServerPlayer player) {
        ResourceKey<Level> dim = player.level().dimension();
        Vec3 pos = player.position();
        Field best = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (Field field : fields.values()) {
            if (!field.dimension.equals(dim)) continue;
            double distSqr = pos.distanceToSqr(field.centre);
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                best = field;
            }
        }
        if (best == null) return EmtStatePayload.INACTIVE;
        return new EmtStatePayload(true, best.centre.x, best.centre.y, best.centre.z,
                best.radius, best.dimension.location());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, nearestFieldFor(player));
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        upkeepTicks.remove(uuid);
        if (fields.remove(uuid) != null && event.getEntity().getServer() != null) {
            syncAll(event.getEntity().getServer());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        fields.clear();
        upkeepTicks.clear();
    }

    private static void actionBar(ServerPlayer player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }

    private record Field(ResourceKey<Level> dimension, Vec3 centre, double radius) {}
}
