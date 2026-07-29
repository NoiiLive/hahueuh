package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class EmmClient {
    private EmmClient() {}

    @SubscribeEvent
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientMagicState.emmActive()) return;

        Input input = event.getInput();
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!ClientMagicState.emmActive()) return;
        event.setSwingHand(false);
        event.setCanceled(true);
    }
}
