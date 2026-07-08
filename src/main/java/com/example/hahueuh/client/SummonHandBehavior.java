package com.example.hahueuh.client;

import com.example.hahueuh.api.AbilityBehavior;
import com.example.hahueuh.api.AbilityContext;

final class SummonHandBehavior implements AbilityBehavior.Held {
    @Override
    public void onHeldTick(AbilityContext ctx, boolean down) {
        SlothHandController.INSTANCE.reportSummonHeld(down);
    }
}
