package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ClientMagicState {
    private ClientMagicState() {}

    public static boolean hasYin() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && net.noiilive.hahueuh.MagicSchool.YIN.acquiredBy(player);
    }

    public static boolean sensoryDeprived() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(ModEffects.SENSORY_DEPRIVATION);
    }

    public static boolean bodilyDisconnected() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(ModEffects.BODILY_DISCONNECT);
    }

    public static boolean hasStoredSpell() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.getData(net.noiilive.hahueuh.ModAttachments.PLAYER_STORED_SPELL.get()).isEmpty();
    }

    public static boolean sealed() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getData(net.noiilive.hahueuh.ModAttachments.PLAYER_SEALED.get());
    }

    public static boolean emmActive() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getData(net.noiilive.hahueuh.ModAttachments.PLAYER_EMM_ACTIVE.get());
    }

    public static boolean hasTrappedEntities() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getData(net.noiilive.hahueuh.ModAttachments.PLAYER_HAS_TRAPPED_ENTITIES.get());
    }
}
