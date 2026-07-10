package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.api.AbilityBehavior;
import net.noiilive.hahueuh.api.AbilityContext;

final class SummonHandBehavior implements AbilityBehavior.Held {
    @Override
    public void onHeldTick(AbilityContext ctx, boolean down) {
        SlothHandController.INSTANCE.reportSummonHeld(down);
    }
}
