package net.noiilive.hahueuh.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.mixin.LevelRendererAccessor;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class OutlineEffectSupport {
    private OutlineEffectSupport() {}

    private static boolean pending;

    public static void request() {
        pending = true;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        if (!pending) return;
        pending = false;

        Minecraft mc = Minecraft.getInstance();
        if (!mc.levelRenderer.shouldShowEntityOutlines()) return;
        PostChain effect = ((LevelRendererAccessor) mc.levelRenderer).hahueuh$entityEffect();
        if (effect == null) return;
        effect.process(event.getPartialTick());
        mc.getMainRenderTarget().bindWrite(false);
    }
}
