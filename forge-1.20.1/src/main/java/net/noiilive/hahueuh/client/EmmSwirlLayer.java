package net.noiilive.hahueuh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.ClientPlayerData;

public class EmmSwirlLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation DEFAULT_LAYER = new ModelLayerLocation(
            new ResourceLocation(HahUeuh.MODID, "emm_swirl"), "default");
    public static final ModelLayerLocation SLIM_LAYER = new ModelLayerLocation(
            new ResourceLocation(HahUeuh.MODID, "emm_swirl"), "slim");

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HahUeuh.MODID, "textures/entity/emm_active.png");
    private static final float SWIRL_GREY = 0.5f;

    private final PlayerModel<AbstractClientPlayer> model;

    public EmmSwirlLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
                         EntityModelSet modelSet, boolean slim) {
        super(renderer);
        this.model = new PlayerModel<>(modelSet.bakeLayer(slim ? SLIM_LAYER : DEFAULT_LAYER), slim);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientPlayerData.of(player).isEmmActive()) return;
        if (player.isInvisible()) return;

        float f = (float) player.tickCount + partialTicks;
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        VertexConsumer consumer = buffer.getBuffer(
                RenderType.energySwirl(TEXTURE, f * 0.01F % 1.0F, f * 0.01F % 1.0F));
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                SWIRL_GREY, SWIRL_GREY, SWIRL_GREY, 1.0f);
    }
}
