package net.noiilive.hahueuh.network;

public enum BoundVisionAbility {
    VISION_OF_LIFE("hahueuh.ability.vision_of_life"),
    VISION_OF_DANGER("hahueuh.ability.vision_of_danger"),
    VISION_OF_INFORMATION("hahueuh.gui.vision_info.title");

    public final String translationKey;

    BoundVisionAbility(String translationKey) {
        this.translationKey = translationKey;
    }

    public static BoundVisionAbility byOrdinal(int ordinal) {
        BoundVisionAbility[] values = values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : null;
    }
}
