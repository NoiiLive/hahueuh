package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GateStatus;
import net.noiilive.hahueuh.network.PlayerRace;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HahUeuh.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<WitchFactorAuthority>> MOB_WITCH_FACTOR =
            ATTACHMENT_TYPES.register("mob_witch_factor",
                    () -> AttachmentType.builder(() -> WitchFactorAuthority.NONE)
                            .serialize(Codec.STRING.xmap(WitchFactorAuthority::byId, a -> a.id))
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> MOB_WITCH_FACTOR_VARIANT =
            ATTACHMENT_TYPES.register("mob_witch_factor_variant",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MOB_FINGER_HANDS =
            ATTACHMENT_TYPES.register("mob_finger_hands",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ChunkMana>> CHUNK_AMBIENT_MANA =
            ATTACHMENT_TYPES.register("chunk_ambient_mana",
                    () -> AttachmentType.builder(() -> ChunkMana.UNINITIALIZED)
                            .serialize(ChunkMana.CODEC)
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ChunkMiasma>> CHUNK_MIASMA =
            ATTACHMENT_TYPES.register("chunk_miasma",
                    () -> AttachmentType.builder(() -> ChunkMiasma.UNINITIALIZED)
                            .serialize(ChunkMiasma.CODEC)
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerRace>> PLAYER_RACE =
            ATTACHMENT_TYPES.register("player_race",
                    () -> AttachmentType.builder(() -> PlayerRace.HUMAN)
                            .serialize(Codec.STRING.xmap(PlayerRace::byId, r -> r.id))
                            .sync(ByteBufCodecs.VAR_INT.map(PlayerRace::byOrdinal, Enum::ordinal))
                            .copyOnDeath()
                            .build());

    public static final int DEFAULT_PLAYER_AGE = 16;

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_AGE =
            ATTACHMENT_TYPES.register("player_age",
                    () -> AttachmentType.builder(() -> DEFAULT_PLAYER_AGE)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GateStatus>> PLAYER_GATE_STATUS =
            ATTACHMENT_TYPES.register("player_gate_status",
                    () -> AttachmentType.builder(() -> GateStatus.OPEN)
                            .serialize(Codec.STRING.xmap(GateStatus::byId, s -> s.id))
                            .sync(ByteBufCodecs.VAR_INT.map(GateStatus::byOrdinal, Enum::ordinal))
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_GATE_DEFECTIVE_VARIANT =
            ATTACHMENT_TYPES.register("player_gate_defective_variant",
                    () -> AttachmentType.builder(() -> -1)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_GATE_OUTPUT =
            ATTACHMENT_TYPES.register("player_gate_output",
                    () -> AttachmentType.builder(() -> -1)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_GATE_EFFICIENCY =
            ATTACHMENT_TYPES.register("player_gate_efficiency",
                    () -> AttachmentType.builder(() -> -1)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_GATE_STRAIN =
            ATTACHMENT_TYPES.register("player_gate_strain",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_MANA_CURRENT =
            ATTACHMENT_TYPES.register("player_mana_current",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_OD_CURRENT =
            ATTACHMENT_TYPES.register("player_od_current",
                    () -> AttachmentType.builder(() -> 100)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_LIFESPAN =
            ATTACHMENT_TYPES.register("player_lifespan",
                    () -> AttachmentType.builder(() -> -1)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_AGE_LAST_DAY =
            ATTACHMENT_TYPES.register("player_age_last_day",
                    () -> AttachmentType.builder(() -> -1)
                            .serialize(Codec.INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_REPUTATION =
            ATTACHMENT_TYPES.register("player_reputation",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_MAGIC_SCHOOLS =
            ATTACHMENT_TYPES.register("player_magic_schools",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> PLAYER_OD_DEPLETED =
            ATTACHMENT_TYPES.register("player_od_depleted",
                    () -> AttachmentType.builder(() -> Boolean.FALSE)
                            .serialize(Codec.BOOL)
                            .sync(ByteBufCodecs.BOOL)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_SPELL_HEAT =
            ATTACHMENT_TYPES.register("player_spell_heat",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.VAR_INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> PLAYER_HEAT_LAST_RESET_DAY =
            ATTACHMENT_TYPES.register("player_heat_last_reset_day",
                    () -> AttachmentType.builder(() -> -1)
                            .serialize(Codec.INT)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> PLAYER_STORED_SPELL =
            ATTACHMENT_TYPES.register("player_stored_spell",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .sync(ByteBufCodecs.STRING_UTF8)
                            .copyOnDeath()
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> PLAYER_HAS_TRAPPED_ENTITIES =
            ATTACHMENT_TYPES.register("player_has_trapped_entities",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .sync(ByteBufCodecs.BOOL)
                            .copyOnDeath()
                            .build());

    private ModAttachments() {}
}
