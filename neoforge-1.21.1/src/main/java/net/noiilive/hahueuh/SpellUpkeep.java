package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SpellUpkeep {
    public static final int TICKS_PER_SECOND = 20;

    private SpellUpkeep() {}

    public static boolean drain(ServerPlayer player, int perSecond) {
        if (perSecond <= 0 || player.isCreative()) return true;
        int mana = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        if (mana < perSecond) {
            player.displayClientMessage(Component.translatable("hahueuh.message.spell_upkeep_exhausted")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        player.setData(ModAttachments.PLAYER_MANA_CURRENT.get(), mana - perSecond);
        return true;
    }
}
