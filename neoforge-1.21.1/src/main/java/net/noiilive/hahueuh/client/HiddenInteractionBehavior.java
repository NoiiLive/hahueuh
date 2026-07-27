package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.api.AbilityBehavior;
import net.noiilive.hahueuh.api.AbilityContext;

final class HiddenInteractionBehavior implements AbilityBehavior.Held {
    private boolean wasDown;

    @Override
    public void onHeldTick(AbilityContext ctx, boolean down) {
        if (down && !wasDown) {
            SlothHandController.INSTANCE.requestHiddenInteraction();
        }
        wasDown = down;
    }
}
