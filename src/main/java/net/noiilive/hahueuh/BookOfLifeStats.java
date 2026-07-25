package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public final class BookOfLifeStats {
    public static final int OVERCHARGE_CAP_MULTIPLIER = 2;

    private BookOfLifeStats() {}

    public static int maxOd(IAttachmentHolder player) {
        int lifespan = Math.max(0, player.getData(ModAttachments.PLAYER_LIFESPAN.get()));
        return lifespan * ConfigMagic.OD_LIFESPAN_MULTIPLIER.getAsInt();
    }

    public static int maxMana(IAttachmentHolder player) {
        return maxOd(player);
    }

    public static void setOdToMax(ServerPlayer player) {
        player.setData(ModAttachments.PLAYER_OD_CURRENT.get(), maxOd(player));
    }

    public static void clampToMax(ServerPlayer player) {
        int maxOd = maxOd(player);
        int currentOd = player.getData(ModAttachments.PLAYER_OD_CURRENT.get());
        if (currentOd > maxOd) {
            player.setData(ModAttachments.PLAYER_OD_CURRENT.get(), maxOd);
        }

        int overchargeCap = maxMana(player) * OVERCHARGE_CAP_MULTIPLIER;
        int currentMana = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        if (currentMana > overchargeCap) {
            player.setData(ModAttachments.PLAYER_MANA_CURRENT.get(), overchargeCap);
        }
    }
}
