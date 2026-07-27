package net.noiilive.hahueuh.client.gui;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

public abstract class BookPageScreen extends Screen {
    protected static final int PANEL_W = 280;
    protected static final int PANEL_H = 180;

    protected static final int TEXT_COLOR = 0xFF825738;
    protected static final int TEXT_SHADOW_COLOR = 0xFFD9C199;

    private static final ResourceLocation BOOK_TAB =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/book_tab.png");
    private static final int TAB_W = 32;
    private static final int TAB_H = 16;
    private static final int TAB_X = 269;
    private static final int[] TAB_Y = {16, 35, 54, 73, 92};

    protected int left;
    protected int top;

    protected BookPageScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
    }

    protected enum Align { LEFT, CENTER, RIGHT }

    protected void drawAligned(GuiGraphics graphics, Component text, double x, double bottomY, Align align) {
        drawAligned(graphics, text, x, bottomY, align, TEXT_COLOR, TEXT_SHADOW_COLOR);
    }

    protected void drawAligned(GuiGraphics graphics, Component text, double x, double bottomY, Align align,
                               int color, int shadowColor) {
        int width = font.width(text);
        int drawX = switch (align) {
            case LEFT -> (int) Math.round(x);
            case CENTER -> (int) Math.round(x - width / 2.0);
            case RIGHT -> (int) Math.round(x) - width;
        };
        int drawY = (int) Math.round(bottomY) - (font.lineHeight - 3);
        graphics.drawString(font, text, drawX + 1, drawY + 1, shadowColor, false);
        graphics.drawString(font, text, drawX, drawY, color, false);
    }

    protected void renderBookTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        BookTab hovered = bookTabAt(mouseX, mouseY);
        for (BookTab tab : BookTab.values()) {
            int x = left + TAB_X;
            int y = top + TAB_Y[tab.ordinal()];
            graphics.blit(BOOK_TAB, x, y, TAB_W, TAB_H,
                    0f, tab == hovered ? TAB_H : 0f, TAB_W, TAB_H, TAB_W, TAB_H * 2);
        }
    }

    protected void renderBookTabTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        BookTab hovered = bookTabAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderTooltip(font, Component.translatable(hovered.translationKey), mouseX, mouseY);
        }
    }

    @Nullable
    protected BookTab bookTabAt(double mouseX, double mouseY) {
        for (BookTab tab : BookTab.values()) {
            if (isHovering(mouseX, mouseY, left + TAB_X, top + TAB_Y[tab.ordinal()], TAB_W, TAB_H)) {
                return tab;
            }
        }
        return null;
    }

    protected boolean handleBookTabClick(double mouseX, double mouseY) {
        BookTab tab = bookTabAt(mouseX, mouseY);
        if (tab == null) return false;
        playPageTurn();
        minecraft.setScreen(tab == BookTab.INFO ? new BookOfLifeScreen() : new SkillsScreen(tab));
        return true;
    }

    protected void playPageTurn() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    protected static boolean isHovering(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
