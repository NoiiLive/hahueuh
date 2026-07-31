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

public final class MorningstarHeadModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "morningstar_head"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/entity/morningstar_head.png");

    public static final float TOP_SPIKE_TIP = 11.0F / 16.0F;

    private MorningstarHeadModel() {}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 16).addBox(-1.0F, -5.0F, -7.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 16).addBox(-1.0F, -5.0F, 4.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(20, 16).addBox(4.0F, -5.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(20, 20).addBox(-7.0F, -5.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 21).addBox(-1.0F, -11.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 21).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
