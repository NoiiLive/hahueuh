package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.WitchFactorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public final class WitchFactorRenderer extends EntityRenderer<WitchFactorEntity> {
    public WitchFactorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(WitchFactorEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(WitchFactorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
    }
}
