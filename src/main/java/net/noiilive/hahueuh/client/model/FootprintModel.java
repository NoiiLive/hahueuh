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

public final class FootprintModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "footprint"), "main");

    public static final ResourceLocation TEXTURE_RED =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/entity/footprint_red.png");
    public static final ResourceLocation TEXTURE_GREEN =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/entity/footprint_green.png");
    public static final ResourceLocation TEXTURE_WHITE =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/entity/footprint_white.png");
    public static final ResourceLocation TEXTURE_PURPLE =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/entity/footprint_purple.png");

    private FootprintModel() {}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("footprint",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(2.0F, -5.0F, 4.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 24.0F, -3.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }
}
