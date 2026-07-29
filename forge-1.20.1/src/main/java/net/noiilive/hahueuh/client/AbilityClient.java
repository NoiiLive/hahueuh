package net.noiilive.hahueuh.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.api.Ability;
import net.noiilive.hahueuh.api.AbilityContext;
import net.noiilive.hahueuh.api.AbilityCooldowns;
import net.noiilive.hahueuh.api.AbilityRegistry;
import net.noiilive.hahueuh.client.gui.BookOfLifeScreen;
import net.noiilive.hahueuh.network.ManaChargePacket;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.client.gui.AbilityMenuScreen;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class AbilityClient {
    private AbilityClient() {}

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.hahueuh.open_gui", KeyConflictContext.IN_GAME, InputKey.of(GLFW.GLFW_KEY_G), "key.categories.hahueuh");
    public static final KeyMapping CYCLE_SLOTS_KEY = new KeyMapping(
            "key.hahueuh.cycle_slots", KeyConflictContext.IN_GAME, InputKey.of(GLFW.GLFW_KEY_Z), "key.categories.hahueuh");
    public static final KeyMapping SLOT_KEY_1 = new KeyMapping(
            "key.hahueuh.slot_1", KeyConflictContext.IN_GAME, InputKey.of(GLFW.GLFW_KEY_X), "key.categories.hahueuh");
    public static final KeyMapping SLOT_KEY_2 = new KeyMapping(
            "key.hahueuh.slot_2", KeyConflictContext.IN_GAME, InputKey.of(GLFW.GLFW_KEY_C), "key.categories.hahueuh");
    public static final KeyMapping SLOT_KEY_3 = new KeyMapping(
            "key.hahueuh.slot_3", KeyConflictContext.IN_GAME, InputKey.of(GLFW.GLFW_KEY_V), "key.categories.hahueuh");
    public static final KeyMapping HIDE_HUD_KEY = new KeyMapping(
            "key.hahueuh.hide_hud", KeyConflictContext.IN_GAME, InputKey.of(GLFW.GLFW_KEY_GRAVE_ACCENT), "key.categories.hahueuh");

    public static final KeyMapping CHARGE_MANA_KEY = new KeyMapping(
            "key.hahueuh.charge_mana", KeyConflictContext.IN_GAME, InputKey.of(GLFW.GLFW_KEY_LEFT_ALT), "key.categories.hahueuh");

    private static final KeyMapping[] SLOT_KEYS = {SLOT_KEY_1, SLOT_KEY_2, SLOT_KEY_3};
    private static final boolean[] slotTapHandledThisHold = new boolean[SLOT_KEYS.length];
    private static boolean wasChargingMana;

    public static void resetChargeManaState() {
        wasChargingMana = false;
    }

    @Mod.EventBusSubscriber(modid = HahUeuh.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_GUI_KEY);
            event.register(CYCLE_SLOTS_KEY);
            event.register(SLOT_KEY_1);
            event.register(SLOT_KEY_2);
            event.register(SLOT_KEY_3);
            event.register(HIDE_HUD_KEY);
            event.register(CHARGE_MANA_KEY);
        }

        private ModBus() {}
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Minecraft.getInstance().isPaused()) AbilityCooldowns.tick();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        while (OPEN_GUI_KEY.consumeClick()) {
            if (mc.screen instanceof net.noiilive.hahueuh.client.gui.BookPageScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new BookOfLifeScreen());
            }
        }

        boolean chargingMana = CHARGE_MANA_KEY.isDown();
        if (chargingMana != wasChargingMana) {
            wasChargingMana = chargingMana;
            ModNetworking.CHANNEL.sendToServer(new ManaChargePacket(chargingMana));
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

        if (net.noiilive.hahueuh.network.AllyTrackerData.consumeOpenRequest() && mc.screen == null) {
            mc.setScreen(new net.noiilive.hahueuh.client.gui.AllyTrackerScreen());
        }

        boolean onRealGround = player.onGround()
                && !player.getBlockStateOn().getCollisionShape(player.level(), player.getOnPos()).isEmpty();
        net.noiilive.hahueuh.network.ClientLionsHeartState.updateFloor(
                player.getY(), player.onGround(), onRealGround,
                mc.options.keyJump.isDown(), mc.options.keyShift.isDown(), player.isUnderWater());
    }

    @SubscribeEvent
    public static void onClickInput(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (UnseenHandState.isActive() && (event.isAttack() || event.isUseItem())) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(net.minecraftforge.client.event.InputEvent.MouseScrollingEvent event) {
        if (!UnseenHandState.isActive()) return;
        double range = UnseenHandState.adjustMaxRange(event.getScrollDelta());
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "hahueuh.message.hand_reach", Math.round(range))
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        }
        event.setCanceled(true);
    }

    private static final class InputKey {
        static com.mojang.blaze3d.platform.InputConstants.Key of(int keyCode) {
            return com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        }

        private InputKey() {}
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
