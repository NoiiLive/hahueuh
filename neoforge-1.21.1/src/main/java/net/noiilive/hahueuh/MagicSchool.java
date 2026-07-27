package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public enum MagicSchool {
    FIRE("fire"),
    WATER("water"),
    EARTH("earth"),
    WIND("wind"),
    YIN("yin"),
    YANG("yang");

    public static final ResourceLocation GENERAL_AUTHORITY =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "magic_general");

    public final String id;
    public final ResourceLocation authorityId;
    public final String translationKey;

    MagicSchool(String id) {
        this.id = id;
        this.authorityId = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, id);
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

    public boolean acquiredBy(IAttachmentHolder holder) {
        return (holder.getData(ModAttachments.PLAYER_MAGIC_SCHOOLS.get()) & bit()) != 0;
    }

    public void grant(ServerPlayer player, boolean acquired) {
        int mask = player.getData(ModAttachments.PLAYER_MAGIC_SCHOOLS.get());
        mask = acquired ? (mask | bit()) : (mask & ~bit());
        player.setData(ModAttachments.PLAYER_MAGIC_SCHOOLS.get(), mask);
    }

    public static boolean canUseGeneralMagic(IAttachmentHolder holder) {
        GateStatus status = holder.getData(ModAttachments.PLAYER_GATE_STATUS.get());
        if (status == GateStatus.DESTROYED) return false;
        if (status == GateStatus.DEFECTIVE) {
            return GateDefectiveVariant.byOrdinal(holder.getData(ModAttachments.PLAYER_GATE_DEFECTIVE_VARIANT.get()))
                    == GateDefectiveVariant.NO_RELEASE;
        }
        return true;
    }

    public static boolean canReleaseMagic(IAttachmentHolder holder) {
        GateStatus status = holder.getData(ModAttachments.PLAYER_GATE_STATUS.get());
        if (status == GateStatus.DESTROYED) return false;
        if (status == GateStatus.DEFECTIVE) {
            return GateDefectiveVariant.byOrdinal(holder.getData(ModAttachments.PLAYER_GATE_DEFECTIVE_VARIANT.get()))
                    != GateDefectiveVariant.NO_RELEASE;
        }
        return true;
    }
}
