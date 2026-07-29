package net.noiilive.hahueuh.client.model;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.YinSealEntity;
import net.noiilive.hahueuh.client.animation.YinSealAnimations;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public final class YinSealModel extends HierarchicalModel<YinSealEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(HahUeuh.MODID, "yin_seal"), "main");
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(HahUeuh.MODID, "textures/entity/blackhole.png");

    private static final Vector3f ANIM_VEC = new Vector3f();

    private final ModelPart root;

    public YinSealModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("bone", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(YinSealEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        KeyframeAnimations.animate(this, YinSealAnimations.IDLE, (long) (ageInTicks * 50.0F), 1.0F, ANIM_VEC);
    }
}
