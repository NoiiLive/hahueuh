package net.noiilive.hahueuh.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class EmmLockFeedback {
    private static long lastShownAtMillis;

    private EmmLockFeedback() {}

    public static void deny() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastShownAtMillis < 500L) return;
        lastShownAtMillis = now;
        mc.player.displayClientMessage(
                Component.translatable("hahueuh.message.emm_locked").withStyle(ChatFormatting.RED), true);
    }
}
