package net.noiilive.hahueuh;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;

public enum MagicSchool {
    FIRE("fire"),
    WATER("water"),
    EARTH("earth"),
    WIND("wind"),
    YIN("yin"),
    YANG("yang");

    public static final ResourceLocation GENERAL_AUTHORITY =
            new ResourceLocation(HahUeuh.MODID, "magic_general");

    public final String id;
    public final ResourceLocation authorityId;
    public final String translationKey;

    MagicSchool(String id) {
        this.id = id;
        this.authorityId = new ResourceLocation(HahUeuh.MODID, id);
        this.translationKey = "hahueuh.authority." + id;
    }

    private int bit() {
        return 1 << ordinal();
    }

    public static MagicSchool byId(String id) {
        for (MagicSchool school : values()) {
            if (school.id.equalsIgnoreCase(id)) return school;
        }
        return null;
    }

    public boolean acquiredBy(PlayerData data) {
        return (data.getMagicSchools() & bit()) != 0;
    }

    public void grant(ServerPlayer player, boolean acquired) {
        PlayerData data = PlayerData.get(player);
        int mask = data.getMagicSchools();
        data.setMagicSchools(acquired ? (mask | bit()) : (mask & ~bit()));
        PlayerDataEvents.sync(player);
    }

    public static boolean canUseGeneralMagic(PlayerData data) {
        GateStatus status = data.getGateStatus();
        if (status == GateStatus.DESTROYED) return false;
        if (status == GateStatus.DEFECTIVE) {
            return GateDefectiveVariant.byOrdinal(data.getGateDefectiveVariant())
                    == GateDefectiveVariant.NO_RELEASE;
        }
        return true;
    }

    public static boolean canReleaseMagic(PlayerData data) {
        GateStatus status = data.getGateStatus();
        if (status == GateStatus.DESTROYED) return false;
        if (status == GateStatus.DEFECTIVE) {
            return GateDefectiveVariant.byOrdinal(data.getGateDefectiveVariant())
                    != GateDefectiveVariant.NO_RELEASE;
        }
        return true;
    }
}
