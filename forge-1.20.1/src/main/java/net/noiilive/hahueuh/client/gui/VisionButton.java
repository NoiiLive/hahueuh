package net.noiilive.hahueuh.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

public final class VisionButton extends Button {
    private final ResourceLocation texture;
    private final int texW;
    private final int texH;
    private final BooleanSupplier activeState;

    VisionButton(int x, int y, int w, int h, Component label, ResourceLocation texture,
                 int texW, int texH, BooleanSupplier activeState, OnPress onPress) {
        super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.texW = texW;
        this.texH = texH;
        this.activeState = activeState;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean useActive = (activeState != null && activeState.getAsBoolean()) || isHoveredOrFocused();
        int state = useActive ? 1 : 0;
        g.blit(texture, getX(), getY(), getWidth(), getHeight(), 0.0f, state * texH, texW, texH, texW, texH * 2);
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);
    }
}
