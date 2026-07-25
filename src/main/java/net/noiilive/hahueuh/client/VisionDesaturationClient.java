package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.BookOfLifeStats;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Field;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class VisionDesaturationClient {
    private static final ResourceLocation EFFECT =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "shaders/post/desaturate_vision.json");
    private static final float START_FRACTION = 0.5f;

    private static Field postEffectField;
    private static Field effectActiveField;
    private static boolean reflectionFailed;

    private static PostChain ourEffect;
    private static boolean ourEffectFailed;

    private VisionDesaturationClient() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GameRenderer renderer = mc.gameRenderer;
        if (renderer == null || !ensureReflection()) return;

        float intensity = desiredIntensity(mc.player);
        if (intensity > 0.001f) {
            PostChain effect = ensureInstalled(mc, renderer);
            if (effect != null) effect.setUniform("Intensity", intensity);
        } else {
            uninstallIfOurs(renderer);
        }
    }

    private static PostChain ensureInstalled(Minecraft mc, GameRenderer renderer) {
        if (ourEffect == null && !ourEffectFailed) {
            try {
                ourEffect = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), EFFECT);
                ourEffect.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            } catch (Exception e) {
                ourEffectFailed = true;
                HahUeuh.LOGGER.warn("Failed to load vision desaturation shader: {}", EFFECT, e);
                return null;
            }
        }
        if (ourEffect == null) return null;

        try {
            Object current = postEffectField.get(renderer);
            if (current != ourEffect) {
                if (current instanceof PostChain otherChain) otherChain.close();
                postEffectField.set(renderer, ourEffect);
                effectActiveField.setBoolean(renderer, true);
            }
        } catch (ReflectiveOperationException e) {
            reflectionFailed = true;
            return null;
        }
        return ourEffect;
    }

    private static void uninstallIfOurs(GameRenderer renderer) {
        if (ourEffect == null) return;
        try {
            if (postEffectField.get(renderer) == ourEffect) {
                postEffectField.set(renderer, null);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean ensureReflection() {
        if (reflectionFailed) return false;
        if (postEffectField != null) return true;
        try {
            postEffectField = GameRenderer.class.getDeclaredField("postEffect");
            postEffectField.setAccessible(true);
            effectActiveField = GameRenderer.class.getDeclaredField("effectActive");
            effectActiveField.setAccessible(true);
            return true;
        } catch (ReflectiveOperationException e) {
            reflectionFailed = true;
            HahUeuh.LOGGER.warn("Vision desaturation: couldn't reflect GameRenderer's post-effect fields", e);
            return false;
        }
    }

    private static float desiredIntensity(LocalPlayer player) {
        if (player == null) return 0f;
        int max = BookOfLifeStats.maxOd(player);
        if (max <= 0) return 0f;
        float fraction = player.getData(ModAttachments.PLAYER_OD_CURRENT.get()) / (float) max;
        if (fraction >= START_FRACTION) return 0f;
        return Mth.clamp((START_FRACTION - fraction) / START_FRACTION, 0f, 1f);
    }
}
