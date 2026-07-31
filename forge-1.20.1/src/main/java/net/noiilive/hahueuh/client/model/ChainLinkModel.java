package net.noiilive.hahueuh.client.model;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class ChainLinkModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(HahUeuh.MODID, "chain_link"), "main");

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(HahUeuh.MODID, "textures/entity/chain_link.png");

    public static final float LINK_LENGTH = 5.6F / 16.0F;

    private ChainLinkModel() {}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition link = root.addOrReplaceChild("link", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        link.addOrReplaceChild("chain1", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.5F, -4.0F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 3.35F, 0.0F));

        PartDefinition chain2 = link.addOrReplaceChild("chain2", CubeListBuilder.create(),
                PartPose.offset(0.0F, -3.3F, 0.0F));

        chain2.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(0, 4).addBox(-1.5F, -2.0F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.15F, 0.0F, 0.0F, -1.5708F, 0.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }
}
