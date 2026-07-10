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

public final class UnseenTendrilModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "unseen_tendril"), "main");

    public static final float SEGMENT_LENGTH = 6.0F / 16.0F;

    private UnseenTendrilModel() {}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("tendril",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }
}
