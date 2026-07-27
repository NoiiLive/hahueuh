

public class chain_link<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "chain_link"), "main");
	private final ModelPart link;
	private final ModelPart chain1;
	private final ModelPart chain2;

	public chain_link(ModelPart root) {
		this.link = root.getChild("link");
		this.chain1 = this.link.getChild("chain1");
		this.chain2 = this.link.getChild("chain2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition link = partdefinition.addOrReplaceChild("link", CubeListBuilder.create(), PartPose.offset(0.0F, 20.65F, 0.0F));

		PartDefinition chain1 = link.addOrReplaceChild("chain1", CubeListBuilder.create(), PartPose.offset(0.0F, 3.35F, 0.0F));

		PartDefinition cube_r1 = chain1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(6, 2).addBox(0.375F, -0.625F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-0.625F, -1.625F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 4).addBox(-1.625F, -0.625F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 7).addBox(-0.625F, 0.375F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1768F, -2.1213F, 0.0F, 0.0F, 0.0F, -0.7854F));

		PartDefinition chain2 = link.addOrReplaceChild("chain2", CubeListBuilder.create(), PartPose.offset(0.0F, -3.3F, 0.0F));

		PartDefinition cube_r2 = chain2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(4, 4).addBox(-1.625F, -0.625F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 2).addBox(-0.625F, -1.625F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(6, 0).addBox(0.375F, -0.625F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 7).addBox(-0.625F, 0.375F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.1287F, 0.1768F, 1.5708F, -0.7854F, -1.5708F));

		return LayerDefinition.create(meshdefinition, 16, 16);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		link.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
