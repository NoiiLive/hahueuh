package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.ClientVisionOfLifeGlowState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class VisionOfLifeInfoRenderer {
    private VisionOfLifeInfoRenderer() {}

    private static final float LINE_HEIGHT = 9.0f;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (ClientVisionOfLifeGlowState.isEmpty()) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        if (ClientVisionOfLifeGlowState.categoryOf(entity.getId()) == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player) return;

        Vec3 attach = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(event.getPartialTick()));
        if (attach == null) return;

        event.setCanRender(TriState.FALSE);

        Component username = event.getOriginalContent();
        Component info = buildInfoLine(living);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(attach.x, attach.y + 0.5, attach.z);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = poseStack.last().pose();

        Font font = mc.font;
        int bgColor = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();

        drawLine(font, info, -LINE_HEIGHT, buffer, matrix4f, bgColor, light);
        drawLine(font, username, 0.0f, buffer, matrix4f, bgColor, light);

        poseStack.popPose();
    }

    private static Component buildInfoLine(LivingEntity living) {
        int armor = (int) living.getArmorValue();
        float health = living.getHealth();
        float maxHealth = living.getMaxHealth();
        ChatFormatting healthColor = healthColor(health, maxHealth);

        return Component.literal("Armor: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(armor)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("  HP: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.0f/%.0f", health, maxHealth)).withStyle(healthColor, ChatFormatting.BOLD));
    }

    private static ChatFormatting healthColor(float health, float maxHealth) {
        float pct = maxHealth <= 0.0f ? 0.0f : health / maxHealth;
        if (pct > 0.5f) return ChatFormatting.GREEN;
        if (pct > 0.25f) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static void drawLine(Font font, Component line, float y, MultiBufferSource buffer, Matrix4f matrix4f, int bgColor, int light) {
        float x = -font.width(line) / 2.0f;
        font.drawInBatch(line, x, y, 0xFFFFFFFF, false, matrix4f, buffer, Font.DisplayMode.SEE_THROUGH, bgColor, light);
        font.drawInBatch(line, x, y, -1, false, matrix4f, buffer, Font.DisplayMode.NORMAL, 0, light);
    }
}
