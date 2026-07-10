package net.noiilive.hahueuh.api;

public final class AbilityBehavior {
    private AbilityBehavior() {}

    @FunctionalInterface
    public interface Tap {
        void onActivate(AbilityContext ctx);
    }

    @FunctionalInterface
    public interface Held {
        void onHeldTick(AbilityContext ctx, boolean down);
    }
}
