package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobWitchFactor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final String MOB_HOLDERS_FILE = "hahueuh_mob_witch_factor_holders.json";
    private static final String WANDERING_FILE = "hahueuh_wandering_witch_factors.json";

    private final Map<UUID, WitchFactorAuthority> mobHolders = new ConcurrentHashMap<>();
    private final Map<UUID, WitchFactorAuthority> wandering = new ConcurrentHashMap<>();
    private Path mobHoldersPath;
    private Path wanderingPath;

    public boolean existsAnywhere(WitchFactorAuthority sin) {
        if (sin == WitchFactorAuthority.NONE) return false;
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        boolean playerHolds = switch (sin) {
            case SLOTH -> !am.holdersOfWitchFactorSloth().isEmpty();
            case GREED -> !am.holdersOfWitchFactorGreed().isEmpty();
            case NONE -> false;
        };
        if (playerHolds) return true;
        if (mobHolders.containsValue(sin)) return true;
        return wandering.containsValue(sin);
    }

    public void registerWandering(WitchFactorEntity entity) {
        WitchFactorAuthority sin = entity.getAssignedAuthority();
        if (sin == WitchFactorAuthority.NONE) return;
        wandering.put(entity.getUUID(), sin);
        saveWandering();
    }

    public void unregisterWandering(WitchFactorEntity entity) {
        if (wandering.remove(entity.getUUID()) != null) saveWandering();
    }

    public void registerMobHolder(UUID mobUuid, WitchFactorAuthority sin) {
        mobHolders.put(mobUuid, sin);
        saveMobHolders();
    }

    public void clearMobHolder(UUID mobUuid) {
        if (mobHolders.remove(mobUuid) != null) saveMobHolders();
    }

    public static boolean isEligibleMobType(Entity entity) {
        if (!(entity instanceof Mob)) return false;
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        for (String allowed : ConfigMain.MOB_WITCH_FACTOR_ELIGIBLE_ENTITIES.get()) {
            if (typeId.equals(ResourceLocation.tryParse(allowed))) return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof WitchFactorEntity wf) {
            unregisterWandering(wf);
            return;
        }
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel level)) return;

        WitchFactorAuthority sin = entity.getData(ModAttachments.MOB_WITCH_FACTOR.get());
        if (sin == WitchFactorAuthority.NONE) return;

        clearMobHolder(entity.getUUID());
        WitchFactorEntity spawned = new WitchFactorEntity(ModEntities.WITCH_FACTOR.get(), level);
        spawned.moveTo(entity.getX(), entity.getY(), entity.getZ(), 0.0f, 0.0f);
        spawned.setAssignedAuthority(sin);
        level.addFreshEntity(spawned);
        registerWandering(spawned);
    }

    @SubscribeEvent
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!ConfigMain.MOB_WITCH_FACTORS_ENABLED.get() || !ConfigMain.MOB_WITCH_FACTOR_NATURAL_SPAWN_ENABLED.get()) return;
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;

        Mob mob = event.getEntity();
        if (!isEligibleMobType(mob)) return;
        if (event.getY() < ConfigMain.MOB_WITCH_FACTOR_NATURAL_SPAWN_MIN_Y.get()) return;
        if (mob.getRandom().nextDouble() * 100.0 >= ConfigMain.MOB_WITCH_FACTOR_NATURAL_SPAWN_CHANCE.get()) return;

        List<WitchFactorAuthority> candidates = new ArrayList<>(List.of(WitchFactorAuthority.SLOTH, WitchFactorAuthority.GREED));
        if (ConfigMain.SINGLE_AUTHORITY_HOLDER.get()) {
            candidates.removeIf(this::existsAnywhere);
        }
        if (candidates.isEmpty()) return;

        WitchFactorAuthority sin = candidates.get(mob.getRandom().nextInt(candidates.size()));
        mob.setData(ModAttachments.MOB_WITCH_FACTOR.get(), sin);
        String variantId = switch (sin) {
            case SLOTH -> SlothVariant.randomForMob(mob.getRandom()).id;
            case GREED -> GreedVariant.randomForMob(mob.getRandom()).id;
            case NONE -> "";
        };
        mob.setData(ModAttachments.MOB_WITCH_FACTOR_VARIANT.get(), variantId);
        mob.setPersistenceRequired();
        registerMobHolder(mob.getUUID(), sin);
        switch (sin) {
            case SLOTH -> HahUeuh.SLOTH_COMPAT.ensureStartingScore(mob.getUUID());
            case GREED -> HahUeuh.GREED_COMPAT.ensureStartingScore(mob.getUUID());
            case NONE -> {}
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        Path root = server.getWorldPath(LevelResource.ROOT);
        mobHoldersPath = root.resolve(MOB_HOLDERS_FILE);
        wanderingPath = root.resolve(WANDERING_FILE);

        mobHolders.clear();
        wandering.clear();
        loadInto(mobHoldersPath, mobHolders);
        loadInto(wanderingPath, wandering);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        mobHolders.clear();
        wandering.clear();
        mobHoldersPath = null;
        wanderingPath = null;
    }

    private void loadInto(Path path, Map<UUID, WitchFactorAuthority> target) {
        if (path == null || !Files.exists(path)) return;
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, String> raw = GSON.fromJson(json, STRING_MAP_TYPE);
            if (raw != null) {
                raw.forEach((uuidStr, sinId) -> {
                    try {
                        target.put(UUID.fromString(uuidStr), WitchFactorAuthority.byId(sinId));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Ignoring malformed UUID '{}' in {}", uuidStr, path);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load Witch Factor mob data from {}", path, e);
        }
    }

    private void saveMobHolders() {
        save(mobHoldersPath, mobHolders);
    }

    private void saveWandering() {
        save(wanderingPath, wandering);
    }

    private void save(Path path, Map<UUID, WitchFactorAuthority> source) {
        if (path == null) return;
        try {
            Map<String, String> raw = new HashMap<>();
            source.forEach((uuid, sin) -> raw.put(uuid.toString(), sin.id));
            Files.writeString(path, GSON.toJson(raw, STRING_MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save Witch Factor mob data to {}", path, e);
        }
    }
}
