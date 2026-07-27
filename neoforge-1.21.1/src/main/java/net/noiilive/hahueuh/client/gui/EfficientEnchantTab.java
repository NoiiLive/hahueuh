package net.noiilive.hahueuh.client.gui;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class EfficientEnchantTab extends Button {
    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/efficient_enchant_tab.png");
    private static final int TEX_W = 28;
    private static final int TEX_H = 28;

    public EfficientEnchantTab(int x, int y, int w, int h, OnPress onPress) {
        super(x, y, w, h, Component.translatable("hahueuh.gui.efficient_enchanting.tab"), onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.translatable("hahueuh.gui.efficient_enchanting.tab")));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int state = isHoveredOrFocused() ? 1 : 0;
        g.blit(TAB, getX(), getY(), getWidth(), getHeight(), 0.0f, state * TEX_H, TEX_W, TEX_H, TEX_W, TEX_H * 2);
    }
}
