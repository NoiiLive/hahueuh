package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.api.Ability;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class AbilityHud {
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/hud_panel.png");
    private static final ResourceLocation SLOT = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/hud_slot.png");

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 3;
    private static final int HEADER_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int PANEL_WIDTH = SLOT_SIZE + PADDING * 2;
    private static final int PANEL_HEIGHT = HEADER_HEIGHT
            + AbilitySlots.GROUP_SIZE * SLOT_SIZE + (AbilitySlots.GROUP_SIZE - 1) * SLOT_GAP + PADDING;
    private static final int MARGIN_X = 8;
    private static final String[] KEY_LABELS = {"X", "C", "V"};

    private static final int ICON_SIZE = 16;
    private static final int ICON_OFFSET = (SLOT_SIZE - ICON_SIZE) / 2;
    private static final float COOLDOWN_TINT = 0.35f;
    private static final int COOLDOWN_TEXT_COLOR = 0x555555;
    private static final int NORMAL_TEXT_COLOR = 0xFFFFFF;
    private static final int COOLDOWN_NUMBER_COLOR = 0xFF3B3B;
    private static final int HIDDEN_X = -(PANEL_WIDTH + 40);
    private static final float SLIDE_SECONDS = 0.25f;

    private static boolean slideInitialized;
    private static float slideProgress;
    private static long lastSlideNanos;

    private AbilityHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        float progress = advanceSlide();
        if (progress <= 0.001f) return;

        int x = (int) Mth.lerp(progress, HIDDEN_X, MARGIN_X);
        int y = (graphics.guiHeight() - PANEL_HEIGHT) / 2;

        graphics.blit(PANEL, x, y, PANEL_WIDTH, PANEL_HEIGHT, 0f, 0f, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

        int group = AbilitySlots.cycleGroup();
        graphics.drawCenteredString(mc.font, Component.literal((group + 1) + "/" + AbilitySlots.GROUP_COUNT),
                x + PANEL_WIDTH / 2, y + 3, NORMAL_TEXT_COLOR);

        boolean[] keyDown = {
                AbilityClient.SLOT_KEY_1.isDown(),
                AbilityClient.SLOT_KEY_2.isDown(),
                AbilityClient.SLOT_KEY_3.isDown()
        };
        for (int i = 0; i < AbilitySlots.GROUP_SIZE; i++) {
            int slotIndex = group * AbilitySlots.GROUP_SIZE + i;
            Ability ability = AbilitySlots.get(slotIndex);
            int sx = x + PADDING;
            int sy = y + HEADER_HEIGHT + i * (SLOT_SIZE + SLOT_GAP);
            int state = (ability != null && keyDown[i]) ? 1 : 0;
            graphics.blit(SLOT, sx, sy, SLOT_SIZE, SLOT_SIZE, 0f, state * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE * 2);
            if (ability != null) {
                drawAbilityContent(graphics, mc.font, ability, sx, sy);
            }
            graphics.drawString(mc.font, KEY_LABELS[i], x + PANEL_WIDTH + 2, sy + 6, 0xAAAAAA);
        }
    }

    private static void drawAbilityContent(GuiGraphics graphics, Font font, Ability ability, int sx, int sy) {
        int cooldown = ability.cooldownSecondsRemaining();
        boolean onCooldown = cooldown > 0;

        if (AbilityIcons.hasIcon(ability)) {
            if (onCooldown) RenderSystem.setShaderColor(COOLDOWN_TINT, COOLDOWN_TINT, COOLDOWN_TINT, 1.0f);
            graphics.blit(ability.iconLocation(), sx + ICON_OFFSET, sy + ICON_OFFSET, ICON_SIZE, ICON_SIZE,
                    0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            if (onCooldown) RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            graphics.drawCenteredString(font, ability.shortLabel(), sx + SLOT_SIZE / 2, sy + 6,
                    onCooldown ? COOLDOWN_TEXT_COLOR : NORMAL_TEXT_COLOR);
        }

        if (onCooldown) {
            graphics.drawCenteredString(font, Integer.toString(cooldown), sx + SLOT_SIZE / 2, sy + 6, COOLDOWN_NUMBER_COLOR);
        }
    }

    private static float advanceSlide() {
        if (!slideInitialized) {
            slideProgress = AbilitySlots.hudHidden() ? 0f : 1f;
            slideInitialized = true;
        }

        long now = System.nanoTime();
        float dt = lastSlideNanos == 0L ? 0f : (now - lastSlideNanos) / 1_000_000_000f;
        lastSlideNanos = now;
        dt = Math.min(dt, 0.1f);

        float target = AbilitySlots.hudHidden() ? 0f : 1f;
        float rate = 1f / SLIDE_SECONDS;
        if (slideProgress < target) slideProgress = Math.min(target, slideProgress + rate * dt);
        else if (slideProgress > target) slideProgress = Math.max(target, slideProgress - rate * dt);
        return slideProgress;
    }
}
