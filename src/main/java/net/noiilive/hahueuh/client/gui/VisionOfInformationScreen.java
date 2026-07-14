package net.noiilive.hahueuh.client.gui;

import com.mojang.authlib.GameProfile;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.VisionInfoClientData;
import net.noiilive.hahueuh.network.VisionInfoQueryPayload;
import net.noiilive.hahueuh.network.VisionInfoResultPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class VisionOfInformationScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 234;
    private static final int SLOT = 18;

    private static final int CONTENT_X = 6;
    private static final int CONTENT_Y = 52;
    private static final int CONTENT_W = 328;
    private static final int CONTENT_H = 176;

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/" + name + ".png");
    }

    private static final ResourceLocation BACKGROUND = tex("vision_info_bg");
    private static final ResourceLocation PAGE_OVERVIEW = tex("vision_info_page_overview");
    private static final ResourceLocation PAGE_ITEMS = tex("vision_info_page_items");
    private static final ResourceLocation PAGE_AUTHORITIES = tex("vision_info_page_authorities");
    private static final ResourceLocation PAGE_DROPS = tex("vision_info_page_drops");
    private static final ResourceLocation BUTTON_TEX = tex("vision_info_button");
    private static final ResourceLocation TAB_TEX = tex("vision_info_tab");

    private int left, top;
    private EditBox searchBox;

    private int dataVersion = -2;
    private VisionInfoResultPayload result;
    private int page;
    private final List<Button> pageTabs = new ArrayList<>();

    private LivingEntity displayEntity;

    public VisionOfInformationScreen() {
        super(Component.translatable("hahueuh.gui.vision_info.title"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;

        String preserved = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(font, left + 10, top + 10, 250, 18, Component.translatable("hahueuh.gui.vision_info.search"));
        searchBox.setMaxLength(64);
        searchBox.setValue(preserved);
        searchBox.setHint(Component.translatable("hahueuh.gui.vision_info.search_hint"));
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);

        addRenderableWidget(new VisionButton(left + 264, top + 10, 66, 18,
                Component.translatable("hahueuh.gui.vision_info.search_button"),
                BUTTON_TEX, 200, 20, null, b -> submitQuery()));

        rebuildPageTabs();
    }

    private void submitQuery() {
        String q = searchBox.getValue().trim();
        if (q.isEmpty()) return;
        PacketDistributor.sendToServer(new VisionInfoQueryPayload(q));
    }

    @Override
    public void tick() {
        if (VisionInfoClientData.version() != dataVersion) {
            dataVersion = VisionInfoClientData.version();
            result = VisionInfoClientData.latest();
            page = 0;
            rebuildDisplayEntity();
            rebuildPageTabs();
        }
    }

    private void rebuildPageTabs() {
        for (Button b : pageTabs) removeWidget(b);
        pageTabs.clear();
        List<Component> names = pageNames();
        int tabW = names.isEmpty() ? 0 : Math.min(110, (PANEL_W - 20) / Math.max(1, names.size()));
        for (int i = 0; i < names.size(); i++) {
            final int idx = i;
            VisionButton tab = new VisionButton(left + 10 + i * tabW, top + 32, tabW - 2, 16,
                    names.get(i), TAB_TEX, 110, 18, () -> page == idx, b -> page = idx);
            pageTabs.add(tab);
            addRenderableWidget(tab);
        }
    }

    private List<Component> pageNames() {
        if (result == null) return List.of();
        if (result.kind() == VisionInfoResultPayload.KIND_PLAYER) {
            return List.of(Component.translatable("hahueuh.gui.vision_info.page_overview"),
                    Component.translatable("hahueuh.gui.vision_info.page_items"),
                    Component.translatable("hahueuh.gui.vision_info.page_authorities"));
        }
        if (result.kind() == VisionInfoResultPayload.KIND_ENTITY) {
            return List.of(Component.translatable("hahueuh.gui.vision_info.page_overview"),
                    Component.translatable("hahueuh.gui.vision_info.page_drops"));
        }
        return List.of();
    }

    private void rebuildDisplayEntity() {
        displayEntity = null;
        if (result == null || minecraft == null || minecraft.level == null) return;
        try {
            if (result.kind() == VisionInfoResultPayload.KIND_PLAYER) {
                VisionInfoResultPayload.PlayerData p = result.player();
                displayEntity = new RemotePlayer(minecraft.level, new GameProfile(p.uuid(), p.name()));
            } else if (result.kind() == VisionInfoResultPayload.KIND_ENTITY) {
                ResourceLocation id = ResourceLocation.tryParse(result.entity().typeId());
                EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
                if (type != null && type.create(minecraft.level) instanceof LivingEntity living) {
                    displayEntity = living;
                }
            }
        } catch (Exception e) {
            HahUeuh.LOGGER.debug("Vision of Information: could not build display entity", e);
            displayEntity = null;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0000000);
        g.blit(BACKGROUND, left, top, 0.0f, 0.0f, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
        ResourceLocation pageArt = currentPageArt();
        if (pageArt != null) {
            g.blit(pageArt, left + CONTENT_X, top + CONTENT_Y, 0.0f, 0.0f, CONTENT_W, CONTENT_H, CONTENT_W, CONTENT_H);
        }
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    private ResourceLocation currentPageArt() {
        if (result == null) return null;
        if (result.kind() == VisionInfoResultPayload.KIND_PLAYER) {
            return switch (page) {
                case 1 -> PAGE_ITEMS;
                case 2 -> PAGE_AUTHORITIES;
                default -> PAGE_OVERVIEW;
            };
        }
        if (result.kind() == VisionInfoResultPayload.KIND_ENTITY) {
            return page == 1 ? PAGE_DROPS : PAGE_OVERVIEW;
        }
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int contentTop = top + 54;
        if (result == null) {
            g.drawCenteredString(font, Component.translatable("hahueuh.gui.vision_info.prompt"),
                    left + PANEL_W / 2, contentTop + 40, 0xFFAAAAAA);
            return;
        }
        switch (result.kind()) {
            case VisionInfoResultPayload.KIND_NOT_FOUND -> g.drawCenteredString(font,
                    Component.translatable("hahueuh.gui.vision_info.not_found", result.query()),
                    left + PANEL_W / 2, contentTop + 40, 0xFFE06666);
            case VisionInfoResultPayload.KIND_PLAYER -> renderPlayer(g, contentTop, mouseX, mouseY);
            case VisionInfoResultPayload.KIND_ENTITY -> renderEntity(g, contentTop, mouseX, mouseY);
            default -> {}
        }
    }

    private void renderPlayer(GuiGraphics g, int contentTop, int mouseX, int mouseY) {
        VisionInfoResultPayload.PlayerData p = result.player();
        switch (page) {
            case 0 -> {
                renderModel(g, left + 18, contentTop + 10, left + 108, contentTop + 160, 42, mouseX, mouseY);
                int tx = left + 120, ty = contentTop + 6;
                g.drawString(font, Component.literal(p.name()).withStyle(net.minecraft.ChatFormatting.BOLD), tx, ty, 0xFFFFFFFF);
                ty += 14;
                g.drawString(font, statusLine(p.online()), tx, ty, 0xFFAAAAAA); ty += 14;
                g.drawString(font, tr("hahueuh.gui.vision_info.health", fmt(p.health()), fmt(p.maxHealth())), tx, ty, 0xFFFF5555); ty += 12;
                g.drawString(font, tr("hahueuh.gui.vision_info.armor", p.armor()), tx, ty, 0xFFAAAAFF); ty += 12;
                g.drawString(font, tr("hahueuh.gui.vision_info.hunger", p.food()), tx, ty, 0xFFFFAA55); ty += 12;
                g.drawString(font, tr("hahueuh.gui.vision_info.position",
                        (int) Math.floor(p.x()), (int) Math.floor(p.y()), (int) Math.floor(p.z())), tx, ty, 0xFF88FF88); ty += 12;
                g.drawString(font, Component.literal(p.dimension()), tx, ty, 0xFF88CCCC);
            }
            case 1 -> {
                int x = left + 12, y = contentTop + 2;
                g.drawString(font, tr("hahueuh.gui.vision_info.inventory"), x, y, 0xFFFFFFFF);
                ItemStack hover = renderGrid(g, p.inventory(), x, y + 11, 9, mouseX, mouseY);
                int afterInv = y + 11 + rows(p.inventory().size(), 9) * SLOT + 3;
                g.drawString(font, tr("hahueuh.gui.vision_info.ender_chest"), x, afterInv, 0xFFFFFFFF);
                ItemStack hover2 = renderGrid(g, p.enderChest(), x, afterInv + 11, 9, mouseX, mouseY);
                int effX = left + 12 + 9 * SLOT + 12;
                renderEffects(g, p.effects(), effX, contentTop + 7);
                ItemStack tip = hover != null ? hover : hover2;
                if (tip != null && !tip.isEmpty()) g.renderTooltip(font, tip, mouseX, mouseY);
            }
            case 2 -> {
                int x = left + 16, y = contentTop + 8;
                g.drawString(font, tr("hahueuh.gui.vision_info.authorities"), x, y, 0xFFFFD700); y += 16;
                y = authLine(g, x, y, "hahueuh.authority.return_by_death", p.returnByDeath(), null);
                y = authLine(g, x, y, "hahueuh.authority.domain", p.domain(), null);
                y = authLine(g, x, y, "hahueuh.authority.sloth", p.sloth(),
                        p.sloth() ? Component.translatable(SlothVariant.byOrdinal(p.slothVariant()).translationKey) : null);
                authLine(g, x, y, "hahueuh.authority.greed", p.greed(),
                        p.greed() ? Component.translatable(GreedVariant.byOrdinal(p.greedVariant()).translationKey) : null);
            }
            default -> {}
        }
    }

    private int authLine(GuiGraphics g, int x, int y, String key, boolean has, Component variant) {
        Component label = Component.translatable(key);
        g.drawString(font, label, x, y, has ? 0xFFFFFFFF : 0xFF666666);
        Component value = has
                ? (variant != null ? variant : Component.translatable("hahueuh.gui.vision_info.yes"))
                : Component.translatable("hahueuh.gui.vision_info.no");
        g.drawString(font, value, x + 130, y, has ? 0xFF66FF66 : 0xFF884444);
        return y + 14;
    }

    private void renderEntity(GuiGraphics g, int contentTop, int mouseX, int mouseY) {
        VisionInfoResultPayload.EntityData e = result.entity();
        if (page == 0) {
            renderModel(g, left + 18, contentTop + 10, left + 108, contentTop + 160, 42, mouseX, mouseY);
            int tx = left + 120, ty = contentTop + 6;
            g.drawString(font, Component.literal(e.name()).withStyle(net.minecraft.ChatFormatting.BOLD), tx, ty, 0xFFFFFFFF); ty += 14;
            g.drawString(font, Component.literal(e.typeId()), tx, ty, 0xFF888888); ty += 14;
            g.drawString(font, tr("hahueuh.gui.vision_info.health", fmt(e.maxHealth()), fmt(e.maxHealth())), tx, ty, 0xFFFF5555); ty += 12;
            g.drawString(font, tr("hahueuh.gui.vision_info.armor", e.armor()), tx, ty, 0xFFAAAAFF); ty += 20;
            g.drawString(font, tr("hahueuh.gui.vision_info.loaded", e.loadedCount()), tx, ty, 0xFF88CCCC);
        } else {
            int x = left + 14, y = contentTop + 2;
            g.drawString(font, tr("hahueuh.gui.vision_info.drops"), x, y + 4, 0xFFFFD700); y += 14;
            if (e.drops().isEmpty()) {
                g.drawString(font, tr("hahueuh.gui.vision_info.no_drops"), x, y, 0xFF888888);
                return;
            }
            ItemStack tip = null;
            int rowY = y;
            for (VisionInfoResultPayload.Drop d : e.drops()) {
                if (rowY > top + PANEL_H - 20) break;
                ResourceLocation itemId = ResourceLocation.tryParse(d.itemId());
                Item item = itemId == null ? null : BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
                ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
                g.renderItem(stack, x, rowY);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= rowY && mouseY < rowY + 16 && !stack.isEmpty()) tip = stack;
                Component nameComp = stack.isEmpty() ? Component.literal(d.itemId()) : stack.getHoverName();
                g.drawString(font, font.substrByWidth(nameComp, 150).getString(), x + 22, rowY + 4, 0xFFDDDDDD);
                String countStr = d.minCount() == d.maxCount()
                        ? d.minCount() + "x"
                        : d.minCount() + "-" + d.maxCount() + "x";
                g.drawString(font, countStr, x + 178, rowY + 4, 0xFFDDCC88);
                g.drawString(font, String.format("%.1f%%", d.chance() * 100f), x + 230, rowY + 4, 0xFF66FF66);
                rowY += SLOT;
            }
            if (tip != null) g.renderTooltip(font, tip, mouseX, mouseY);
        }
    }

    private void renderModel(GuiGraphics g, int x1, int y1, int x2, int y2, int scale, int mouseX, int mouseY) {
        if (displayEntity == null) {
            g.drawCenteredString(font, Component.literal("?"), (x1 + x2) / 2, (y1 + y2) / 2, 0xFF555555);
            return;
        }
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, scale, 0.0625f,
                    mouseX, mouseY, displayEntity);
        } catch (Exception ignored) {
            g.drawCenteredString(font, Component.literal("?"), (x1 + x2) / 2, (y1 + y2) / 2, 0xFF555555);
        }
    }

    private ItemStack renderGrid(GuiGraphics g, List<ItemStack> items, int x, int y, int cols, int mouseX, int mouseY) {
        ItemStack hovered = null;
        for (int i = 0; i < items.size(); i++) {
            int col = i % cols, row = i / cols;
            int sx = x + col * SLOT, sy = y + row * SLOT;
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, sx, sy);
                g.renderItemDecorations(font, stack, sx, sy);
                if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) hovered = stack;
            }
        }
        return hovered;
    }

    private void renderEffects(GuiGraphics g, List<VisionInfoResultPayload.Effect> effects, int x, int y) {
        g.drawString(font, tr("hahueuh.gui.vision_info.effects"), x, y, 0xFFFFFFFF);
        y += 12;
        if (effects.isEmpty()) {
            g.drawString(font, tr("hahueuh.gui.vision_info.no_effects"), x, y, 0xFF888888);
            return;
        }
        for (VisionInfoResultPayload.Effect eff : effects) {
            ResourceLocation id = ResourceLocation.tryParse(eff.id());
            MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
            Component name = effect != null ? effect.getDisplayName() : Component.literal(eff.id());
            String amp = eff.amplifier() > 0 ? " " + (eff.amplifier() + 1) : "";
            g.drawString(font, Component.literal("• ").append(name).append(amp + "  (" + (eff.duration() / 20) + "s)"),
                    x, y, 0xFFCCCCFF);
            y += 11;
        }
    }

    private Component statusLine(boolean online) {
        return Component.translatable(online ? "hahueuh.gui.vision_info.online" : "hahueuh.gui.vision_info.offline");
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private static String fmt(float v) {
        return String.format("%.0f", v);
    }

    private static int rows(int count, int cols) {
        return (count + cols - 1) / cols;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / numpad Enter
            if (searchBox != null && searchBox.isFocused()) {
                submitQuery();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
