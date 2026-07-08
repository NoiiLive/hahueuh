package com.example.hahueuh.client;

import com.example.hahueuh.api.AbilityBehavior;
import com.example.hahueuh.api.AbilityContext;
import com.example.hahueuh.network.HandMode;

final class QuickStrikeBehavior implements AbilityBehavior.Held {
    private boolean wasDown;

    @Override
    public void onHeldTick(AbilityContext ctx, boolean down) {
        if (down && !wasDown) {
            SlothHandController.INSTANCE.requestQuickAction(HandMode.ATTACK);
        }
        wasDown = down;
    }
}
