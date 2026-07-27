package net.noiilive.hahueuh.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    public static final Capability<PlayerData> PLAYER_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<ChunkMana> CHUNK_MANA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<ChunkMiasma> CHUNK_MIASMA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<MobWitchFactorData> MOB_WITCH_FACTOR =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerData.class);
        event.register(ChunkMana.class);
        event.register(ChunkMiasma.class);
        event.register(MobWitchFactorData.class);
    }

    private ModCapabilities() {}
}
