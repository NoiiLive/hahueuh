

public class puck<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "puck"), "main");
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart strap;
	private final ModelPart pouch;
	private final ModelPart tail_1;
	private final ModelPart tail_2;
	private final ModelPart tail_3;
	private final ModelPart left_arm;
	private final ModelPart right_arm;
	private final ModelPart left_leg;
	private final ModelPart right_leg;

	public puck(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.strap = this.body.getChild("strap");
		this.pouch = this.strap.getChild("pouch");
		this.tail_1 = this.body.getChild("tail_1");
		this.tail_2 = this.tail_1.getChild("tail_2");
		this.tail_3 = this.tail_2.getChild("tail_3");
		this.left_arm = root.getChild("left_arm");
		this.right_arm = root.getChild("right_arm");
		this.left_leg = root.getChild("left_leg");
		this.right_leg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 0).addBox(-2.0F, -4.0F, -2.5F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.1F))
		.texOffs(16, 31).addBox(2.0F, -3.0F, -1.5F, 1.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(0, 33).addBox(-3.0F, -3.0F, -1.5F, 1.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(30, 18).addBox(-1.5F, -2.0F, -3.5F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(18, 24).addBox(0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(8, 31).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, -3.75F, 0.5F, 0.0F, 0.0F, 0.7854F));

		PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(32, 21).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.75F, -4.0F, 0.75F, -0.8375F, 0.639F, -1.165F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition cube_r3 = body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -4.0F, -8.0F, 3.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(22, 21).addBox(-1.5F, -3.0F, -8.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(0.0F, 8.0F, -2.25F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r4 = body.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5F, -3.0F, -8.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(0.0F, 11.0F, -2.25F, -1.5708F, 0.0F, 0.0F));

		PartDefinition strap = body.addOrReplaceChild("strap", CubeListBuilder.create(), PartPose.offset(1.5F, 4.75F, -0.5F));

		PartDefinition cube_r5 = strap.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 12).addBox(-1.5F, -4.0F, -8.0F, 3.0F, 4.0F, 8.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(-1.5F, 3.25F, -1.75F, -1.5708F, 0.0F, 0.0F));

		PartDefinition pouch = strap.addOrReplaceChild("pouch", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r6 = pouch.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(22, 8).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 1.0F, 0.0F, -1.6374F, 0.2079F, -0.3124F));

		PartDefinition tail_1 = body.addOrReplaceChild("tail_1", CubeListBuilder.create().texOffs(0, 29).addBox(-0.5F, -0.5F, -0.25F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.0F, 1.75F));

		PartDefinition tail_2 = tail_1.addOrReplaceChild("tail_2", CubeListBuilder.create().texOffs(30, 14).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 2.75F));

		PartDefinition tail_3 = tail_2.addOrReplaceChild("tail_3", CubeListBuilder.create().texOffs(30, 26).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(18, 26).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 13.0F, -0.25F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 26).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 13.0F, -0.25F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(22, 14).addBox(0.0F, -0.75F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(30, 30).addBox(0.0F, 3.25F, -0.5F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.25F, 17.75F, -0.25F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(10, 24).addBox(-1.0F, -0.75F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(32, 8).addBox(-1.0F, 3.25F, -0.5F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.25F, 17.75F, -0.25F));

		return LayerDefinition.create(meshdefinition, 64, 64);
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
