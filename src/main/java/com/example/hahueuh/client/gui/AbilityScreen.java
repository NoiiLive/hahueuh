package com.example.hahueuh.client.gui;

import com.example.hahueuh.HahUeuh;
import com.example.hahueuh.api.Ability;
import com.example.hahueuh.api.AbilityRegistry;
import com.example.hahueuh.api.Authority;
import com.example.hahueuh.api.OwnershipState;
import com.example.hahueuh.client.AbilitySlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class AbilityScreen extends Screen {
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/ability_screen.png");
    private static final ResourceLocation LIST_BUTTON = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/list_button.png");
    private static final ResourceLocation SLOT = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/slot.png");

    private static final int PANEL_WIDTH = 246;
    private static final int PANEL_HEIGHT = 200;
    private static final int COLUMN_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 2;
    private static final int LIST_TOP_OFFSET = 26;

    private int left;
    private int top;
    private Authority selectedAuthority;
    private Ability selectedAbility;

    public AbilityScreen() {
        super(Component.translatable("hahueuh.gui.ability_screen.title"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        List<Authority> owned = OwnershipState.ownedAuthorities();
        if (selectedAuthority != null && !owned.contains(selectedAuthority)) selectedAuthority = null;
        if (selectedAuthority == null && !owned.isEmpty()) selectedAuthority = owned.get(0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.blit(PANEL, left, top, PANEL_WIDTH, PANEL_HEIGHT, 0f, 0f, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 8, 0xFFFFFF);

        List<Authority> owned = OwnershipState.ownedAuthorities();
        if (owned.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("hahueuh.message.no_authorities"),
                    left + PANEL_WIDTH / 2, top + PANEL_HEIGHT / 2, 0xFF5555);
        }

        int leftColumnX = left + 10;
        int rightColumnX = left + PANEL_WIDTH - 10 - COLUMN_WIDTH;
        int listY = top + LIST_TOP_OFFSET;

        for (int i = 0; i < owned.size(); i++) {
            Authority authority = owned.get(i);
            int by = listY + i * (BUTTON_HEIGHT + BUTTON_GAP);
            int state = authority == selectedAuthority ? 2 : (isHovering(mouseX, mouseY, leftColumnX, by, COLUMN_WIDTH, BUTTON_HEIGHT) ? 1 : 0);
            drawListButton(graphics, leftColumnX, by, state);
            graphics.drawCenteredString(font, Component.translatable(authority.translationKey()),
                    leftColumnX + COLUMN_WIDTH / 2, by + 6, 0xFFFFFF);
        }

        Ability hoveredAbilityButton = null;
        if (selectedAuthority != null) {
            List<Ability> abilities = AbilityRegistry.forAuthority(selectedAuthority.id());
            for (int i = 0; i < abilities.size(); i++) {
                Ability ability = abilities.get(i);
                int by = listY + i * (BUTTON_HEIGHT + BUTTON_GAP);
                boolean hovered = isHovering(mouseX, mouseY, rightColumnX, by, COLUMN_WIDTH, BUTTON_HEIGHT);
                int state = ability == selectedAbility ? 2 : (hovered ? 1 : 0);
                drawListButton(graphics, rightColumnX, by, state);
                graphics.drawCenteredString(font, Component.translatable(ability.translationKey()),
                        rightColumnX + COLUMN_WIDTH / 2, by + 6, 0xFFFFFF);
                if (hovered) hoveredAbilityButton = ability;
            }
        }

        int totalSlotsWidth = AbilitySlots.SLOT_COUNT * SLOT_SIZE + (AbilitySlots.SLOT_COUNT - 1) * SLOT_GAP;
        int slotsX0 = left + (PANEL_WIDTH - totalSlotsWidth) / 2;
        int slotsY = top + PANEL_HEIGHT - 10 - SLOT_SIZE;
        Ability hoveredSlotAbility = null;
        for (int i = 0; i < AbilitySlots.SLOT_COUNT; i++) {
            int sx = slotsX0 + i * (SLOT_SIZE + SLOT_GAP);
            Ability bound = AbilitySlots.get(i);
            boolean hovered = isHovering(mouseX, mouseY, sx, slotsY, SLOT_SIZE, SLOT_SIZE);
            int state = (bound != null && bound == selectedAbility) ? 2 : (hovered ? 1 : 0);
            drawSlot(graphics, sx, slotsY, state);
            if (bound != null) {
                graphics.drawCenteredString(font, bound.shortLabel(), sx + SLOT_SIZE / 2, slotsY + 6, 0xFFFFFF);
            }
            if (hovered && bound != null) hoveredSlotAbility = bound;
        }

        if (hoveredSlotAbility != null) {
            graphics.renderTooltip(font, Component.translatable(hoveredSlotAbility.translationKey()), mouseX, mouseY);
        } else if (hoveredAbilityButton != null) {
            graphics.renderTooltip(font, Component.translatable(hoveredAbilityButton.translationKey()), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Authority> owned = OwnershipState.ownedAuthorities();
        int leftColumnX = left + 10;
        int rightColumnX = left + PANEL_WIDTH - 10 - COLUMN_WIDTH;
        int listY = top + LIST_TOP_OFFSET;

        if (button == 0) {
            for (int i = 0; i < owned.size(); i++) {
                int by = listY + i * (BUTTON_HEIGHT + BUTTON_GAP);
                if (isHovering(mouseX, mouseY, leftColumnX, by, COLUMN_WIDTH, BUTTON_HEIGHT)) {
                    selectedAuthority = owned.get(i);
                    selectedAbility = null;
                    return true;
                }
            }

            if (selectedAuthority != null) {
                List<Ability> abilities = AbilityRegistry.forAuthority(selectedAuthority.id());
                for (int i = 0; i < abilities.size(); i++) {
                    int by = listY + i * (BUTTON_HEIGHT + BUTTON_GAP);
                    if (isHovering(mouseX, mouseY, rightColumnX, by, COLUMN_WIDTH, BUTTON_HEIGHT)) {
                        selectedAbility = abilities.get(i);
                        return true;
                    }
                }
            }
        }

        int totalSlotsWidth = AbilitySlots.SLOT_COUNT * SLOT_SIZE + (AbilitySlots.SLOT_COUNT - 1) * SLOT_GAP;
        int slotsX0 = left + (PANEL_WIDTH - totalSlotsWidth) / 2;
        int slotsY = top + PANEL_HEIGHT - 10 - SLOT_SIZE;
        for (int i = 0; i < AbilitySlots.SLOT_COUNT; i++) {
            int sx = slotsX0 + i * (SLOT_SIZE + SLOT_GAP);
            if (isHovering(mouseX, mouseY, sx, slotsY, SLOT_SIZE, SLOT_SIZE)) {
                if (button == 0 && selectedAbility != null) {
                    AbilitySlots.bind(i, selectedAbility);
                } else if (button == 2) {
                    AbilitySlots.unbind(i);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawListButton(GuiGraphics graphics, int x, int y, int state) {
        graphics.blit(LIST_BUTTON, x, y, COLUMN_WIDTH, BUTTON_HEIGHT, 0f, state * BUTTON_HEIGHT,
                COLUMN_WIDTH, BUTTON_HEIGHT, COLUMN_WIDTH, BUTTON_HEIGHT * 3);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, int state) {
        graphics.blit(SLOT, x, y, SLOT_SIZE, SLOT_SIZE, 0f, state * SLOT_SIZE,
                SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE * 3);
    }

    private static boolean isHovering(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
