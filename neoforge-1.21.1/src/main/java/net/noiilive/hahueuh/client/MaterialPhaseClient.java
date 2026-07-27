package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.ClientMaterialPhaseState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class MaterialPhaseClient {
    private MaterialPhaseClient() {}

    @SubscribeEvent
    static void onBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK
                && ClientMaterialPhaseState.isActive()) {
            event.setCanceled(true);
        }
    }
}
