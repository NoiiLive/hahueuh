package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.api.AbilityBehavior;
import net.noiilive.hahueuh.api.AbilityContext;

final class SummonHandBehavior implements AbilityBehavior.Tap {
    @Override
    public void onActivate(AbilityContext ctx) {
        SlothHandController.INSTANCE.requestSummonToggle();
    }
}
