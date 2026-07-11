package net.noiilive.hahueuh.client.gui;

import net.noiilive.hahueuh.BurdenMath;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.AllyBurdenUpdatePayload;
import net.noiilive.hahueuh.network.AllyDataPayload;
import net.noiilive.hahueuh.network.AllyTrackerData;
import net.noiilive.hahueuh.network.AllyTrackerRefreshPayload;
import net.noiilive.hahueuh.network.AllyType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AllyTrackerScreen extends Screen {
    private static final ResourceLocation MARKER = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/ally_marker.png");
    private static final ResourceLocation EVEN_OUT_BUTTON = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/even_out_button.png");
    private static final ResourceLocation INFO_BOX = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/ally_info_box.png");
    private static final ResourceLocation SLIDER_TRACK = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/ally_slider_track.png");
    private static final ResourceLocation SLIDER_KNOB = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/ally_slider_knob.png");

    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final double PIXELS_PER_BLOCK = 3.0;
    private static final int RING_FALLBACK_RADIUS = 90;
    private static final int SELF_EDGE_MARGIN = 16;
    private static final int MARKER_FRAME = 16;
    private static final int MARKER_HIT_RADIUS = 9;
    private static final float SELF_MARKER_SCALE = 1.5f;
    private static final int SELF_HIT_RADIUS = Math.round(MARKER_HIT_RADIUS * SELF_MARKER_SCALE);

    private static final int BOX_W = 180;
    private static final int BOX_H = 150;
    private static final int EVEN_OUT_W = 140;
    private static final int EVEN_OUT_H = 20;
    private static final int SLIDER_W = 160;
    private static final int SLIDER_H = 10;

    private static final long FADE_SELF_MS = 150;
    private static final long FADE_UI_MS = 250;
    private static final long FADE_DOT_BASE_DELAY_MS = 120;
    private static final long FADE_DOT_SWEEP_MS = 650;
    private static final long FADE_DOT_OWN_MS = 150;
    private static final double FADE_SWEEP_NORMALIZE_PX = 260.0;

    private static final long SUPPRESS_SYNC_MS = 450;

    private int dataVersion = -1;
    private List<AllyDataPayload.Ally> allies = List.of();
    private double[] weights = new double[0]; // aligned to `allies`
    private double selfWeight = 100.0;
    private float selfHealth = 20.0f;
    private float selfMaxHealth = 20.0f;
    private List<AllyDataPayload.Effect> selfEffects = List.of();
    private final Map<UUID, Float> ringAngle = new HashMap<>();

    private UUID selectedAlly;
    private boolean selfSelected;
    private int draggingParticipant = -1; // -1 none, 0 self, i+1 = allies[i]
    private boolean panning;
    private double panX;
    private double panY;
    private int refreshTimer;
    private long openAtMillis;
    private long suppressSyncUntilMillis;

    public AllyTrackerScreen() {
        super(Component.translatable("hahueuh.gui.ally_tracker.title"));
    }

    @Override
    protected void init() {
        openAtMillis = System.currentTimeMillis();
        syncFromData(true);
        PacketDistributor.sendToServer(AllyTrackerRefreshPayload.INSTANCE);
    }

    @Override
    public void tick() {
        if (++refreshTimer >= REFRESH_INTERVAL_TICKS) {
            refreshTimer = 0;
            PacketDistributor.sendToServer(AllyTrackerRefreshPayload.INSTANCE);
        }
        syncFromData(false);
    }

    private void syncFromData(boolean force) {
        if (!force && AllyTrackerData.version() == dataVersion) return;
        dataVersion = AllyTrackerData.version();
        AllyDataPayload data = AllyTrackerData.latest();

        boolean suppressed = !force && (draggingParticipant >= 0 || System.currentTimeMillis() < suppressSyncUntilMillis)
                && data.allies().size() == allies.size();
        double[] preservedWeights = weights;
        double preservedSelfWeight = selfWeight;

        allies = data.allies();
        weights = new double[allies.size()];
        for (int i = 0; i < allies.size(); i++) weights[i] = allies.get(i).weight();
        selfWeight = data.selfWeight();
        selfHealth = data.selfHealth();
        selfMaxHealth = data.selfMaxHealth();
        selfEffects = data.selfEffects();

        if (suppressed) {
            System.arraycopy(preservedWeights, 0, weights, 0, weights.length);
            selfWeight = preservedSelfWeight;
        }

        for (AllyDataPayload.Ally ally : allies) {
            ringAngle.computeIfAbsent(ally.uuid(), uuid -> {
                java.util.Random rng = new java.util.Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
                return rng.nextFloat() * (float) (Math.PI * 2);
            });
        }
        if (selectedAlly != null && allies.stream().noneMatch(a -> a.uuid().equals(selectedAlly))) {
            selectedAlly = null;
        }
    }

    private int selfCenterX() {
        return width / 2 + (int) Math.round(panX);
    }

    private int selfCenterY() {
        return height / 2 + (int) Math.round(panY);
    }

    private int[] clampToScreen(int x, int y) {
        int minY = 26;
        int maxY = height - 40;
        int cxClamped = Math.clamp(x, SELF_EDGE_MARGIN, width - SELF_EDGE_MARGIN);
        int cyClamped = Math.clamp(y, minY, maxY);
        boolean clamped = cxClamped != x || cyClamped != y;
        return new int[]{cxClamped, cyClamped, clamped ? 1 : 0};
    }

    private int[] selfScreenPos() {
        return clampToScreen(selfCenterX(), selfCenterY());
    }

    private double[] allyOffsetPx(AllyDataPayload.Ally ally) {
        if (ally.sameDimension() && ally.hasData()) {
            return new double[]{ally.dx() * PIXELS_PER_BLOCK, ally.dz() * PIXELS_PER_BLOCK};
        }
        float angle = ringAngle.getOrDefault(ally.uuid(), 0.0f);
        return new double[]{Math.cos(angle) * RING_FALLBACK_RADIUS, Math.sin(angle) * RING_FALLBACK_RADIUS};
    }

    private int[] allyScreenPos(AllyDataPayload.Ally ally) {
        double[] off = allyOffsetPx(ally);
        return clampToScreen(selfCenterX() + (int) Math.round(off[0]), selfCenterY() + (int) Math.round(off[1]));
    }

    private int boxX() {
        return width - BOX_W - 8;
    }

    private int boxY() {
        return 30;
    }

    private int infoSliderX() {
        return boxX() + 10;
    }

    private int infoSliderY() {
        return boxY() + 118;
    }

    private int evenOutX() {
        return (width - EVEN_OUT_W) / 2;
    }

    private int evenOutY() {
        return height - EVEN_OUT_H - 10;
    }

    private long elapsedMs() {
        return System.currentTimeMillis() - openAtMillis;
    }

    private static float clampedFrac(long elapsed, long durationMs) {
        if (durationMs <= 0) return 1f;
        return (float) Math.clamp(elapsed / (double) durationMs, 0.0, 1.0);
    }

    private float selfFadeAlpha() {
        return clampedFrac(elapsedMs(), FADE_SELF_MS);
    }

    private float uiFadeAlpha() {
        return clampedFrac(elapsedMs(), FADE_UI_MS);
    }

    private float dotFadeAlpha(double distancePx) {
        double frac = Math.min(1.0, distancePx / FADE_SWEEP_NORMALIZE_PX);
        long delay = FADE_DOT_BASE_DELAY_MS + (long) (frac * FADE_DOT_SWEEP_MS);
        long local = elapsedMs() - delay;
        if (local <= 0) return 0f;
        return clampedFrac(local, FADE_DOT_OWN_MS);
    }

    private static int withAlpha(int argb, float alphaFrac) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.clamp(alphaFrac, 0f, 1f));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        float uiAlpha = uiFadeAlpha();
        graphics.drawCenteredString(font, title, width / 2, 12, withAlpha(0xFFFFFFFF, uiAlpha));

        int[] self = selfScreenPos();
        boolean selfHovered = !panning && withinDot(mouseX, mouseY, self[0], self[1], SELF_HIT_RADIUS);

        AllyDataPayload.Ally hoveredAlly = null;
        for (AllyDataPayload.Ally ally : allies) {
            int[] pos = allyScreenPos(ally);
            double dist = Math.hypot(pos[0] - self[0], pos[1] - self[1]);
            float alpha = dotFadeAlpha(dist);
            if (alpha <= 0f) continue;

            boolean hovered = !panning && withinDot(mouseX, mouseY, pos[0], pos[1], MARKER_HIT_RADIUS);
            boolean selected = ally.uuid().equals(selectedAlly);
            drawMarker(graphics, pos[0], pos[1], ally.type().ordinal(), selected, alpha, !ally.online(), 1f);
            if (hovered) hoveredAlly = ally;
        }

        drawMarker(graphics, self[0], self[1], -1, selfSelected || selfHovered, selfFadeAlpha(), false, SELF_MARKER_SCALE);

        if (selfSelected) {
            renderSelfInfoBox(graphics, uiAlpha, mouseX, mouseY);
        } else if (selectedAlly != null) {
            renderAllyInfoBox(graphics, uiAlpha, mouseX, mouseY);
        }

        boolean evenOutHover = inRect(mouseX, mouseY, evenOutX(), evenOutY(), EVEN_OUT_W, EVEN_OUT_H);
        drawEvenOutButton(graphics, evenOutHover, uiAlpha);

        if (allies.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("hahueuh.gui.ally_tracker.empty"),
                    width / 2, height / 2 + 40, withAlpha(0xFF8890A0, uiAlpha));
        }

        if (hoveredAlly != null) {
            Component name = Component.literal(hoveredAlly.name());
            if (!hoveredAlly.online()) {
                name = name.copy().append(" ").append(Component.translatable(unloadedLabelKey(hoveredAlly)));
            }
            graphics.renderTooltip(font, name, mouseX, mouseY);
        } else if (selfHovered) {
            graphics.renderTooltip(font, Component.translatable("hahueuh.gui.ally_tracker.self_label"), mouseX, mouseY);
        }
    }

    private static String unloadedLabelKey(AllyDataPayload.Ally ally) {
        if (!ally.hasData()) return "hahueuh.gui.ally_tracker.no_data";
        return ally.type() == AllyType.PLAYER ? "hahueuh.gui.ally_tracker.offline" : "hahueuh.gui.ally_tracker.unloaded";
    }

    private boolean withinDot(double mouseX, double mouseY, int x, int y, int radius) {
        return Math.abs(mouseX - x) <= radius && Math.abs(mouseY - y) <= radius;
    }

    private static void beginFadedBlit(float r, float g, float b, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, alpha);
    }

    private static void endFadedBlit() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawMarker(GuiGraphics graphics, int x, int y, int typeOrdinal, boolean selected, float alpha, boolean darkTint, float scale) {
        if (alpha <= 0f) return;
        int frameIndex = (typeOrdinal < 0 ? 3 : typeOrdinal) * 2 + (selected ? 1 : 0);
        int texHeight = MARKER_FRAME * 8;
        int size = Math.round(MARKER_FRAME * scale);
        float shade = darkTint ? 0.45f : 1.0f;
        beginFadedBlit(shade, shade, shade, alpha);
        graphics.blit(MARKER, x - size / 2, y - size / 2, size, size,
                0f, frameIndex * (float) MARKER_FRAME, MARKER_FRAME, MARKER_FRAME, MARKER_FRAME, texHeight);
        endFadedBlit();
        if (selected) {
            graphics.renderOutline(x - size / 2 - 2, y - size / 2 - 2, size + 4, size + 4,
                    withAlpha(0xFFFFFFFF, alpha));
        }
    }

    private void drawEvenOutButton(GuiGraphics graphics, boolean hover, float alpha) {
        beginFadedBlit(1f, 1f, 1f, alpha);
        graphics.blit(EVEN_OUT_BUTTON, evenOutX(), evenOutY(), 0f, hover ? EVEN_OUT_H : 0f,
                EVEN_OUT_W, EVEN_OUT_H, EVEN_OUT_W, EVEN_OUT_H * 2);
        endFadedBlit();
        graphics.drawCenteredString(font, Component.translatable("hahueuh.gui.ally_tracker.reset"),
                evenOutX() + EVEN_OUT_W / 2, evenOutY() + (EVEN_OUT_H - 8) / 2, withAlpha(0xFFFFFFFF, alpha));
    }

    private void renderSelfInfoBox(GuiGraphics graphics, float uiAlpha, int mouseX, int mouseY) {
        int x = boxX();
        int y = boxY();
        beginFadedBlit(1f, 1f, 1f, uiAlpha);
        graphics.blit(INFO_BOX, x, y, 0f, 0f, BOX_W, BOX_H, BOX_W, BOX_H);
        endFadedBlit();

        int tx = x + 10;
        int ty = y + 8;
        graphics.drawString(font, Component.translatable("hahueuh.gui.ally_tracker.self_label"), tx, ty + 1, withAlpha(0xFF7FA8FF, uiAlpha));
        ty += 14;
        graphics.drawString(font, Component.translatable("hahueuh.gui.ally_tracker.health",
                String.format("%.1f", selfHealth), String.format("%.1f", selfMaxHealth)), tx, ty, withAlpha(0xFFFF6B6B, uiAlpha));
        ty += 13;
        ty = renderEffectsBlock(graphics, tx, ty, selfEffects, uiAlpha);

        renderSlider(graphics, infoSliderX(), infoSliderY(), SLIDER_W, selfWeight,
                Component.translatable("hahueuh.gui.ally_tracker.burden", Math.round(selfWeight)), uiAlpha,
                draggingParticipant == 0, mouseX, mouseY);
    }

    private void renderAllyInfoBox(GuiGraphics graphics, float uiAlpha, int mouseX, int mouseY) {
        AllyDataPayload.Ally ally = allyByUuid(selectedAlly);
        if (ally == null) return;
        int x = boxX();
        int y = boxY();
        beginFadedBlit(1f, 1f, 1f, uiAlpha);
        graphics.blit(INFO_BOX, x, y, 0f, 0f, BOX_W, BOX_H, BOX_W, BOX_H);
        endFadedBlit();

        int tx = x + 10;
        int ty = y + 8;
        graphics.drawString(font, Component.literal(ally.name()), tx, ty + 1, withAlpha(ally.type().dotColor, uiAlpha));
        ty += 14;
        if (ally.hasData()) {
            graphics.drawString(font, Component.translatable("hahueuh.gui.ally_tracker.health",
                    String.format("%.1f", ally.health()), String.format("%.1f", ally.maxHealth())), tx, ty, withAlpha(0xFFFF6B6B, uiAlpha));
            ty += 11;
            graphics.drawString(font, Component.translatable("hahueuh.gui.ally_tracker.coords",
                    (int) Math.floor(ally.x()), (int) Math.floor(ally.y()), (int) Math.floor(ally.z())), tx, ty, withAlpha(0xFF9AD1FF, uiAlpha));
            ty += 13;
            if (!ally.online()) {
                graphics.drawString(font, Component.translatable(unloadedLabelKey(ally)), tx, ty, withAlpha(0xFF808080, uiAlpha));
                ty += 11;
            }
            ty = renderEffectsBlock(graphics, tx, ty, ally.effects(), uiAlpha);
        } else {
            graphics.drawString(font, Component.translatable("hahueuh.gui.ally_tracker.no_data"), tx, ty, withAlpha(0xFF808080, uiAlpha));
        }

        int ai = allyIndex(selectedAlly);
        double w = ai >= 0 ? weights[ai] : 0.0;
        renderSlider(graphics, infoSliderX(), infoSliderY(), SLIDER_W, w,
                Component.translatable("hahueuh.gui.ally_tracker.burden", Math.round(w)), uiAlpha,
                draggingParticipant == ai + 1, mouseX, mouseY);
    }

    private int renderEffectsBlock(GuiGraphics graphics, int tx, int ty, List<AllyDataPayload.Effect> effects, float uiAlpha) {
        graphics.drawString(font, Component.translatable("hahueuh.gui.ally_tracker.effects"), tx, ty, withAlpha(0xFFCFCFCF, uiAlpha));
        ty += 11;
        if (effects.isEmpty()) {
            graphics.drawString(font, Component.translatable("hahueuh.gui.ally_tracker.no_effects"), tx + 4, ty, withAlpha(0xFF808080, uiAlpha));
            ty += 11;
        } else {
            for (AllyDataPayload.Effect effect : effects) {
                graphics.drawString(font, formatEffect(effect), tx + 4, ty, withAlpha(0xFFB9A6E8, uiAlpha));
                ty += 11;
            }
        }
        return ty;
    }

    private void renderSlider(GuiGraphics graphics, int x, int y, int w, double value, Component label, float uiAlpha,
                               boolean active, int mouseX, int mouseY) {
        graphics.drawString(font, label, x, y - 11, withAlpha(0xFFFFFFFF, uiAlpha));

        beginFadedBlit(1f, 1f, 1f, uiAlpha);
        graphics.blit(SLIDER_TRACK, x, y, w, SLIDER_H, 0f, 0f, 200, 10, 200, 10);
        endFadedBlit();

        int knobX = x + (int) (Math.clamp(value, 0.0, 100.0) / 100.0 * (w - 6));
        boolean hover = active || inRect(mouseX, mouseY, knobX - 1, y - 3, 8, SLIDER_H + 6);
        beginFadedBlit(1f, 1f, 1f, uiAlpha);
        graphics.blit(SLIDER_KNOB, knobX, y - 2, 0f, hover ? SLIDER_H + 4f : 0f, 6, SLIDER_H + 4, 6, (SLIDER_H + 4) * 2);
        endFadedBlit();
    }

    private Component formatEffect(AllyDataPayload.Effect effect) {
        ResourceLocation rl = ResourceLocation.tryParse(effect.id());
        MobEffect me = rl != null ? BuiltInRegistries.MOB_EFFECT.get(rl) : null;
        Component name = me != null ? Component.translatable(me.getDescriptionId()) : Component.literal(effect.id());
        String amp = effect.amplifier() > 0 ? " " + (effect.amplifier() + 1) : "";
        int secs = effect.duration() / 20;
        String time = String.format("%d:%02d", secs / 60, secs % 60);
        return Component.literal("").append(name).append(amp + " (" + time + ")");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (inRect(mouseX, mouseY, evenOutX(), evenOutY(), EVEN_OUT_W, EVEN_OUT_H)) {
            double[] even = BurdenMath.evenOut(allies.size() + 1);
            selfWeight = even[0];
            for (int i = 0; i < allies.size(); i++) weights[i] = even[i + 1];
            sendBurden();
            return true;
        }

        boolean boxOpen = selfSelected || selectedAlly != null;
        if (boxOpen) {
            if (overSlider(mouseX, mouseY, infoSliderX(), infoSliderY(), SLIDER_W, SLIDER_H)) {
                draggingParticipant = selfSelected ? 0 : allyIndex(selectedAlly) + 1;
                applySlider(draggingParticipant, mouseX, infoSliderX(), SLIDER_W);
                return true;
            }
            if (inRect(mouseX, mouseY, boxX(), boxY(), BOX_W, BOX_H)) {
                return true;
            }
        }

        int[] self = selfScreenPos();
        if (withinDot(mouseX, mouseY, self[0], self[1], SELF_HIT_RADIUS)) {
            selfSelected = true;
            selectedAlly = null;
            if (self[2] == 1) {
                panX = 0;
                panY = 0;
            }
            return true;
        }

        for (AllyDataPayload.Ally ally : allies) {
            int[] pos = allyScreenPos(ally);
            if (withinDot(mouseX, mouseY, pos[0], pos[1], MARKER_HIT_RADIUS)) {
                selectedAlly = ally.uuid();
                selfSelected = false;
                if (pos[2] == 1) {
                    double[] off = allyOffsetPx(ally);
                    panX = -off[0];
                    panY = -off[1];
                }
                return true;
            }
        }

        selfSelected = false;
        selectedAlly = null;
        panning = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingParticipant >= 0) {
            applySlider(draggingParticipant, mouseX, infoSliderX(), SLIDER_W);
            return true;
        }
        if (panning) {
            panX += dx;
            panY += dy;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        panning = false;
        if (draggingParticipant >= 0) {
            draggingParticipant = -1;
            sendBurden();
            suppressSyncUntilMillis = System.currentTimeMillis() + SUPPRESS_SYNC_MS;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void applySlider(int participant, double mouseX, int trackX, int trackW) {
        double frac = Math.clamp((mouseX - trackX) / trackW, 0.0, 1.0);
        double[] p = participantArray();
        double[] redistributed = BurdenMath.redistribute(p, participant, frac * 100.0);
        selfWeight = redistributed[0];
        for (int i = 0; i < allies.size(); i++) weights[i] = redistributed[i + 1];
    }

    private double[] participantArray() {
        double[] p = new double[allies.size() + 1];
        p[0] = selfWeight;
        for (int i = 0; i < allies.size(); i++) p[i + 1] = weights[i];
        return p;
    }

    private void sendBurden() {
        Map<UUID, Float> map = new LinkedHashMap<>();
        for (int i = 0; i < allies.size(); i++) map.put(allies.get(i).uuid(), (float) weights[i]);
        PacketDistributor.sendToServer(new AllyBurdenUpdatePayload((float) selfWeight, map));
    }

    private AllyDataPayload.Ally allyByUuid(UUID uuid) {
        for (AllyDataPayload.Ally ally : allies) {
            if (ally.uuid().equals(uuid)) return ally;
        }
        return null;
    }

    private int allyIndex(UUID uuid) {
        for (int i = 0; i < allies.size(); i++) {
            if (allies.get(i).uuid().equals(uuid)) return i;
        }
        return -1;
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean overSlider(double mx, double my, int x, int y, int w, int h) {
        return inRect(mx, my, x - 2, y - 3, w + 4, h + 6);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
