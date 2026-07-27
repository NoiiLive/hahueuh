package net.noiilive.hahueuh.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.client.EfficientEnchantOptionsData;
import net.noiilive.hahueuh.menu.EfficientEnchantingMenu;
import net.noiilive.hahueuh.network.BackToEnchantingPayload;
import net.noiilive.hahueuh.network.EfficientEnchantOptionsPayload;
import net.noiilive.hahueuh.network.EfficientEnchantSelectPayload;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class EfficientEnchantingScreen extends AbstractContainerScreen<EfficientEnchantingMenu> {
    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/gui/" + name + ".png");
    }

    private static final ResourceLocation BG = tex("efficient_enchanting_bg");
    private static final ResourceLocation ROW = tex("efficient_enchant_row");
    private static final ResourceLocation BACK_BUTTON = tex("efficient_enchant_back_button");
    private static final ResourceLocation BOOK_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/enchanting_table_book.png");

    private static final int HEADER_COLOR = 0xFF373737;
    private static final int BODY_COLOR = 0xFF6B614C;
    private static final int BODY_COLOR_DIM = 0xFF8A8478;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int NAME_COLOR_DIM = 0xFF888888;
    private static final int COST_OK_COLOR = 0xFF80FF80;
    private static final int COST_BAD_COLOR = 0xFFFF6060;

    private static final int LIST_X = 60;
    private static final int LIST_Y = 14;
    private static final int ROW_W = 108;
    private static final int ROW_H = 19;
    private static final int VISIBLE_ROWS = 3;

    private static final int ARROW_X = LIST_X + ROW_W + 1;
    private static final int ARROW_W = 7;
    private static final int ARROW_H = 8;

    private static final int BACK_BTN_X = -27;
    private static final int BACK_BTN_Y = 8;
    private static final int BACK_BTN_SIZE = 28;

    private int scroll;

    private BookModel bookModel;
    private final RandomSource random = RandomSource.create();
    private int time;
    private float flip, oFlip, flipT, flipA;
    private float open, oOpen;
    private ItemStack lastItem = ItemStack.EMPTY;

    public EfficientEnchantingScreen(EfficientEnchantingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        tickBook();
    }

    private void tickBook() {
        ItemStack current = menu.itemSlot();
        if (!ItemStack.matches(current, lastItem)) {
            lastItem = current;
            do {
                flipT += random.nextInt(4) - random.nextInt(4);
            } while (flip <= flipT + 1.0F && flip >= flipT - 1.0F);
        }

        time++;
        oFlip = flip;
        oOpen = open;

        open = Mth.clamp(open + (options().isEmpty() ? -0.2F : 0.2F), 0.0F, 1.0F);
        float target = Mth.clamp((flipT - flip) * 0.4F, -0.2F, 0.2F);
        flipA += (target - flipA) * 0.9F;
        flip += flipA;
    }

    private List<EfficientEnchantOptionsPayload.Option> options() {
        return EfficientEnchantOptionsData.get();
    }

    private boolean canAfford() {
        if (minecraft == null || minecraft.player == null) return false;
        if (minecraft.player.getAbilities().instabuild) return true;
        return minecraft.player.experienceLevel >= EfficientEnchantingMenu.XP_COST
                && menu.lapisSlot().getCount() >= EfficientEnchantingMenu.LAPIS_COST;
    }

    private int maxScroll() {
        return Math.max(0, options().size() - VISIBLE_ROWS);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0.0f, 0.0f, imageWidth, imageHeight, imageWidth, imageHeight);
        renderBook(g, leftPos, topPos, partialTick);
    }

    private void renderBook(GuiGraphics g, int x, int y, float partialTick) {
        float openAmount = Mth.lerp(partialTick, oOpen, open);
        float flipAmount = Mth.lerp(partialTick, oFlip, flip);
        Lighting.setupForEntityInInventory();
        g.pose().pushPose();
        g.pose().translate(x + 33.0F, y + 31.0F, 100.0F);
        g.pose().scale(-40.0F, 40.0F, 40.0F);
        g.pose().mulPose(Axis.XP.rotationDegrees(25.0F));
        g.pose().translate((1.0F - openAmount) * 0.2F, (1.0F - openAmount) * 0.1F, (1.0F - openAmount) * 0.25F);
        g.pose().mulPose(Axis.YP.rotationDegrees(-(1.0F - openAmount) * 90.0F - 90.0F));
        g.pose().mulPose(Axis.XP.rotationDegrees(180.0F));
        float pageLeft = Mth.clamp(Mth.frac(flipAmount + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float pageRight = Mth.clamp(Mth.frac(flipAmount + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        bookModel.setupAnim(0.0F, pageLeft, pageRight, openAmount);
        VertexConsumer buffer = g.bufferSource().getBuffer(bookModel.renderType(BOOK_LOCATION));
        bookModel.renderToBuffer(g.pose(), buffer, 15728880, OverlayTexture.NO_OVERLAY);
        g.flush();
        g.pose().popPose();
        Lighting.setupFor3DItems();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderOptions(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
        if (isHovering(BACK_BTN_X, BACK_BTN_Y, BACK_BTN_SIZE, BACK_BTN_SIZE, mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable("hahueuh.gui.efficient_enchanting.back"), mouseX, mouseY);
        }
    }

    private void renderOptions(GuiGraphics g, int mouseX, int mouseY) {
        List<EfficientEnchantOptionsPayload.Option> opts = options();
        int listLeft = leftPos + LIST_X;
        int listTop = topPos + LIST_Y;

        if (menu.itemSlot().isEmpty()) {
            drawWrapped(g, Component.translatable("hahueuh.gui.efficient_enchanting.prompt"),
                    listLeft + 2, listTop + 6, ROW_W - 4, BODY_COLOR);
        } else if (opts.isEmpty()) {
            drawWrapped(g, Component.translatable("hahueuh.gui.efficient_enchanting.none"),
                    listLeft + 2, listTop + 6, ROW_W - 4, BODY_COLOR);
        } else {
            scroll = Math.min(scroll, maxScroll());
            boolean afford = canAfford();
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int idx = scroll + row;
                if (idx >= opts.size()) break;
                EfficientEnchantOptionsPayload.Option opt = opts.get(idx);
                int rx = listLeft;
                int ry = listTop + row * ROW_H;
                boolean hovered = isHovering(LIST_X, LIST_Y + row * ROW_H, ROW_W, ROW_H, mouseX, mouseY);

                int state = hovered && afford ? 1 : 0;
                g.blit(ROW, rx, ry, ROW_W, ROW_H, 0.0f, state * ROW_H, ROW_W, ROW_H, ROW_W, ROW_H * 2);

                Component name = enchantName(opt);
                int nameColor = afford ? NAME_COLOR : NAME_COLOR_DIM;
                String costText = String.valueOf(EfficientEnchantingMenu.XP_COST);
                int costColor = afford ? COST_OK_COLOR : COST_BAD_COLOR;
                int nameWidth = ROW_W - 12 - font.width(costText);
                g.drawString(font, font.substrByWidth(name, nameWidth).getString(), rx + 5, ry + 6, nameColor, true);
                g.drawString(font, costText, rx + ROW_W - 6 - font.width(costText), ry + 6, costColor, true);
            }
        }

        boolean canUp = scroll > 0;
        boolean canDown = scroll < maxScroll();
        int arrowX = listLeft + ARROW_X - LIST_X;
        g.drawString(font, "▲", arrowX, topPos + LIST_Y, canUp ? HEADER_COLOR : BODY_COLOR_DIM, false);
        g.drawString(font, "▼", arrowX, topPos + LIST_Y + VISIBLE_ROWS * ROW_H - ARROW_H, canDown ? HEADER_COLOR : BODY_COLOR_DIM, false);

        boolean backHovered = isHovering(BACK_BTN_X, BACK_BTN_Y, BACK_BTN_SIZE, BACK_BTN_SIZE, mouseX, mouseY);
        int backState = backHovered ? 1 : 0;
        g.blit(BACK_BUTTON, leftPos + BACK_BTN_X, topPos + BACK_BTN_Y, BACK_BTN_SIZE, BACK_BTN_SIZE,
                0.0f, backState * BACK_BTN_SIZE, BACK_BTN_SIZE, BACK_BTN_SIZE, BACK_BTN_SIZE, BACK_BTN_SIZE * 2);
    }

    private void drawWrapped(GuiGraphics g, FormattedText text, int x, int y, int lineWidth, int color) {
        for (var line : font.split(text, lineWidth)) {
            g.drawString(font, line, x, y, color, false);
            y += 9;
        }
    }

    private Component enchantName(EfficientEnchantOptionsPayload.Option opt) {
        Holder<Enchantment> holder = holderFor(opt.enchantmentId());
        if (holder == null) return Component.literal(opt.enchantmentId());
        return Enchantment.getFullname(holder, opt.level());
    }

    private Holder<Enchantment> holderFor(String id) {
        if (minecraft == null || minecraft.level == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, rl)).orElse(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (isHovering(BACK_BTN_X, BACK_BTN_Y, BACK_BTN_SIZE, BACK_BTN_SIZE, mouseX, mouseY)) {
            PacketDistributor.sendToServer(new BackToEnchantingPayload());
            playClickSound();
            return true;
        }

        if (isHovering(ARROW_X, LIST_Y, ARROW_W, ARROW_H, mouseX, mouseY) && scroll > 0) {
            scroll--;
            return true;
        }
        if (isHovering(ARROW_X, LIST_Y + VISIBLE_ROWS * ROW_H - ARROW_H, ARROW_W, ARROW_H, mouseX, mouseY) && scroll < maxScroll()) {
            scroll++;
            return true;
        }

        if (!menu.itemSlot().isEmpty() && canAfford()) {
            List<EfficientEnchantOptionsPayload.Option> opts = options();
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int idx = scroll + row;
                if (idx >= opts.size()) break;
                if (isHovering(LIST_X, LIST_Y + row * ROW_H, ROW_W, ROW_H, mouseX, mouseY)) {
                    PacketDistributor.sendToServer(new EfficientEnchantSelectPayload(opts.get(idx).enchantmentId()));
                    playClickSound();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClickSound() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, HEADER_COLOR, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HEADER_COLOR, false);
    }

    @Override
    public void removed() {
        super.removed();
        EfficientEnchantOptionsData.clear();
    }
}
