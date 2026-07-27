package net.noiilive.hahueuh.snapshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.ConfigMain;

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

public class PlayerAuthorityManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();
    private static final String FILE_NAME = "hahueuh_authority.json";
    private static final String DOMAIN_FILE_NAME = "hahueuh_domain_authority.json";
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final Map<UUID, Boolean> returnByDeath = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> domain = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> sloth = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> greed = new ConcurrentHashMap<>();
    private final Map<UUID, SlothVariant> slothVariant = new ConcurrentHashMap<>();
    private final Map<UUID, GreedVariant> greedVariant = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> witchFactorSloth = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> witchFactorGreed = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> sageCandidate = new ConcurrentHashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

    private Path filePath;
    private Path domainFilePath;
    private Path slothFilePath;
    private Path greedFilePath;
    private Path slothVariantFilePath;
    private Path greedVariantFilePath;
    private Path witchFactorSlothFilePath;
    private Path witchFactorGreedFilePath;
    private Path sageCandidateFilePath;

    public void load(MinecraftServer server) {
        returnByDeath.clear();
        domain.clear();
        sloth.clear();
        greed.clear();
        slothVariant.clear();
        greedVariant.clear();
        witchFactorSloth.clear();
        witchFactorGreed.clear();
        sageCandidate.clear();
        Path root = server.getWorldPath(LevelResource.ROOT);
        filePath = root.resolve(FILE_NAME);
        domainFilePath = root.resolve(DOMAIN_FILE_NAME);
        slothFilePath = root.resolve("hahueuh_sloth_authority.json");
        greedFilePath = root.resolve("hahueuh_greed_authority.json");
        slothVariantFilePath = root.resolve("hahueuh_sloth_variant.json");
        greedVariantFilePath = root.resolve("hahueuh_greed_variant.json");
        witchFactorSlothFilePath = root.resolve("hahueuh_witch_factor_sloth.json");
        witchFactorGreedFilePath = root.resolve("hahueuh_witch_factor_greed.json");
        sageCandidateFilePath = root.resolve("hahueuh_sage_candidate.json");
        loadInto(filePath, returnByDeath);
        loadInto(domainFilePath, domain);
        loadInto(slothFilePath, sloth);
        loadInto(greedFilePath, greed);
        loadVariants(slothVariantFilePath, slothVariant, SlothVariant::byId);
        loadVariants(greedVariantFilePath, greedVariant, GreedVariant::byId);
        loadInto(witchFactorSlothFilePath, witchFactorSloth);
        loadInto(witchFactorGreedFilePath, witchFactorGreed);
        loadInto(sageCandidateFilePath, sageCandidate);
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

    private <T extends Enum<T>> void loadVariants(Path path, Map<UUID, T> target,
                                                  java.util.function.Function<String, T> byId) {
        if (path == null || !Files.exists(path)) return;
        try {
            Map<String, String> raw = GSON.fromJson(
                    Files.readString(path, StandardCharsets.UTF_8), STRING_MAP_TYPE);
            if (raw == null) return;
            raw.forEach((uuidStr, value) -> {
                try {
                    target.put(UUID.fromString(uuidStr), byId.apply(value));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Ignoring malformed UUID '{}' in {}", uuidStr, path);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to load variant data from {}", path, e);
        }
    }

    private <T extends Enum<T>> void saveVariants(Path path, Map<UUID, T> source) {
        if (path == null) return;
        try {
            Map<String, String> raw = new HashMap<>();
            source.forEach((uuid, v) -> raw.put(uuid.toString(), variantId(v)));
            Files.writeString(path, GSON.toJson(raw, STRING_MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save variant data to {}", path, e);
        }
    }

    private static String variantId(Object variant) {
        if (variant instanceof SlothVariant s) return s.id;
        if (variant instanceof GreedVariant g) return g.id;
        return String.valueOf(variant);
    }

    private static List<UUID> currentHolders(Map<UUID, Boolean> authorityMap) {
        List<UUID> holders = new ArrayList<>();
        authorityMap.forEach((uuid, has) -> { if (Boolean.TRUE.equals(has)) holders.add(uuid); });
        return holders;
    }

    public List<UUID> holdersOfReturnByDeath() {
        return currentHolders(returnByDeath);
    }

    public boolean canReturnByDeath(UUID uuid) {
        return Boolean.TRUE.equals(returnByDeath.get(uuid));
    }

    public void setReturnByDeath(UUID uuid, boolean value) {
        returnByDeath.put(uuid, value);
        saveMap(filePath, returnByDeath);
    }

    public List<UUID> holdersOfSloth() {
        return currentHolders(sloth);
    }

    public boolean canUseSloth(UUID uuid) {
        return Boolean.TRUE.equals(sloth.get(uuid));
    }

    public void setSloth(UUID uuid, boolean value) {
        sloth.put(uuid, value);
        saveMap(slothFilePath, sloth);
    }

    public List<UUID> holdersOfGreed() {
        return currentHolders(greed);
    }

    public boolean canUseGreed(UUID uuid) {
        return Boolean.TRUE.equals(greed.get(uuid));
    }

    public void setGreed(UUID uuid, boolean value) {
        greed.put(uuid, value);
        saveMap(greedFilePath, greed);
    }

    public SlothVariant getSlothVariant(UUID uuid) {
        return slothVariant.getOrDefault(uuid, SlothVariant.UNSEEN_HANDS);
    }

    public void setSlothVariant(UUID uuid, SlothVariant variant) {
        slothVariant.put(uuid, variant);
        saveVariants(slothVariantFilePath, slothVariant);
    }

    public GreedVariant getGreedVariant(UUID uuid) {
        return greedVariant.getOrDefault(uuid, GreedVariant.LIONSHEART);
    }

    public void setGreedVariant(UUID uuid, GreedVariant variant) {
        greedVariant.put(uuid, variant);
        saveVariants(greedVariantFilePath, greedVariant);
    }

    public List<UUID> holdersOfWitchFactorSloth() {
        return currentHolders(witchFactorSloth);
    }

    public boolean hasWitchFactorSloth(UUID uuid) {
        return Boolean.TRUE.equals(witchFactorSloth.get(uuid));
    }

    public void setWitchFactorSloth(UUID uuid, boolean value) {
        witchFactorSloth.put(uuid, value);
        saveMap(witchFactorSlothFilePath, witchFactorSloth);
    }

    public List<UUID> holdersOfWitchFactorGreed() {
        return currentHolders(witchFactorGreed);
    }

    public boolean hasWitchFactorGreed(UUID uuid) {
        return Boolean.TRUE.equals(witchFactorGreed.get(uuid));
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
        return Boolean.TRUE.equals(sageCandidate.get(uuid));
    }

    public void setSageCandidate(UUID uuid, boolean value) {
        sageCandidate.put(uuid, value);
        saveMap(sageCandidateFilePath, sageCandidate);
    }

    public void ensureSageCandidateRolled(UUID uuid) {
        if (sageCandidate.containsKey(uuid)) return;
        boolean rolled = RANDOM.nextDouble() * 100.0 < ConfigMain.SAGE_CANDIDATE_CHANCE.get();
        sageCandidate.put(uuid, rolled);
        saveMap(sageCandidateFilePath, sageCandidate);
    }

    public List<UUID> holdersOfDomain() {
        return currentHolders(domain);
    }

    public boolean canUseDomain(UUID uuid) {
        return Boolean.TRUE.equals(domain.get(uuid));
    }

    public void setDomain(UUID uuid, boolean value) {
        domain.put(uuid, value);
        saveMap(domainFilePath, domain);
    }
}
