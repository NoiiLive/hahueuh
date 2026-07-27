package net.noiilive.hahueuh.snapshot;

import net.noiilive.hahueuh.ConfigMain;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerAuthorityManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final String FILE_NAME = "hahueuh_authority.json";
    private static final String DOMAIN_FILE_NAME = "hahueuh_domain_authority.json";
    private static final String SLOTH_FILE_NAME = "hahueuh_sloth_authority.json";
    private static final String SLOTH_VARIANT_FILE_NAME = "hahueuh_sloth_variant.json";
    private static final String GREED_FILE_NAME = "hahueuh_greed_authority.json";
    private static final String GREED_VARIANT_FILE_NAME = "hahueuh_greed_variant.json";
    private static final String WITCH_FACTOR_SLOTH_FILE_NAME = "hahueuh_witch_factor_sloth.json";
    private static final String WITCH_FACTOR_GREED_FILE_NAME = "hahueuh_witch_factor_greed.json";
    private static final String SAGE_CANDIDATE_FILE_NAME = "hahueuh_sage_candidate.json";

    private static final Random RANDOM = new Random();

    private final Map<UUID, Boolean> returnByDeath = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> domain = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> sloth = new ConcurrentHashMap<>();
    private final Map<UUID, SlothVariant> slothVariant = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> greed = new ConcurrentHashMap<>();
    private final Map<UUID, GreedVariant> greedVariant = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> witchFactorSloth = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> witchFactorGreed = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> sageCandidate = new ConcurrentHashMap<>();
    private Path filePath;
    private Path domainFilePath;
    private Path slothFilePath;
    private Path slothVariantFilePath;
    private Path greedFilePath;
    private Path greedVariantFilePath;
    private Path witchFactorSlothFilePath;
    private Path witchFactorGreedFilePath;
    private Path sageCandidateFilePath;

    public void load(MinecraftServer server) {
        returnByDeath.clear();
        domain.clear();
        sloth.clear();
        slothVariant.clear();
        greed.clear();
        greedVariant.clear();
        witchFactorSloth.clear();
        witchFactorGreed.clear();
        sageCandidate.clear();
        Path root = server.getWorldPath(LevelResource.ROOT);
        filePath = root.resolve(FILE_NAME);
        domainFilePath = root.resolve(DOMAIN_FILE_NAME);
        slothFilePath = root.resolve(SLOTH_FILE_NAME);
        slothVariantFilePath = root.resolve(SLOTH_VARIANT_FILE_NAME);
        greedFilePath = root.resolve(GREED_FILE_NAME);
        greedVariantFilePath = root.resolve(GREED_VARIANT_FILE_NAME);
        witchFactorSlothFilePath = root.resolve(WITCH_FACTOR_SLOTH_FILE_NAME);
        witchFactorGreedFilePath = root.resolve(WITCH_FACTOR_GREED_FILE_NAME);
        sageCandidateFilePath = root.resolve(SAGE_CANDIDATE_FILE_NAME);
        loadInto(filePath, returnByDeath);
        loadInto(domainFilePath, domain);
        loadInto(slothFilePath, sloth);
        loadInto(greedFilePath, greed);
        loadInto(witchFactorSlothFilePath, witchFactorSloth);
        loadInto(witchFactorGreedFilePath, witchFactorGreed);
        loadInto(sageCandidateFilePath, sageCandidate);
        loadVariants(slothVariantFilePath, slothVariant, SlothVariant::byId, v -> v.id);
        loadVariants(greedVariantFilePath, greedVariant, GreedVariant::byId, v -> v.id);
    }

    private <T> void loadVariants(Path path, Map<UUID, T> target, java.util.function.Function<String, T> byId,
                                   java.util.function.Function<T, String> toId) {
        if (path == null || !Files.exists(path)) return;
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, String> raw = GSON.fromJson(json, STRING_MAP_TYPE);
            if (raw != null) {
                raw.forEach((uuidStr, value) -> {
                    try {
                        target.put(UUID.fromString(uuidStr), byId.apply(value));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Ignoring malformed UUID '{}' in {}", uuidStr, path);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load variant data from {}", path, e);
        }
    }

    private <T> void saveVariants(Path path, Map<UUID, T> source, java.util.function.Function<T, String> toId) {
        if (path == null) return;
        try {
            Map<String, String> raw = new HashMap<>();
            source.forEach((uuid, v) -> raw.put(uuid.toString(), toId.apply(v)));
            Files.writeString(path, GSON.toJson(raw, STRING_MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save variant data to {}", path, e);
        }
    }

    private void loadInto(Path path, Map<UUID, Boolean> target) {
        if (path == null || !Files.exists(path)) return;
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, Boolean> raw = GSON.fromJson(json, MAP_TYPE);
            if (raw != null) {
                raw.forEach((uuidStr, value) -> {
                    try {
                        target.put(UUID.fromString(uuidStr), value);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Ignoring malformed UUID '{}' in {}", uuidStr, path);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load player authority data from {}", path, e);
        }
    }

    private void saveMap(Path path, Map<UUID, Boolean> source) {
        if (path == null) return;
        try {
            Map<String, Boolean> raw = new HashMap<>();
            source.forEach((uuid, value) -> raw.put(uuid.toString(), value));
            Files.writeString(path, GSON.toJson(raw, MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save player authority data to {}", path, e);
        }
    }

    private static java.util.List<UUID> currentHolders(Map<UUID, Boolean> authorityMap) {
        java.util.List<UUID> holders = new java.util.ArrayList<>();
        authorityMap.forEach((uuid, has) -> { if (Boolean.TRUE.equals(has)) holders.add(uuid); });
        return holders;
    }

    public java.util.List<UUID> holdersOfReturnByDeath() {
        return currentHolders(returnByDeath);
    }

    public java.util.List<UUID> holdersOfDomain() {
        return currentHolders(domain);
    }

    public java.util.List<UUID> holdersOfSloth() {
        return currentHolders(sloth);
    }

    public java.util.List<UUID> holdersOfGreed() {
        return currentHolders(greed);
    }

    public java.util.List<UUID> holdersOfWitchFactorSloth() {
        return currentHolders(witchFactorSloth);
    }

    public java.util.List<UUID> holdersOfWitchFactorGreed() {
        return currentHolders(witchFactorGreed);
    }

    public boolean canReturnByDeath(UUID uuid) {
        return returnByDeath.getOrDefault(uuid, false);
    }

    public void setReturnByDeath(UUID uuid, boolean value) {
        returnByDeath.put(uuid, value);
        saveMap(filePath, returnByDeath);
    }

    public boolean canUseDomain(UUID uuid) {
        return domain.getOrDefault(uuid, false);
    }

    public void setDomain(UUID uuid, boolean value) {
        domain.put(uuid, value);
        saveMap(domainFilePath, domain);
    }

    public boolean canUseSloth(UUID uuid) {
        return sloth.getOrDefault(uuid, false);
    }

    public void setSloth(UUID uuid, boolean value) {
        sloth.put(uuid, value);
        saveMap(slothFilePath, sloth);
    }

    public SlothVariant getSlothVariant(UUID uuid) {
        return slothVariant.getOrDefault(uuid, SlothVariant.INVISIBLE_PROVIDENCE);
    }

    public void setSlothVariant(UUID uuid, SlothVariant variant) {
        slothVariant.put(uuid, variant);
        saveVariants(slothVariantFilePath, slothVariant, v -> v.id);
    }

    public boolean canUseGreed(UUID uuid) {
        return greed.getOrDefault(uuid, false);
    }

    public void setGreed(UUID uuid, boolean value) {
        greed.put(uuid, value);
        saveMap(greedFilePath, greed);
    }

    public GreedVariant getGreedVariant(UUID uuid) {
        return greedVariant.getOrDefault(uuid, GreedVariant.LIONSHEART);
    }

    public void setGreedVariant(UUID uuid, GreedVariant variant) {
        greedVariant.put(uuid, variant);
        saveVariants(greedVariantFilePath, greedVariant, v -> v.id);
    }

    public boolean hasWitchFactorSloth(UUID uuid) {
        return witchFactorSloth.getOrDefault(uuid, false);
    }

    public void setWitchFactorSloth(UUID uuid, boolean value) {
        witchFactorSloth.put(uuid, value);
        saveMap(witchFactorSlothFilePath, witchFactorSloth);
    }

    public boolean hasWitchFactorGreed(UUID uuid) {
        return witchFactorGreed.getOrDefault(uuid, false);
    }

    public void setWitchFactorGreed(UUID uuid, boolean value) {
        witchFactorGreed.put(uuid, value);
        saveMap(witchFactorGreedFilePath, witchFactorGreed);
    }

    public boolean hasAnyWitchFactor(UUID uuid) {
        return hasWitchFactorSloth(uuid) || hasWitchFactorGreed(uuid);
    }

    public boolean hasOtherWitchFactor(UUID uuid, WitchFactorAuthority sin) {
        return switch (sin) {
            case SLOTH -> hasWitchFactorGreed(uuid);
            case GREED -> hasWitchFactorSloth(uuid);
            case NONE -> hasAnyWitchFactor(uuid);
        };
    }

    public boolean isSageCandidate(UUID uuid) {
        return sageCandidate.getOrDefault(uuid, false);
    }
    public void setSageCandidate(UUID uuid, boolean value) {
        sageCandidate.put(uuid, value);
        saveMap(sageCandidateFilePath, sageCandidate);
    }

    public void ensureSageCandidateRolled(UUID uuid) {
        if (sageCandidate.containsKey(uuid)) return;
        boolean rolled = RANDOM.nextDouble() * 100.0 < ConfigMain.SAGE_CANDIDATE_CHANCE.get();
        setSageCandidate(uuid, rolled);
    }
}
