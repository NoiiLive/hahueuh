package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.DualWield;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.ClientDualWieldState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class DualWieldClient {
    private DualWieldClient() {}

    @SubscribeEvent
    static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!DualWield.isDualWielding(player)) return;

        event.setSwingHand(false);
        player.swing(ClientDualWieldState.takeSwingHand());
    }
}
