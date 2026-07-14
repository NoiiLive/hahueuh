package net.noiilive.hahueuh.client.gui;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.menu.BookOfWisdomBindMenu;
import net.noiilive.hahueuh.network.BindVisionAbilityPayload;
import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BookOfWisdomBindScreen extends AbstractContainerScreen<BookOfWisdomBindMenu> {
    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/" + name + ".png");
    }

    private static final ResourceLocation BG = tex("book_of_wisdom_bind_bg");
    private static final ResourceLocation ABILITY_BUTTON = tex("book_of_wisdom_ability_button");

    private static final int BTN_X = 20;
    private static final int BTN_Y = 44;
    private static final int BTN_W = 136;
    private static final int BTN_H = 18;
    private static final int BTN_SPACING = 20;
    private static final int TEXT_MARGIN = 8;

    private static final int HEADER_COLOR = 0xFF373737;
    private static final int BODY_COLOR = 0xFF6B614C;

    public BookOfWisdomBindScreen(BookOfWisdomBindMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0.0f, 0.0f, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderPage(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    private void renderPage(GuiGraphics g, int mouseX, int mouseY) {
        boolean hasBook = !menu.inputSlot().isEmpty();
        if (!hasBook) {
            drawWrapped(g, Component.translatable("hahueuh.gui.book_of_wisdom.prompt"),
                    leftPos + TEXT_MARGIN, topPos + BTN_Y, imageWidth - TEXT_MARGIN * 2, BODY_COLOR);
            return;
        }

        BoundVisionAbility[] abilities = BoundVisionAbility.values();
        for (int i = 0; i < abilities.length; i++) {
            int bx = leftPos + BTN_X;
            int by = topPos + BTN_Y + i * BTN_SPACING;
            boolean hovered = mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;

            int state = hovered ? 1 : 0;
            g.blit(ABILITY_BUTTON, bx, by, BTN_W, BTN_H, 0.0f, state * BTN_H, BTN_W, BTN_H, BTN_W, BTN_H * 2);
            Component label = Component.translatable(abilities[i].translationKey);
            String clipped = font.substrByWidth(label, BTN_W - 8).getString();
            g.drawCenteredString(font, clipped, bx + BTN_W / 2, by + (BTN_H - 8) / 2, 0xFFFFFFFF);
        }
    }

    private void drawWrapped(GuiGraphics g, FormattedText text, int x, int y, int lineWidth, int color) {
        for (var line : font.split(text, lineWidth)) {
            g.drawString(font, line, x, y, color, false);
            y += 9;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !menu.inputSlot().isEmpty()) {
            BoundVisionAbility[] abilities = BoundVisionAbility.values();
            for (int i = 0; i < abilities.length; i++) {
                int bx = leftPos + BTN_X;
                int by = topPos + BTN_Y + i * BTN_SPACING;
                if (mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H) {
                    PacketDistributor.sendToServer(new BindVisionAbilityPayload(abilities[i].ordinal()));
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component clippedTitle = Component.literal(font.substrByWidth(title, imageWidth - TEXT_MARGIN * 2).getString());
        g.drawString(font, clippedTitle, titleLabelX, titleLabelY, HEADER_COLOR, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HEADER_COLOR, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
