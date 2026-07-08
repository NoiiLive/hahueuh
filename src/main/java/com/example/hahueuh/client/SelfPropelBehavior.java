package com.example.hahueuh.client;

import com.example.hahueuh.api.AbilityBehavior;
import com.example.hahueuh.api.AbilityContext;

final class SelfPropelBehavior implements AbilityBehavior.Held {
    @Override
    public void onHeldTick(AbilityContext ctx, boolean down) {
        SlothHandController.INSTANCE.reportSelfPropelHeld(down);
    }
}
