package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;
import net.minecraft.world.effect.MobEffectInstance;

public final class SpellUpkeep {
    public static final int TICKS_PER_SECOND = 20;
    public static final int UNTIMED_TICKS = 1_000_000_000;
    private static final int UNTIMED_THRESHOLD = 1_000_000;

    private SpellUpkeep() {}

    public static boolean isUntimed(MobEffectInstance instance) {
        return instance != null && instance.getDuration() > UNTIMED_THRESHOLD;
    }

    public static boolean drain(ServerPlayer player, int perSecond) {
        if (perSecond <= 0 || player.isCreative()) return true;
        PlayerData data = PlayerData.get(player);
        int mana = data.getManaCurrent();
        if (mana < perSecond) {
            player.displayClientMessage(Component.translatable("hahueuh.message.spell_upkeep_exhausted")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        data.setManaCurrent(mana - perSecond);
        PlayerDataEvents.sync(player);
        return true;
    }
}
