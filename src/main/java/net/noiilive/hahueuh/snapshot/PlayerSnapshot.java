package net.noiilive.hahueuh.snapshot;

import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerSnapshot {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final double x, y, z;
    private final float yRot, xRot;
    private final ResourceKey<Level> dimension;
    private final float health;
    private final int foodLevel;
    private final float saturation;
    private final float exhaustion;
    private final ListTag inventoryTag;
    private final ListTag enderChestTag;
    private final int experienceLevel;
    private final float experienceProgress;
    private final int totalExperience;
    private final List<CompoundTag> activeEffects;
    private final GameType gameType;
    private final int selectedSlot;
    private final CompoundTag abilitiesTag;

    private PlayerSnapshot(double x, double y, double z, float yRot, float xRot,
                           ResourceKey<Level> dimension, float health, int foodLevel,
                           float saturation, float exhaustion, ListTag inventoryTag,
                           ListTag enderChestTag, int experienceLevel, float experienceProgress,
                           int totalExperience, List<CompoundTag> activeEffects,
                           GameType gameType, int selectedSlot, CompoundTag abilitiesTag) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
        this.dimension = dimension;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.inventoryTag = inventoryTag;
        this.enderChestTag = enderChestTag;
        this.experienceLevel = experienceLevel;
        this.experienceProgress = experienceProgress;
        this.totalExperience = totalExperience;
        this.activeEffects = activeEffects;
        this.gameType = gameType;
        this.selectedSlot = selectedSlot;
        this.abilitiesTag = abilitiesTag;
    }

    public static PlayerSnapshot capture(ServerPlayer player) {
        ListTag inventoryTag = player.getInventory().save(new ListTag());

        ListTag enderChestTag = player.getEnderChestInventory().createTag(player.registryAccess());

        List<CompoundTag> activeEffects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            Tag tag = effect.save();
            if (tag instanceof CompoundTag ct) {
                activeEffects.add(ct);
            }
        }

        CompoundTag abilitiesTag = new CompoundTag();
        player.getAbilities().addSaveData(abilitiesTag);

        return new PlayerSnapshot(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.level().dimension(),
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.getFoodData().getExhaustionLevel(),
                inventoryTag,
                enderChestTag,
                player.experienceLevel,
                player.experienceProgress,
                player.totalExperience,
                activeEffects,
                player.gameMode.getGameModeForPlayer(),
                player.getInventory().selected,
                abilitiesTag
        );
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("YRot", yRot);
        tag.putFloat("XRot", xRot);
        tag.putString("Dimension", dimension.location().toString());
        tag.putFloat("Health", health);
        tag.putInt("FoodLevel", foodLevel);
        tag.putFloat("Saturation", saturation);
        tag.putFloat("Exhaustion", exhaustion);
        tag.put("Inventory", inventoryTag);
        tag.put("EnderChest", enderChestTag);
        tag.putInt("XpLevel", experienceLevel);
        tag.putFloat("XpProgress", experienceProgress);
        tag.putInt("XpTotal", totalExperience);
        ListTag effectsList = new ListTag();
        effectsList.addAll(activeEffects);
        tag.put("ActiveEffects", effectsList);
        tag.putString("GameType", gameType.getName());
        tag.putInt("SelectedSlot", selectedSlot);
        tag.put("Abilities", abilitiesTag);
        return tag;
    }

    public static PlayerSnapshot fromNbt(CompoundTag tag) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension")));

        List<CompoundTag> activeEffects = new ArrayList<>();
        ListTag effectsList = tag.getList("ActiveEffects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effectsList.size(); i++) {
            activeEffects.add(effectsList.getCompound(i));
        }

        return new PlayerSnapshot(
                tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"),
                tag.getFloat("YRot"), tag.getFloat("XRot"),
                dimension,
                tag.getFloat("Health"),
                tag.getInt("FoodLevel"),
                tag.getFloat("Saturation"),
                tag.getFloat("Exhaustion"),
                tag.getList("Inventory", Tag.TAG_COMPOUND),
                tag.getList("EnderChest", Tag.TAG_COMPOUND),
                tag.getInt("XpLevel"),
                tag.getFloat("XpProgress"),
                tag.getInt("XpTotal"),
                activeEffects,
                GameType.byName(tag.getString("GameType")),
                tag.getInt("SelectedSlot"),
                tag.getCompound("Abilities")
        );
    }

    public void restore(ServerPlayer player, MinecraftServer server) {
        ServerLevel targetLevel = server.getLevel(dimension);
        if (targetLevel == null) {
            targetLevel = player.serverLevel();
        }
        player.teleportTo(targetLevel, x, y, z, Set.of(), yRot, xRot);

        player.setHealth(health);

        player.getFoodData().setFoodLevel(foodLevel);
        player.getFoodData().setSaturation(saturation);
        player.getFoodData().setExhaustion(exhaustion);

        player.getInventory().clearContent();
        player.getInventory().load(inventoryTag);
        player.getInventory().selected = selectedSlot;
        player.connection.send(new ClientboundSetCarriedItemPacket(selectedSlot));
        forceEquipmentAttributeRefresh(player);

        player.getEnderChestInventory().clearContent();
        player.getEnderChestInventory().fromTag(enderChestTag, player.registryAccess());

        player.experienceLevel = experienceLevel;
        player.experienceProgress = experienceProgress;
        player.totalExperience = totalExperience;

        player.removeAllEffects();
        for (CompoundTag effectTag : activeEffects) {
            MobEffectInstance effect = MobEffectInstance.load(effectTag);
            if (effect != null) {
                player.addEffect(effect);
            }
        }

        player.setGameMode(gameType);

        player.getAbilities().loadSaveData(abilitiesTag);

        resyncAdvancements(player, server);

        player.clearFire();
        player.setRemainingFireTicks(0);
        player.fallDistance = 0;
        player.setAirSupply(player.getMaxAirSupply());

        player.inventoryMenu.broadcastChanges();
        player.connection.send(new ClientboundSetExperiencePacket(
                experienceProgress, totalExperience, experienceLevel
        ));
        player.onUpdateAbilities();
    }

    @SuppressWarnings("unchecked")
    private static void forceEquipmentAttributeRefresh(ServerPlayer player) {
        try {
            for (String fieldName : new String[]{"lastHandItemStacks", "lastArmorItemStacks"}) {
                Field field = LivingEntity.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                NonNullList<ItemStack> tracked = (NonNullList<ItemStack>) field.get(player);
                for (int i = 0; i < tracked.size(); i++) {
                    tracked.set(i, ItemStack.EMPTY);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to refresh equipment attributes for {} on rollback",
                    player.getGameProfile().getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void resyncAdvancements(ServerPlayer player, MinecraftServer server) {
        try {
            PlayerList playerList = server.getPlayerList();

            player.getAdvancements().stopListening();

            Field cacheField = PlayerList.class.getDeclaredField("advancements");
            cacheField.setAccessible(true);
            Map<UUID, PlayerAdvancements> cache = (Map<UUID, PlayerAdvancements>) cacheField.get(playerList);
            cache.remove(player.getUUID());

            PlayerAdvancements fresh = playerList.getPlayerAdvancements(player);

            Field playerField = ServerPlayer.class.getDeclaredField("advancements");
            playerField.setAccessible(true);
            playerField.set(player, fresh);

            fresh.flushDirty(player);
        } catch (Exception e) {
            LOGGER.warn("Failed to fully resync advancements for {}", player.getGameProfile().getName(), e);
        }
    }
}
