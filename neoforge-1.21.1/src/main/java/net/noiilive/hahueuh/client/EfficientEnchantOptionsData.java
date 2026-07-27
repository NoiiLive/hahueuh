package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.network.EfficientEnchantOptionsPayload;

import java.util.List;

public final class EfficientEnchantOptionsData {
    private EfficientEnchantOptionsData() {}

    private static volatile List<EfficientEnchantOptionsPayload.Option> options = List.of();

    public static void set(List<EfficientEnchantOptionsPayload.Option> latest) {
        options = latest;
    }

    public static List<EfficientEnchantOptionsPayload.Option> get() {
        return options;
    }

    public static void clear() {
        options = List.of();
    }
}
