package net.noiilive.hahueuh.client.gui;

import net.noiilive.hahueuh.BookOfLifeStats;
import net.noiilive.hahueuh.ConfigMagic;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModAttachments;
import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;
import net.noiilive.hahueuh.network.PlayerRace;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class BookOfLifeScreen extends BookPageScreen {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/mainscreen_bg.png");

    private static final double TITLE_X = 72.5, TITLE_Y = 25;
    private static final double NAME_X = 72.5, NAME_Y = 37;

    private static final int MODEL_X1 = 16, MODEL_Y1 = 97;
    private static final int MODEL_X2 = 52, MODEL_Y2 = 153;
    private static final int MODEL_SCALE = 24;

    private static final double INFO_X = 57;
    private static final double[] INFO_Y = {106, 117, 128, 139, 150};
    private static final int GATE_FIELD_INDEX = 2;

    public BookOfLifeScreen() {
        super(Component.translatable("hahueuh.gui.book_of_life.title"));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.blit(BACKGROUND, left, top, 0f, 0f, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawAligned(graphics, title, left + TITLE_X, top + TITLE_Y, Align.CENTER);

        LocalPlayer player = minecraft != null ? minecraft.player : null;
        Component nameText = player != null ? player.getName() : Component.empty();
        drawAligned(graphics, nameText, left + NAME_X, top + NAME_Y, Align.CENTER);

        Component[] infoFields = buildInfoFields(player);
        for (int i = 0; i < infoFields.length; i++) {
            drawAligned(graphics, infoFields[i], left + INFO_X, top + INFO_Y[i], Align.LEFT);
        }

        renderPlayerModel(graphics, mouseX, mouseY, player);
        renderBookTabs(graphics, mouseX, mouseY);

        if (player != null && isHoveringGateField(infoFields[GATE_FIELD_INDEX], mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, buildGateTooltip(player, infoFields[GATE_FIELD_INDEX]), mouseX, mouseY);
        }
        renderBookTabTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleBookTabClick(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringGateField(Component gateText, int mouseX, int mouseY) {
        int width = font.width(gateText);
        int x = (int) Math.round(left + INFO_X);
        int y = (int) Math.round(top + INFO_Y[GATE_FIELD_INDEX]) - (font.lineHeight - 3);
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + font.lineHeight;
    }

    private List<Component> buildGateTooltip(LocalPlayer player, Component gateText) {
        int output = Math.max(0, player.getData(ModAttachments.PLAYER_GATE_OUTPUT.get()));
        int efficiency = Math.max(0, player.getData(ModAttachments.PLAYER_GATE_EFFICIENCY.get()));
        int strain = player.getData(ModAttachments.PLAYER_GATE_STRAIN.get());

        int damagedThreshold = ConfigMagic.GATE_STRAIN_DAMAGED.getAsInt();
        int destroyedThreshold = ConfigMagic.GATE_STRAIN_DESTROYED.getAsInt();
        ChatFormatting strainColor = strain >= destroyedThreshold ? ChatFormatting.DARK_RED
                : strain >= damagedThreshold ? ChatFormatting.RED
                : strain >= damagedThreshold / 2 ? ChatFormatting.YELLOW
                : ChatFormatting.GREEN;

        return List.of(
                gateText.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("hahueuh.gui.book_of_life.tooltip_output", output).withStyle(ChatFormatting.AQUA),
                Component.translatable("hahueuh.gui.book_of_life.tooltip_efficiency", efficiency).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("hahueuh.gui.book_of_life.tooltip_strain", strain).withStyle(strainColor)
        );
    }

    private Component[] buildInfoFields(LocalPlayer player) {
        if (player == null) {
            return new Component[]{Component.empty(), Component.empty(), Component.empty(), Component.empty(), Component.empty()};
        }
        PlayerRace race = player.getData(ModAttachments.PLAYER_RACE.get());
        int age = player.getData(ModAttachments.PLAYER_AGE.get());
        GateStatus gate = player.getData(ModAttachments.PLAYER_GATE_STATUS.get());
        int manaCurrent = player.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        int manaMax = BookOfLifeStats.maxMana(player);
        int odCurrent = player.getData(ModAttachments.PLAYER_OD_CURRENT.get());
        int odMax = BookOfLifeStats.maxOd(player);
        return new Component[]{
                Component.translatable("hahueuh.gui.book_of_life.field_race", Component.translatable(race.translationKey)),
                Component.translatable("hahueuh.gui.book_of_life.field_age", age),
                gateFieldText(gate, player),
                Component.translatable("hahueuh.gui.book_of_life.field_mana", manaCurrent, manaMax),
                Component.translatable("hahueuh.gui.book_of_life.field_od", odCurrent, odMax),
        };
    }

    private static Component gateFieldText(GateStatus gate, LocalPlayer player) {
        if (gate != GateStatus.DEFECTIVE) {
            return Component.translatable("hahueuh.gui.book_of_life.field_gate", Component.translatable(gate.translationKey));
        }
        GateDefectiveVariant variant = GateDefectiveVariant.byOrdinal(
                player.getData(ModAttachments.PLAYER_GATE_DEFECTIVE_VARIANT.get()));
        return Component.translatable("hahueuh.gui.book_of_life.field_gate_defective",
                Component.translatable(gate.translationKey), Component.translatable(variant.translationKey));
    }

    private void renderPlayerModel(GuiGraphics graphics, int mouseX, int mouseY, LocalPlayer player) {
        if (player == null) return;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                left + MODEL_X1, top + MODEL_Y1, left + MODEL_X2, top + MODEL_Y2,
                MODEL_SCALE, 0.0625f, mouseX, mouseY, player);
    }

}
