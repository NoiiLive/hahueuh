package net.noiilive.hahueuh.network;


import java.util.List;

public final class EfficientEnchantOptionsData {
    private EfficientEnchantOptionsData() {}

    private static volatile List<EfficientEnchantOptionsPacket.Option> options = List.of();

    public static void set(List<EfficientEnchantOptionsPacket.Option> latest) {
        options = latest;
    }

    public static List<EfficientEnchantOptionsPacket.Option> get() {
        return options;
    }

    public static void clear() {
        options = List.of();
    }
}
