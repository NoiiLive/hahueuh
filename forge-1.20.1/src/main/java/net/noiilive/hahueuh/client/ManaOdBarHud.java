package net.noiilive.hahueuh.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.noiilive.hahueuh.BookOfLifeStats;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.network.ClientPlayerData;
import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;

public final class ManaOdBarHud {
    private static final ResourceLocation BOTTOM = tex("mana_bar_bottom");
    private static final ResourceLocation TOP = tex("mana_bar_top");
    private static final ResourceLocation OD = tex("mana_bar_od");
    private static final ResourceLocation MANA = tex("mana_bar_mana");
    private static final ResourceLocation OVERCHARGE = tex("mana_bar_overcharge");
    private static final ResourceLocation STRAIN = tex("mana_bar_strain");
    private static final ResourceLocation HEAT = tex("mana_bar_heat");

    private static final ResourceLocation GATE_PARTIAL = tex("gate_partial");
    private static final ResourceLocation GATE_OPEN = tex("gate_open");
    private static final ResourceLocation GATE_DAMAGED = tex("gate_damaged");
    private static final ResourceLocation GATE_DESTROYED = tex("gate_destroyed");
    private static final ResourceLocation GATE_DEFECTIVE_NO_ABSORB = tex("gate_defective_noabsorb");
    private static final ResourceLocation GATE_DEFECTIVE_NO_RELEASE = tex("gate_defective_norelease");

    private static final int PANEL_W = 128;
    private static final int PANEL_H = 42;
    private static final int PANEL_MARGIN_X = 3;
    private static final int PANEL_MARGIN_Y = 3;

    private static final int RESERVOIR_X = 35, RESERVOIR_Y = 10, RESERVOIR_W = 88, RESERVOIR_H = 7;
    private static final int STRAIN_X = 35, STRAIN_Y = 21, STRAIN_W = 73, STRAIN_H = 5;
    private static final int HEAT_X = 35, HEAT_Y = 30, HEAT_W = 59, HEAT_H = 3;
    private static final int GATE_X = 6, GATE_Y = 6, GATE_SIZE = 30;

    private static final int MAX_STRAIN = 100;

    private ManaOdBarHud() {}

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(HahUeuh.MODID, "textures/gui/" + name + ".png");
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;

        PlayerData data = ClientPlayerData.of(player);

        int panelX = PANEL_MARGIN_X;
        int panelY = PANEL_MARGIN_Y;

        graphics.blit(BOTTOM, panelX, panelY, PANEL_W, PANEL_H, 0f, 0f, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        int max = BookOfLifeStats.maxMana(data);
        if (max > 0) {
            int odCurrent = data.getOdCurrent();
            int manaCurrent = data.getManaCurrent();

            float odFraction = fraction(odCurrent, max);
            float manaFraction = fraction(Math.min(manaCurrent, max), max);
            float overchargeFraction = fraction(Math.max(0, manaCurrent - max), max);

            int rx = panelX + RESERVOIR_X;
            int ry = panelY + RESERVOIR_Y;
            blitFillLeft(graphics, OD, rx, ry, RESERVOIR_W, RESERVOIR_H, odFraction);
            blitFillLeft(graphics, MANA, rx, ry, RESERVOIR_W, RESERVOIR_H, manaFraction);
            blitFillLeft(graphics, OVERCHARGE, rx, ry, RESERVOIR_W, RESERVOIR_H, overchargeFraction);

            float heatFraction = fraction(data.getSpellHeat(), max);
            blitFillLeft(graphics, HEAT, panelX + HEAT_X, panelY + HEAT_Y, HEAT_W, HEAT_H, heatFraction);
        }

        float strainFraction = fraction(data.getGateStrain(), MAX_STRAIN);
        blitFillLeft(graphics, STRAIN, panelX + STRAIN_X, panelY + STRAIN_Y, STRAIN_W, STRAIN_H, strainFraction);

        graphics.blit(TOP, panelX, panelY, PANEL_W, PANEL_H, 0f, 0f, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        ResourceLocation gate = gateTexture(data);
        graphics.blit(gate, panelX + GATE_X, panelY + GATE_Y, GATE_SIZE, GATE_SIZE,
                0f, 0f, GATE_SIZE, GATE_SIZE, GATE_SIZE, GATE_SIZE);
    }

    private static ResourceLocation gateTexture(PlayerData data) {
        GateStatus status = data.getGateStatus();
        return switch (status) {
            case OPEN -> GATE_OPEN;
            case PARTLY_OPEN -> GATE_PARTIAL;
            case DAMAGED -> GATE_DAMAGED;
            case DESTROYED -> GATE_DESTROYED;
            case DEFECTIVE -> GateDefectiveVariant.byOrdinal(data.getGateDefectiveVariant())
                    == GateDefectiveVariant.NO_RELEASE ? GATE_DEFECTIVE_NO_RELEASE : GATE_DEFECTIVE_NO_ABSORB;
        };
    }

    private static float fraction(int amount, int max) {
        return max <= 0 ? 0f : Mth.clamp(amount / (float) max, 0f, 1f);
    }

    private static void blitFillLeft(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                                     int width, int height, float fraction) {
        int fillWidth = Math.round(width * fraction);
        if (fillWidth <= 0) return;
        graphics.blit(texture, x, y, fillWidth, height, 0f, 0f, fillWidth, height, width, height);
    }
}
