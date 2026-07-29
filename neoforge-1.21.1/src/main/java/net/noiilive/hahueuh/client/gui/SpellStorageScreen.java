package net.noiilive.hahueuh.client.gui;

import net.noiilive.hahueuh.magic.Spells;
import net.noiilive.hahueuh.magic.Spell;
import net.noiilive.hahueuh.magic.SpellRegistry;
import net.noiilive.hahueuh.network.StoreSpellPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class SpellStorageScreen extends Screen {
    private static final int BUTTON_W = 180;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_GAP = 4;
    private static final int ROW_STEP = BUTTON_H + BUTTON_GAP;
    private static final int TOP_MARGIN = 40;
    private static final int BOTTOM_MARGIN = 16;

    private static final int SCROLLBAR_W = 4;
    private static final int SCROLLBAR_GAP = 6;
    private static final int TRACK_COLOR = 0x60000000;
    private static final int THUMB_COLOR = 0xFFAAAAAA;

    private final List<Spell> storable = new ArrayList<>();
    private int scroll;
    private int visibleRows;
    private int listX;
    private int listY;

    public SpellStorageScreen() {
        super(Component.translatable("hahueuh.gui.spell_storage.title"));
    }

    @Override
    protected void init() {
        storable.clear();
        for (Spell spell : SpellRegistry.all()) {
            if (!spell.id().equals(Spells.AL_SHAMAK)) storable.add(spell);
        }

        int room = Math.max(ROW_STEP, height - TOP_MARGIN - BOTTOM_MARGIN);
        visibleRows = Math.max(1, Math.min(storable.size(), room / ROW_STEP));
        listX = (width - BUTTON_W) / 2;
        listY = Math.max(TOP_MARGIN, (height - visibleRows * ROW_STEP) / 2);
        scroll = clampScroll(scroll);

        refreshRows();
    }

    private void refreshRows() {
        clearWidgets();
        for (int row = 0; row < visibleRows; row++) {
            int index = scroll + row;
            if (index >= storable.size()) break;
            Spell spell = storable.get(index);
            Component name = Component.translatable("hahueuh.ability." + spell.id().getPath());
            addRenderableWidget(Button.builder(name, b -> store(spell))
                    .bounds(listX, listY + row * ROW_STEP, BUTTON_W, BUTTON_H)
                    .build());
        }
    }

    private int maxScroll() {
        return Math.max(0, storable.size() - visibleRows);
    }

    private int clampScroll(int value) {
        return Math.max(0, Math.min(value, maxScroll()));
    }

    private void store(Spell spell) {
        PacketDistributor.sendToServer(new StoreSpellPayload(spell.id()));
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) -Math.signum(scrollY);
        if (step != 0 && maxScroll() > 0) {
            int next = clampScroll(scroll + step);
            if (next != scroll) {
                scroll = next;
                refreshRows();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int max = maxScroll();
        if (max <= 0) return;

        int trackX = listX + BUTTON_W + SCROLLBAR_GAP;
        int trackTop = listY;
        int trackHeight = visibleRows * ROW_STEP - BUTTON_GAP;
        graphics.fill(trackX, trackTop, trackX + SCROLLBAR_W, trackTop + trackHeight, TRACK_COLOR);

        int thumbHeight = Math.max(12, trackHeight * visibleRows / storable.size());
        int thumbTop = trackTop + (trackHeight - thumbHeight) * scroll / max;
        graphics.fill(trackX, thumbTop, trackX + SCROLLBAR_W, thumbTop + thumbHeight, THUMB_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
