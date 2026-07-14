package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.WitchFactorAuthority;
import com.mojang.serialization.Codec;
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

    private ModAttachments() {}
}
