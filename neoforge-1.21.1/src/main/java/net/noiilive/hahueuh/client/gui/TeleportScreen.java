package net.noiilive.hahueuh.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.hahueuh.network.TeleportCastPayload;

public final class TeleportScreen extends Screen {
    private static final int FIELD_W = 60;
    private static final int FIELD_H = 18;
    private static final int FIELD_GAP = 6;
    private static final int BUTTON_W = 92;
    private static final int BUTTON_H = 20;

    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    public TeleportScreen() {
        super(Component.translatable("hahueuh.gui.teleport.title"));
    }

    @Override
    protected void init() {
        int totalW = FIELD_W * 3 + FIELD_GAP * 2;
        int x = (width - totalW) / 2;
        int y = height / 2 - 24;

        net.minecraft.core.BlockPos here = minecraft != null && minecraft.player != null
                ? minecraft.player.blockPosition() : net.minecraft.core.BlockPos.ZERO;

        xBox = coordinateBox(x, y, "hahueuh.gui.teleport.x", here.getX());
        yBox = coordinateBox(x + FIELD_W + FIELD_GAP, y, "hahueuh.gui.teleport.y", here.getY());
        zBox = coordinateBox(x + (FIELD_W + FIELD_GAP) * 2, y, "hahueuh.gui.teleport.z", here.getZ());
        addRenderableWidget(xBox);
        addRenderableWidget(yBox);
        addRenderableWidget(zBox);
        setInitialFocus(xBox);

        int buttonY = y + FIELD_H + 12;
        int buttonX = (width - (BUTTON_W * 2 + FIELD_GAP)) / 2;
        addRenderableWidget(Button.builder(Component.translatable("hahueuh.gui.teleport.portal"),
                        b -> submit(true))
                .bounds(buttonX, buttonY, BUTTON_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("hahueuh.gui.teleport.self"),
                        b -> submit(false))
                .bounds(buttonX + BUTTON_W + FIELD_GAP, buttonY, BUTTON_W, BUTTON_H).build());
    }

    private EditBox coordinateBox(int x, int y, String hintKey, int current) {
        EditBox box = new EditBox(font, x, y, FIELD_W, FIELD_H, Component.translatable(hintKey));
        box.setMaxLength(12);
        box.setHint(Component.translatable(hintKey));
        box.setFilter(TeleportScreen::isCoordinateInput);
        box.setValue(Integer.toString(current));
        return box;
    }

    private static boolean isCoordinateInput(String value) {
        return value.matches("-?\\d{0,9}");
    }

    private void submit(boolean portal) {
        Integer x = parse(xBox.getValue());
        Integer y = parse(yBox.getValue());
        Integer z = parse(zBox.getValue());
        if (x == null || y == null || z == null) return;
        PacketDistributor.sendToServer(new TeleportCastPayload(x, y, z, portal));
        onClose();
    }

    private static Integer parse(String raw) {
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 48, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("hahueuh.gui.teleport.hint"),
                width / 2, height / 2 - 36, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
