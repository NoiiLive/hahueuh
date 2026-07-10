package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.api.Ability;
import net.noiilive.hahueuh.api.AbilityCooldowns;
import net.noiilive.hahueuh.api.AbilityContext;
import net.noiilive.hahueuh.api.AbilityRegistry;
import net.noiilive.hahueuh.client.gui.AbilityScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class AbilityClient {
    private AbilityClient() {}

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.hahueuh.open_gui", GLFW.GLFW_KEY_G, "key.categories.hahueuh");
    public static final KeyMapping CYCLE_SLOTS_KEY = new KeyMapping(
            "key.hahueuh.cycle_slots", GLFW.GLFW_KEY_Z, "key.categories.hahueuh");
    public static final KeyMapping SLOT_KEY_1 = new KeyMapping(
            "key.hahueuh.slot_1", GLFW.GLFW_KEY_X, "key.categories.hahueuh");
    public static final KeyMapping SLOT_KEY_2 = new KeyMapping(
            "key.hahueuh.slot_2", GLFW.GLFW_KEY_C, "key.categories.hahueuh");
    public static final KeyMapping SLOT_KEY_3 = new KeyMapping(
            "key.hahueuh.slot_3", GLFW.GLFW_KEY_V, "key.categories.hahueuh");
    public static final KeyMapping HIDE_HUD_KEY = new KeyMapping(
            "key.hahueuh.hide_hud", GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.hahueuh");

    private static final KeyMapping[] SLOT_KEYS = {SLOT_KEY_1, SLOT_KEY_2, SLOT_KEY_3};
    private static final boolean[] slotTapHandledThisHold = new boolean[SLOT_KEYS.length];

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI_KEY);
        event.register(CYCLE_SLOTS_KEY);
        event.register(SLOT_KEY_1);
        event.register(SLOT_KEY_2);
        event.register(SLOT_KEY_3);
        event.register(HIDE_HUD_KEY);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            UnseenHandState.setActive(false);
            return;
        }

        while (OPEN_GUI_KEY.consumeClick()) {
            if (mc.screen instanceof AbilityScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new AbilityScreen());
            }
        }
        while (CYCLE_SLOTS_KEY.consumeClick()) AbilitySlots.advanceCycleGroup();
        while (HIDE_HUD_KEY.consumeClick()) AbilitySlots.toggleHudHidden();

        int groupBase = AbilitySlots.cycleGroup() * AbilitySlots.GROUP_SIZE;
        Ability[] slotAbility = new Ability[SLOT_KEYS.length];
        boolean[] slotDown = new boolean[SLOT_KEYS.length];
        boolean[] slotJustPressed = new boolean[SLOT_KEYS.length];
        for (int i = 0; i < SLOT_KEYS.length; i++) {
            KeyMapping key = SLOT_KEYS[i];
            boolean down = key.isDown();
            boolean hadClick = false;
            while (key.consumeClick()) hadClick = true;
            boolean justPressed = hadClick && !slotTapHandledThisHold[i];
            if (justPressed) slotTapHandledThisHold[i] = true;
            if (!down) slotTapHandledThisHold[i] = false;

            slotAbility[i] = AbilitySlots.get(groupBase + i);
            slotDown[i] = down;
            slotJustPressed[i] = justPressed;
        }

        for (int i = 0; i < SLOT_KEYS.length; i++) {
            Ability ability = slotAbility[i];
            if (ability != null && slotJustPressed[i] && !ability.holdBased()) {
                ability.onActivate(new SlotContext(player, ability, slotDown[i]));
            }
        }

        for (Ability ability : AbilityRegistry.all()) {
            if (!ability.holdBased() || !ability.isAvailable()) continue;
            boolean down = false;
            for (int i = 0; i < SLOT_KEYS.length; i++) {
                if (slotAbility[i] == ability && slotDown[i]) {
                    down = true;
                    break;
                }
            }
            ability.onHeldTick(new SlotContext(player, ability, down), down);
        }

        SlothHandController.INSTANCE.tick(player);

        boolean onRealGround = player.onGround()
                && !player.getBlockStateOn().getCollisionShape(player.level(), player.getOnPos()).isEmpty();
        net.noiilive.hahueuh.network.ClientLionsHeartState.updateFloor(
                player.getY(), player.onGround(), onRealGround,
                mc.options.keyJump.isDown(), mc.options.keyShift.isDown(), player.isUnderWater());
    }

    @SubscribeEvent
    static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (UnseenHandState.isActive() && (event.isAttack() || event.isUseItem())) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!UnseenHandState.isActive()) return;
        double range = UnseenHandState.adjustMaxRange(event.getScrollDeltaY());
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("hahueuh.message.hand_reach", Math.round(range))
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        }
        event.setCanceled(true);
    }

    private static final class SlotContext implements AbilityContext {
        private final Player player;
        private final Ability ability;
        private final boolean anyDown;

        SlotContext(Player player, Ability ability, boolean anyDown) {
            this.player = player;
            this.ability = ability;
            this.anyDown = anyDown;
        }

        @Override
        public Player player() {
            return player;
        }

        @Override
        public boolean isOnCooldown() {
            return AbilityCooldowns.secondsRemaining(ability.cooldownId()) > 0;
        }

        @Override
        public int cooldownSecondsRemaining() {
            return AbilityCooldowns.secondsRemaining(ability.cooldownId());
        }

        @Override
        public void startCooldown(double seconds) {
            AbilityCooldowns.startCooldown(ability.cooldownId(), seconds);
        }

        @Override
        public boolean anyBoundSlotDown() {
            return anyDown;
        }
    }
}
