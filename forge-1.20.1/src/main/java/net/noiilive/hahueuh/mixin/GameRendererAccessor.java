package net.noiilive.hahueuh.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("postEffect")
    PostChain hahueuh$getPostEffect();

    @Accessor("postEffect")
    void hahueuh$setPostEffect(PostChain chain);

    @Accessor("effectActive")
    void hahueuh$setEffectActive(boolean active);
}
