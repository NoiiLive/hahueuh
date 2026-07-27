package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.ConfigMagicYin;
import net.noiilive.hahueuh.client.gui.SpellStorageScreen;
import net.noiilive.hahueuh.network.AlShamakActivatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AlShamakClient {
    private AlShamakClient() {}

    public static void activate() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (ClientMagicState.hasTrappedEntities() || ClientMagicState.hasStoredSpell()) {
            if (player.isShiftKeyDown()) {
                PacketDistributor.sendToServer(new AlShamakActivatePayload(AlShamakActivatePayload.DISCARD, -1));
                return;
            }
            Entity releaseTarget = UlMinyaClient.raycastEntity(player, ConfigMagicYin.UL_MINYA_RANGE.get());
            int releaseTargetId = releaseTarget != null ? releaseTarget.getId() : -1;
            PacketDistributor.sendToServer(new AlShamakActivatePayload(AlShamakActivatePayload.RELEASE, releaseTargetId));
            return;
        }

        Entity target = UlMinyaClient.raycastEntity(player, ConfigMagicYin.AL_SHAMAK_RANGE.get());
        if (target != null) {
            PacketDistributor.sendToServer(new AlShamakActivatePayload(AlShamakActivatePayload.BANISH, target.getId()));
        } else {
            mc.setScreen(new SpellStorageScreen());
        }
    }
}
