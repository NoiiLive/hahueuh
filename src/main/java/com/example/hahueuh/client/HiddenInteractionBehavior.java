package com.example.hahueuh.client;

import com.example.hahueuh.api.AbilityBehavior;
import com.example.hahueuh.api.AbilityContext;

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
