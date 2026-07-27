package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class BodilyDisconnectClient {
    private static final int MOVE_REROLL_TICKS = 20;
    private static final int LOOK_REROLL_TICKS = 30;
    private static final float LOOK_ARC = 90f;
    private static final float YAW_APPROACH = 8f;
    private static final float PITCH_APPROACH = 5f;
    private static final int HOTBAR_SWAP_MIN_TICKS = 20;
    private static final int HOTBAR_SWAP_MAX_TICKS = 60;

    private static int moveTick;
    private static float rolledForward;
    private static float rolledStrafe;
    private static boolean rolledJump;

    private static int lookTick = -1;
    private static float targetYaw;
    private static float targetPitch;
    private static int actionTimer;
    private static int hotbarTimer;

    private BodilyDisconnectClient() {}

    @SubscribeEvent
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientMagicState.bodilyDisconnected()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (moveTick % MOVE_REROLL_TICKS == 0) {
            var rnd = player.getRandom();
            rolledForward = rnd.nextInt(3) - 1;
            rolledStrafe = rnd.nextInt(3) - 1;
            rolledJump = rnd.nextFloat() < 0.15f;
        }
        moveTick++;

        Input input = event.getInput();
        input.forwardImpulse = rolledForward;
        input.leftImpulse = rolledStrafe;
        input.up = rolledForward > 0;
        input.down = rolledForward < 0;
        input.left = rolledStrafe > 0;
        input.right = rolledStrafe < 0;
        input.jumping = rolledJump;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !ClientMagicState.bodilyDisconnected()) {
            lookTick = -1;
            return;
        }

        if (lookTick < 0) {
            targetYaw = player.getYRot();
            targetPitch = player.getXRot();
            actionTimer = 10 + player.getRandom().nextInt(31);
            hotbarTimer = rollHotbarDelay(player);
            lookTick = 0;
        }
        if (lookTick % LOOK_REROLL_TICKS == 0) {
            var rnd = player.getRandom();
            targetYaw = player.getYRot() + (rnd.nextFloat() * LOOK_ARC - LOOK_ARC / 2f);
            targetPitch = rnd.nextFloat() * 60f - 30f;
        }
        lookTick++;

        float yaw = approachDegrees(player.getYRot(), targetYaw, YAW_APPROACH);
        float pitch = approachDegrees(player.getXRot(), targetPitch, PITCH_APPROACH);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);

        if (--hotbarTimer <= 0) {
            hotbarTimer = rollHotbarDelay(player);
            swapHotbarSlot(mc, player);
        }

        if (--actionTimer <= 0) {
            actionTimer = 10 + player.getRandom().nextInt(31);
            if (player.getRandom().nextBoolean()) {
                performAttack(mc, player);
            } else {
                performUse(mc, player);
            }
        }
    }

    private static int rollHotbarDelay(LocalPlayer player) {
        return HOTBAR_SWAP_MIN_TICKS + player.getRandom().nextInt(HOTBAR_SWAP_MAX_TICKS - HOTBAR_SWAP_MIN_TICKS + 1);
    }

    private static void swapHotbarSlot(Minecraft mc, LocalPlayer player) {
        int newSlot = player.getRandom().nextInt(9);
        if (newSlot == player.getInventory().selected) return;
        player.getInventory().selected = newSlot;
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(newSlot));
        }
    }

    private static void performAttack(Minecraft mc, LocalPlayer player) {
        player.swing(InteractionHand.MAIN_HAND);
        if (mc.hitResult instanceof EntityHitResult hit && mc.gameMode != null) {
            Entity target = hit.getEntity();
            if (target != null && target != player) {
                mc.gameMode.attack(player, target);
            }
        }
    }

    private static void performUse(Minecraft mc, LocalPlayer player) {
        if (mc.gameMode == null) return;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = mc.hitResult;

        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            InteractionResult result = mc.gameMode.useItemOn(player, hand, blockHit);
            if (result.consumesAction()) {
                if (result.shouldSwing()) player.swing(hand);
                return;
            }
        } else if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            InteractionResult result = mc.gameMode.interactAt(player, entity, entityHit, hand);
            if (!result.consumesAction()) result = mc.gameMode.interact(player, entity, hand);
            if (result.consumesAction()) {
                if (result.shouldSwing()) player.swing(hand);
                return;
            }
        }

        if (!stack.isEmpty()) {
            InteractionResult result = mc.gameMode.useItem(player, hand);
            if (result.consumesAction() && result.shouldSwing()) player.swing(hand);
        }
    }

    @SubscribeEvent
    static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (ClientMagicState.bodilyDisconnected() && (event.isAttack() || event.isUseItem() || event.isPickBlock())) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }
}
