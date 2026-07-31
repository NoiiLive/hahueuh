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

        PartDefinition chain1 = link.addOrReplaceChild("chain1", CubeListBuilder.create(),
                PartPose.offset(0.0F, 3.35F, 0.0F));

        chain1.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(6, 2).addBox(0.375F, -0.625F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-0.625F, -1.625F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 4).addBox(-1.625F, -0.625F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 7).addBox(-0.625F, 0.375F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.1768F, -2.1213F, 0.0F, 0.0F, 0.0F, -0.7854F));

        PartDefinition chain2 = link.addOrReplaceChild("chain2", CubeListBuilder.create(),
                PartPose.offset(0.0F, -3.3F, 0.0F));

        chain2.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                        .texOffs(4, 4).addBox(-1.625F, -0.625F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 2).addBox(-0.625F, -1.625F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(6, 0).addBox(0.375F, -0.625F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 7).addBox(-0.625F, 0.375F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.1287F, 0.1768F, 1.5708F, -0.7854F, -1.5708F));

        return LayerDefinition.create(mesh, 16, 16);
    }
}
