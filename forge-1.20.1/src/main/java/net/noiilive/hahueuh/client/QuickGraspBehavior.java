package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.api.AbilityBehavior;
import net.noiilive.hahueuh.api.AbilityContext;
import net.noiilive.hahueuh.network.HandMode;

final class QuickGraspBehavior implements AbilityBehavior.Held {
    private boolean wasDown;

    @Override
    public void onHeldTick(AbilityContext ctx, boolean down) {
        if (down && !wasDown) {
            SlothHandController.INSTANCE.requestQuickAction(HandMode.GRAB);
        }
        wasDown = down;
    }
}
