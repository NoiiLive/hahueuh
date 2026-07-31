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

public final class WhipSegmentModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(HahUeuh.MODID, "whip_segment"), "main");

    public static final ResourceLocation TEXTURE =
            new ResourceLocation(HahUeuh.MODID, "textures/entity/whip_segment.png");

    public static final float SEGMENT_LENGTH = 4.0F / 16.0F;

    private WhipSegmentModel() {}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition main = root.addOrReplaceChild("bb_main", CubeListBuilder.create(),
                PartPose.offset(0.0F, 2.8284F, 0.0F));

        main.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(6, 0).addBox(1.0F, -3.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 3).addBox(2.0F, -3.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 6).addBox(0.0F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 3).addBox(-1.0F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.4142F, 0.0F, 0.0F, 0.0F, -0.7854F));

        main.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.8284F, 0.0F, 0.0F, 0.0F, -0.7854F));

        return LayerDefinition.create(mesh, 16, 16);
    }
}
