package net.noiilive.hahueuh.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.hahueuh.ConfigMagicYin;
import net.noiilive.hahueuh.network.ClientMurakState;
import net.noiilive.hahueuh.network.MurakFlightTogglePayload;

@EventBusSubscriber(modid = net.noiilive.hahueuh.HahUeuh.MODID, value = Dist.CLIENT)
public final class MurakClient {
    private static final int DOUBLE_TAP_WINDOW_TICKS = 8;
    private static final double COLLISION_DAMP = 0.4;

    private static boolean jumpWasDown;
    private static int ticksSinceJumpTap = Integer.MAX_VALUE;

    private static Vec3 momentum = Vec3.ZERO;
    private static int ticksUntilGust;

    private MurakClient() {}

    public static void reset() {
        jumpWasDown = false;
        ticksSinceJumpTap = Integer.MAX_VALUE;
        momentum = Vec3.ZERO;
        ticksUntilGust = 0;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            jumpWasDown = false;
            return;
        }
        if (!ClientMurakState.hasReducedGravity()) {
            reset();
            return;
        }

        if (ticksSinceJumpTap != Integer.MAX_VALUE) ticksSinceJumpTap++;

        boolean jumpDown = mc.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown) {
            if (ticksSinceJumpTap <= DOUBLE_TAP_WINDOW_TICKS) {
                ticksSinceJumpTap = Integer.MAX_VALUE;
                PacketDistributor.sendToServer(new MurakFlightTogglePayload(!ClientMurakState.isFlying()));
            } else {
                ticksSinceJumpTap = 0;
            }
        }
        jumpWasDown = jumpDown;

        if (!ClientMurakState.isFlying()) momentum = Vec3.ZERO;
    }

    private static int rollGustDelay(LocalPlayer player) {
        int average = Math.max(1, ConfigMagicYin.MURAK_GUST_INTERVAL_SECONDS.get() * 20);
        int jitter = Math.max(1, average / 2);
        return average - jitter / 2 + player.getRandom().nextInt(jitter + 1);
    }

    @SubscribeEvent
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientMurakState.isFlying()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getEntity() != player) return;

        Input input = event.getInput();
        float forward = input.forwardImpulse;
        float strafe = input.leftImpulse;
        boolean rising = input.jumping;
        boolean sinking = input.shiftKeyDown;

        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;

        double impulse = ConfigMagicYin.MURAK_FLIGHT_IMPULSE.get();
        double drag = ConfigMagicYin.MURAK_FLIGHT_DRAG.get();
        double gustStrength = ConfigMagicYin.MURAK_GUST_STRENGTH.get();
        double maxSpeed = ConfigMagicYin.MURAK_FLIGHT_MAX_SPEED.get();

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() > 1.0e-6 ? flat.normalize() : new Vec3(0.0, 0.0, 1.0);
        Vec3 left = new Vec3(flat.z, 0.0, -flat.x);

        Vec3 push = Vec3.ZERO;
        if (forward != 0f) push = push.add(look.scale(forward));
        if (strafe != 0f) push = push.add(left.scale(strafe));
        if (push.lengthSqr() > 1.0e-6) push = push.normalize().scale(impulse);
        if (rising) push = push.add(0.0, impulse, 0.0);
        if (sinking) push = push.add(0.0, -impulse, 0.0);

        Vec3 gust = Vec3.ZERO;
        if (gustStrength > 0.0) {
            if (ticksUntilGust <= 0) {
                ticksUntilGust = rollGustDelay(player);
                double theta = player.getRandom().nextDouble() * Math.PI * 2.0;
                double vertical = (player.getRandom().nextDouble() - 0.5) * 0.6;
                gust = new Vec3(Math.cos(theta), vertical, Math.sin(theta)).normalize().scale(gustStrength);
            } else {
                ticksUntilGust--;
            }
        }

        momentum = momentum.scale(drag).add(push).add(gust);

        if (player.horizontalCollision) {
            momentum = new Vec3(momentum.x * COLLISION_DAMP, momentum.y, momentum.z * COLLISION_DAMP);
        }
        if (player.verticalCollision) {
            momentum = new Vec3(momentum.x, momentum.y * COLLISION_DAMP, momentum.z);
        }

        double speed = momentum.length();
        if (speed > maxSpeed) momentum = momentum.scale(maxSpeed / speed);
        if (speed < 1.0e-4) momentum = Vec3.ZERO;

        player.setDeltaMovement(momentum);
        player.fallDistance = 0f;
    }
}
