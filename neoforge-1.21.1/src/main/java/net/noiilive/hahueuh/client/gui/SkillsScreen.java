package net.noiilive.hahueuh.client.gui;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.api.Ability;
import net.noiilive.hahueuh.api.AbilityRegistry;
import net.noiilive.hahueuh.api.Authority;
import net.noiilive.hahueuh.api.AuthorityRegistry;
import net.noiilive.hahueuh.api.OwnershipState;
import net.noiilive.hahueuh.client.AbilitySlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public final class SkillsScreen extends BookPageScreen {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/skillscreen_bg.png");
    private static final ResourceLocation SKILL_BUTTON =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/skill_button.png");
    private static final ResourceLocation SKILLBAR =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/skillbar.png");

    private static final int BAR_W = 210;
    private static final int BAR_H = 34;
    private static final int BAR_GAP = 4;
    private static final int SLOT_SIZE = 20;
    private static final int[] SLOT_X = {7, 29, 51, 73, 95, 117, 139, 161, 183};
    private static final int SLOT_Y = 7;

    private static final double LEFT_TITLE_X = 72.5;
    private static final double RIGHT_TITLE_X = 206.5;
    private static final double TITLE_Y = 25;

    private static final int BUTTON_W = 114;
    private static final int BUTTON_H = 17;
    private static final int LEFT_BUTTON_X = 16;
    private static final int RIGHT_BUTTON_X = 150;
    private static final int BUTTON_Y0 = 33;
    private static final int BUTTON_STEP = 20;
    private static final int MAX_ROWS = 6;

    private static final double BUTTON_TEXT_X = 56.5;
    private static final double BUTTON_TEXT_Y = 11;

    private static final int SELECTED_TEXT_COLOR = 0xFFED8D00;

    private final BookTab page;
    private int selectedCategory;
    private Ability selectedSkill;
    private int categoryScroll;
    private int skillScroll;

    public SkillsScreen(BookTab page) {
        super(Component.translatable(page.translationKey));
        this.page = page;
    }

    private List<Authority> categories() {
        return switch (page) {
            case AUTHORITIES -> ownedAuthorities(false);
            case MISC_SKILLS -> ownedAuthorities(true);
            case MAGIC_SKILLS -> magicCategories();
            default -> List.of();
        };
    }

    private static List<Authority> magicCategories() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return List.of();
        List<Authority> list = new ArrayList<>();
        if (net.noiilive.hahueuh.MagicSchool.canUseGeneralMagic(player)) {
            AuthorityRegistry.get(net.noiilive.hahueuh.MagicSchool.GENERAL_AUTHORITY).ifPresent(list::add);
        }
        for (net.noiilive.hahueuh.MagicSchool school : net.noiilive.hahueuh.MagicSchool.values()) {
            if (school.acquiredBy(player)) {
                AuthorityRegistry.get(school.authorityId).ifPresent(list::add);
            }
        }
        return list;
    }

    private static List<Authority> ownedAuthorities(boolean fingerOnly) {
        return OwnershipState.ownedAuthorities().stream()
                .filter(a -> a.id().equals(HahUeuhAbilities.FINGER_AUTHORITY) == fingerOnly)
                .toList();
    }

    private List<Ability> skills() {
        List<Authority> categories = categories();
        if (selectedCategory < 0 || selectedCategory >= categories.size()) return List.of();
        return AbilityRegistry.forAuthority(categories.get(selectedCategory).id()).stream()
                .filter(Ability::isAvailable).toList();
    }

    private static int buttonY(int row) {
        return BUTTON_Y0 + row * BUTTON_STEP;
    }

    private int barX() {
        return left + (PANEL_W - BAR_W) / 2;
    }

    private int barY() {
        return top + PANEL_H + BAR_GAP;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.blit(BACKGROUND, left, top, 0f, 0f, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawAligned(graphics, title, left + LEFT_TITLE_X, top + TITLE_Y, Align.CENTER);
        drawAligned(graphics, Component.translatable("hahueuh.gui.skills.available"),
                left + RIGHT_TITLE_X, top + TITLE_Y, Align.CENTER);

        List<Authority> categories = categories();
        categoryScroll = clampScroll(categoryScroll, categories.size());
        for (int row = 0; row < visibleRows(categories.size(), categoryScroll); row++) {
            int i = categoryScroll + row;
            drawSkillButton(graphics, left + LEFT_BUTTON_X, top + buttonY(row),
                    Component.translatable(categories.get(i).translationKey()), i == selectedCategory);
        }
        drawScrollIndicators(graphics, left + LEFT_BUTTON_X, categoryScroll, categories.size());

        List<Ability> skills = skills();
        skillScroll = clampScroll(skillScroll, skills.size());
        for (int row = 0; row < visibleRows(skills.size(), skillScroll); row++) {
            Ability skill = skills.get(skillScroll + row);
            drawSkillButton(graphics, left + RIGHT_BUTTON_X, top + buttonY(row),
                    Component.translatable(skill.translationKey()), skill.equals(selectedSkill));
        }
        drawScrollIndicators(graphics, left + RIGHT_BUTTON_X, skillScroll, skills.size());

        renderBookTabs(graphics, mouseX, mouseY);

        Ability hoveredSlotAbility = drawSkillbar(graphics, mouseX, mouseY);

        renderBookTabTooltip(graphics, mouseX, mouseY);
        if (hoveredSlotAbility != null) {
            graphics.renderTooltip(font, Component.translatable(hoveredSlotAbility.translationKey()), mouseX, mouseY);
        }
    }

    private Ability drawSkillbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int bx = barX();
        int by = barY();
        graphics.blit(SKILLBAR, bx, by, 0f, 0f, BAR_W, BAR_H, BAR_W, BAR_H);

        Ability hovered = null;
        for (int i = 0; i < AbilitySlots.SLOT_COUNT; i++) {
            int sx = bx + SLOT_X[i];
            int sy = by + SLOT_Y;
            Ability bound = AbilitySlots.get(i);
            if (bound == null) continue;
            boolean isHovered = isHovering(mouseX, mouseY, sx, sy, SLOT_SIZE, SLOT_SIZE);
            if (isHovered) hovered = bound;
            drawAligned(graphics, Component.literal(bound.shortLabel()), sx + SLOT_SIZE / 2.0, sy + SLOT_SIZE - 8,
                    Align.CENTER, bound.equals(selectedSkill) ? SELECTED_TEXT_COLOR : TEXT_COLOR, TEXT_SHADOW_COLOR);
        }
        return hovered;
    }

    private void drawSkillButton(GuiGraphics graphics, int x, int y, Component label, boolean selected) {
        graphics.blit(SKILL_BUTTON, x, y, BUTTON_W, BUTTON_H, 0f, 0f, BUTTON_W, BUTTON_H, BUTTON_W, BUTTON_H);
        drawAligned(graphics, label, x + BUTTON_TEXT_X, y + BUTTON_TEXT_Y, Align.CENTER,
                selected ? SELECTED_TEXT_COLOR : TEXT_COLOR, TEXT_SHADOW_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleBookTabClick(mouseX, mouseY)) return true;

        if (button == 0) {
            List<Authority> categories = categories();
            for (int row = 0; row < visibleRows(categories.size(), categoryScroll); row++) {
                if (isHovering(mouseX, mouseY, left + LEFT_BUTTON_X, top + buttonY(row), BUTTON_W, BUTTON_H)) {
                    selectedCategory = categoryScroll + row;
                    selectedSkill = null;
                    skillScroll = 0;
                    playClick();
                    return true;
                }
            }
            List<Ability> skills = skills();
            for (int row = 0; row < visibleRows(skills.size(), skillScroll); row++) {
                if (isHovering(mouseX, mouseY, left + RIGHT_BUTTON_X, top + buttonY(row), BUTTON_W, BUTTON_H)) {
                    selectedSkill = skills.get(skillScroll + row);
                    playClick();
                    return true;
                }
            }
        }

        if (button == 0 || button == 2) {
            int bx = barX();
            int by = barY();
            for (int i = 0; i < AbilitySlots.SLOT_COUNT; i++) {
                int sx = bx + SLOT_X[i];
                int sy = by + SLOT_Y;
                if (!isHovering(mouseX, mouseY, sx, sy, SLOT_SIZE, SLOT_SIZE)) continue;
                if (button == 0 && selectedSkill != null) {
                    AbilitySlots.bind(i, selectedSkill);
                    playClick();
                } else if (button == 2) {
                    AbilitySlots.unbind(i);
                    playClick();
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) -Math.signum(scrollY);
        if (step == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        if (isHovering(mouseX, mouseY, left + LEFT_BUTTON_X, top + BUTTON_Y0, BUTTON_W, MAX_ROWS * BUTTON_STEP)) {
            categoryScroll = clampScroll(categoryScroll + step, categories().size());
            return true;
        }
        if (isHovering(mouseX, mouseY, left + RIGHT_BUTTON_X, top + BUTTON_Y0, BUTTON_W, MAX_ROWS * BUTTON_STEP)) {
            skillScroll = clampScroll(skillScroll + step, skills().size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static int visibleRows(int listSize, int scroll) {
        return Math.max(0, Math.min(MAX_ROWS, listSize - scroll));
    }

    private static int clampScroll(int scroll, int listSize) {
        int max = Math.max(0, listSize - MAX_ROWS);
        return Math.max(0, Math.min(scroll, max));
    }

    private void drawScrollIndicators(GuiGraphics graphics, int columnX, int scroll, int listSize) {
        double y = top + buttonY(MAX_ROWS - 1) + BUTTON_H + 9;
        if (scroll > 0) {
            drawAligned(graphics, Component.literal("▲"), columnX + BUTTON_W * 0.25, y, Align.CENTER);
        }
        if (scroll + MAX_ROWS < listSize) {
            drawAligned(graphics, Component.literal("▼"), columnX + BUTTON_W * 0.75, y, Align.CENTER);
        }
    }

    private void playClick() {
        minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
    }
}
