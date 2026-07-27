package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.api.AbilityBehavior;
import net.noiilive.hahueuh.api.AbilityContext;

final class SelfPropelBehavior implements AbilityBehavior.Held {
    @Override
    public void onHeldTick(AbilityContext ctx, boolean down) {
        SlothHandController.INSTANCE.reportSelfPropelHeld(down);
    }
}
