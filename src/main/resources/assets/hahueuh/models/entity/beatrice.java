

public class beatrice<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "beatrice"), "main");
	private final ModelPart head;
	private final ModelPart left_pigtail_1;
	private final ModelPart left_hair_bow;
	private final ModelPart left_pigtail_2;
	private final ModelPart left_pigtail_3;
	private final ModelPart right_pigtail_1;
	private final ModelPart right_hair_bow;
	private final ModelPart right_pigtail_2;
	private final ModelPart right_pigtail_3;
	private final ModelPart body;
	private final ModelPart bow;
	private final ModelPart left_arm;
	private final ModelPart right_arm;
	private final ModelPart left_leg;
	private final ModelPart right_leg;

	public beatrice(ModelPart root) {
		this.head = root.getChild("head");
		this.left_pigtail_1 = this.head.getChild("left_pigtail_1");
		this.left_hair_bow = this.left_pigtail_1.getChild("left_hair_bow");
		this.left_pigtail_2 = this.left_pigtail_1.getChild("left_pigtail_2");
		this.left_pigtail_3 = this.left_pigtail_2.getChild("left_pigtail_3");
		this.right_pigtail_1 = this.head.getChild("right_pigtail_1");
		this.right_hair_bow = this.right_pigtail_1.getChild("right_hair_bow");
		this.right_pigtail_2 = this.right_pigtail_1.getChild("right_pigtail_2");
		this.right_pigtail_3 = this.right_pigtail_2.getChild("right_pigtail_3");
		this.body = root.getChild("body");
		this.bow = this.body.getChild("bow");
		this.left_arm = root.getChild("left_arm");
		this.right_arm = root.getChild("right_arm");
		this.left_leg = root.getChild("left_leg");
		this.right_leg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 19).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 19).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 5.0F, 0.0F));

		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(20, 39).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(64, 20).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -9.75F, 0.5F, 0.0F, 0.0F, -0.1745F));

		PartDefinition left_pigtail_1 = head.addOrReplaceChild("left_pigtail_1", CubeListBuilder.create().texOffs(40, 0).addBox(-0.6485F, -2.8178F, -1.5852F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.25F))
		.texOffs(0, 39).addBox(-0.6485F, -2.8178F, -1.5852F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(3.6485F, -8.1822F, 2.5852F));

		PartDefinition left_hair_bow = left_pigtail_1.addOrReplaceChild("left_hair_bow", CubeListBuilder.create(), PartPose.offset(-0.0273F, -0.2733F, -2.6222F));

		PartDefinition cube_r2 = left_hair_bow.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(62, 35).addBox(-2.5F, -2.0F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.6289F, 1.2055F, 0.537F, 0.0F, 0.0F, 0.0F));

		PartDefinition cube_r3 = left_hair_bow.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(62, 35).addBox(-2.0F, -1.0F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.3789F, -0.7945F, 0.037F, -0.3927F, 0.0F, -0.3927F));

		PartDefinition left_pigtail_2 = left_pigtail_1.addOrReplaceChild("left_pigtail_2", CubeListBuilder.create().texOffs(0, 51).addBox(-0.8333F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 55).addBox(-0.8333F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(0.1849F, 4.1822F, 1.4148F));

		PartDefinition left_pigtail_3 = left_pigtail_2.addOrReplaceChild("left_pigtail_3", CubeListBuilder.create().texOffs(64, 8).addBox(-0.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.25F))
		.texOffs(0, 61).addBox(-0.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.3333F, 6.0F, 0.5F));

		PartDefinition right_pigtail_1 = head.addOrReplaceChild("right_pigtail_1", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-4.3515F, -2.8178F, -1.5852F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.25F)).mirror(false)
		.texOffs(0, 39).mirror().addBox(-4.3515F, -2.8178F, -1.5852F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.6485F, -8.1822F, 2.5852F));

		PartDefinition right_hair_bow = right_pigtail_1.addOrReplaceChild("right_hair_bow", CubeListBuilder.create(), PartPose.offset(0.0273F, -0.2733F, -2.6222F));

		PartDefinition cube_r4 = right_hair_bow.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(62, 35).mirror().addBox(-2.5F, -2.0F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.6289F, 1.2055F, 0.537F, 0.0F, 0.0F, 0.0F));

		PartDefinition cube_r5 = right_hair_bow.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(62, 35).mirror().addBox(-3.0F, -1.0F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.3789F, -0.7945F, 0.037F, -0.3927F, 0.0F, 0.3927F));

		PartDefinition right_pigtail_2 = right_pigtail_1.addOrReplaceChild("right_pigtail_2", CubeListBuilder.create().texOffs(0, 51).mirror().addBox(-3.1667F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(16, 55).mirror().addBox(-3.1667F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.25F)).mirror(false), PartPose.offset(-0.1849F, 4.1822F, 1.4148F));

		PartDefinition right_pigtail_3 = right_pigtail_2.addOrReplaceChild("right_pigtail_3", CubeListBuilder.create().texOffs(64, 8).mirror().addBox(-2.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.25F)).mirror(false)
		.texOffs(0, 61).mirror().addBox(-2.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.3333F, 6.0F, 0.5F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(20, 42).addBox(-3.0F, 5.0F, -1.5F, 6.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(38, 42).addBox(-3.0F, 5.0F, -1.5F, 6.0F, 10.0F, 3.0F, new CubeDeformation(0.25F))
		.texOffs(32, 35).addBox(-6.0F, 5.0F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.6F))
		.texOffs(40, 12).addBox(-4.0F, 11.0F, -1.5F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.5F))
		.texOffs(0, 0).addBox(-6.0F, 14.0F, -2.5F, 12.0F, 11.0F, 8.0F, new CubeDeformation(0.25F))
		.texOffs(0, 71).addBox(-6.0F, 14.0F, -2.5F, 12.0F, 11.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, -1.0F, 0.0F));

		PartDefinition bow = body.addOrReplaceChild("bow", CubeListBuilder.create().texOffs(64, 24).addBox(-1.0589F, -1.0F, -1.3143F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(18, 65).addBox(-3.5589F, -2.5F, -0.5643F, 3.0F, 5.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(24, 65).addBox(0.4411F, -2.5F, -0.5643F, 3.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.7248F, 14.2151F, -2.1641F, -0.3927F, -0.7854F, 0.0F));

		PartDefinition cube_r6 = bow.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(12, 65).addBox(-3.25F, -1.0076F, 0.9243F, 3.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0589F, 1.6522F, -1.0796F, 0.303F, -0.0393F, 0.1249F));

		PartDefinition cube_r7 = bow.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(64, 28).addBox(0.25F, -1.0076F, 0.9243F, 3.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0589F, 1.6522F, -1.0796F, 0.303F, 0.0393F, -0.1249F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 55).addBox(-1.0F, -3.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(56, 55).addBox(-1.0F, -3.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.25F))
		.texOffs(60, 0).addBox(-1.0F, 1.25F, -1.5F, 4.0F, 5.0F, 3.0F, new CubeDeformation(0.35F)), PartPose.offset(4.0F, 7.0F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 55).mirror().addBox(-2.0F, -3.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(56, 55).mirror().addBox(-2.0F, -3.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.25F)).mirror(false)
		.texOffs(60, 0).mirror().addBox(-3.0F, 1.25F, -1.5F, 4.0F, 5.0F, 3.0F, new CubeDeformation(0.35F)).mirror(false), PartPose.offset(-4.0F, 7.0F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(44, 55).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(56, 42).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.25F)), PartPose.offset(1.5F, 14.0F, 0.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(44, 55).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(56, 42).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.25F)).mirror(false), PartPose.offset(-1.5F, 14.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
