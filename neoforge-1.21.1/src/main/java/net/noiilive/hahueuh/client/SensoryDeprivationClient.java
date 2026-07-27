package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class SensoryDeprivationClient {
    private static final float FADE_SECONDS = 0.5f;

    private static float overlayAlpha;
    private static long lastFrameNanos;

    private SensoryDeprivationClient() {}

    public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        boolean active = ClientMagicState.sensoryDeprived() || ClientMagicState.bodilyDisconnected();

        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0f : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        dt = Math.min(dt, 0.1f);

        float rate = 1f / FADE_SECONDS;
        float target = active ? 1f : 0f;
        if (overlayAlpha < target) overlayAlpha = Math.min(target, overlayAlpha + rate * dt);
        else if (overlayAlpha > target) overlayAlpha = Math.max(target, overlayAlpha - rate * dt);

        if (overlayAlpha <= 0.001f) return;
        int a = (int) (overlayAlpha * 255f) & 0xFF;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), a << 24);
    }

    @SubscribeEvent
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientMagicState.sensoryDeprived()) return;
        Input input = event.getInput();
        input.forwardImpulse = -input.forwardImpulse;
        input.leftImpulse = -input.leftImpulse;

        boolean up = input.up;
        input.up = input.down;
        input.down = up;
        boolean left = input.left;
        input.left = input.right;
        input.right = left;
    }
}
