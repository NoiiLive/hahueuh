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

public final class UnseenHandModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "unseen_hand"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "textures/entity/unseen_hand.png");

    private UnseenHandModel() {}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition tendril = root.addOrReplaceChild("tendril",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition hand = tendril.addOrReplaceChild("hand",
                CubeListBuilder.create().texOffs(0, 8).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -1.0F, -5.0F));

        hand.addOrReplaceChild("index",
                CubeListBuilder.create().texOffs(12, 13).addBox(-0.5F, -1.0F, -3.5F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.5F, -3.0F, -1.5F));
        hand.addOrReplaceChild("middle",
                CubeListBuilder.create().texOffs(0, 14).addBox(-0.5F, -1.0F, -3.5F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.5F, -3.0F, -1.5F));
        hand.addOrReplaceChild("ring",
                CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -1.0F, -3.5F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-0.5F, -3.0F, -1.5F));
        hand.addOrReplaceChild("pinky",
                CubeListBuilder.create().texOffs(10, 18).addBox(-0.5F, -1.0F, -2.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.5F, -3.0F, -1.5F));
        hand.addOrReplaceChild("thumb",
                CubeListBuilder.create().texOffs(12, 8).addBox(0.0F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 0.5F, -1.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
