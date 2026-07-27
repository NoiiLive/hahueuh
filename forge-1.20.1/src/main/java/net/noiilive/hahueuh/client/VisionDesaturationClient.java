package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.noiilive.hahueuh.BookOfLifeStats;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.mixin.GameRendererAccessor;
import net.noiilive.hahueuh.mixin.PostChainAccessor;
import net.noiilive.hahueuh.network.ClientPlayerData;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class VisionDesaturationClient {
    private static final ResourceLocation EFFECT =
            new ResourceLocation(HahUeuh.MODID, "shaders/post/desaturate_vision.json");
    private static final float START_FRACTION = 0.5f;

    private static PostChain ourEffect;
    private static boolean ourEffectFailed;

    private VisionDesaturationClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        GameRenderer renderer = mc.gameRenderer;
        if (renderer == null) return;

        float intensity = desiredIntensity(mc.player);
        if (intensity > 0.001f) {
            PostChain effect = ensureInstalled(mc, renderer);
            if (effect != null) setIntensity(effect, intensity);
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

        GameRendererAccessor accessor = (GameRendererAccessor) renderer;
        PostChain current = accessor.hahueuh$getPostEffect();
        if (current != ourEffect) {
            if (current != null) current.close();
            accessor.hahueuh$setPostEffect(ourEffect);
            accessor.hahueuh$setEffectActive(true);
        }
        return ourEffect;
    }

    private static void uninstallIfOurs(GameRenderer renderer) {
        if (ourEffect == null) return;
        GameRendererAccessor accessor = (GameRendererAccessor) renderer;
        if (accessor.hahueuh$getPostEffect() == ourEffect) {
            accessor.hahueuh$setPostEffect(null);
        }
    }

    private static void setIntensity(PostChain effect, float intensity) {
        for (PostPass pass : ((PostChainAccessor) effect).hahueuh$getPasses()) {
            Uniform uniform = pass.getEffect().getUniform("Intensity");
            if (uniform != null) uniform.set(intensity);
        }
    }

    private static float desiredIntensity(LocalPlayer player) {
        if (player == null) return 0f;
        net.noiilive.hahueuh.capability.PlayerData data = ClientPlayerData.of(player);
        if (data == null) return 0f;
        int max = BookOfLifeStats.maxOd(data);
        if (max <= 0) return 0f;
        float fraction = data.getOdCurrent() / (float) max;
        if (fraction >= START_FRACTION) return 0f;
        return Mth.clamp((START_FRACTION - fraction) / START_FRACTION, 0f, 1f);
    }
}
