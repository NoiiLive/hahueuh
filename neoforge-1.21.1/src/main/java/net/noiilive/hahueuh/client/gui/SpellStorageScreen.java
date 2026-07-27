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

    public SpellStorageScreen() {
        super(Component.translatable("hahueuh.gui.spell_storage.title"));
    }

    @Override
    protected void init() {
        List<Spell> storable = new ArrayList<>();
        for (Spell spell : SpellRegistry.all()) {
            if (!spell.id().equals(Spells.AL_SHAMAK)) storable.add(spell);
        }

        int totalH = storable.size() * (BUTTON_H + BUTTON_GAP);
        int y = Math.max(40, (height - totalH) / 2);
        int x = (width - BUTTON_W) / 2;
        for (Spell spell : storable) {
            Component name = Component.translatable("hahueuh.ability." + spell.id().getPath());
            addRenderableWidget(Button.builder(name, b -> store(spell)).bounds(x, y, BUTTON_W, BUTTON_H).build());
            y += BUTTON_H + BUTTON_GAP;
        }
    }

    private void store(Spell spell) {
        PacketDistributor.sendToServer(new StoreSpellPayload(spell.id()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
