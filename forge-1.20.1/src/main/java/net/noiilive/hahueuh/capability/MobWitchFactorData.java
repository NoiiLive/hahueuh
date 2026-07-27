package net.noiilive.hahueuh.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.noiilive.hahueuh.network.WitchFactorAuthority;

public class MobWitchFactorData {
    private WitchFactorAuthority authority = WitchFactorAuthority.NONE;
    private String variant = "";
    private int fingerHands;

    public WitchFactorAuthority getAuthority() { return authority; }
    public void setAuthority(WitchFactorAuthority authority) { this.authority = authority; }

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }

    public int getFingerHands() { return fingerHands; }
    public void setFingerHands(int hands) { this.fingerHands = hands; }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Authority", authority.id);
        tag.putString("Variant", variant);
        tag.putInt("FingerHands", fingerHands);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        authority = WitchFactorAuthority.byId(tag.getString("Authority"));
        variant = tag.getString("Variant");
        fingerHands = tag.getInt("FingerHands");
    }

    public static MobWitchFactorData get(Entity entity) {
        return entity.getCapability(ModCapabilities.MOB_WITCH_FACTOR)
                .orElseGet(MobWitchFactorData::new);
    }
}
