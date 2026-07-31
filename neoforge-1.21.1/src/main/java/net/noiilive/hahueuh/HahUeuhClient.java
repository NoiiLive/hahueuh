package net.noiilive.hahueuh;

import net.noiilive.hahueuh.client.AbilityClient;
import net.noiilive.hahueuh.client.AbilitySlots;
import net.noiilive.hahueuh.network.DeathFadeState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = HahUeuh.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public class HahUeuhClient {
    public HahUeuhClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        HahUeuh.LOGGER.info("HELLO FROM CLIENT SETUP");
        HahUeuh.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.DRAGON_SWORD_REID.get(),
                    ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sheathed"),
                    (stack, level, entity, seed) -> DragonSwordReidItem.isSheathed(stack) ? 1.0f : 0.0f);

            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.DRAGON_SWORD_REID.get(),
                    ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
        });
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FROZEN_OBJECT_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.WITCH_FACTOR.get(), net.noiilive.hahueuh.client.WitchFactorRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), net.noiilive.hahueuh.client.BlackHoleRenderer::new);
        event.registerEntityRenderer(ModEntities.YIN_SEAL.get(), net.noiilive.hahueuh.client.YinSealRenderer::new);
        event.registerEntityRenderer(ModEntities.MINYA_SPIKE.get(), net.noiilive.hahueuh.client.MinyaSpikeRenderer::new);
        event.registerEntityRenderer(ModEntities.MINYA_RING.get(), net.noiilive.hahueuh.client.MinyaRingRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.POCKET_VOID_BE.get(), net.noiilive.hahueuh.client.PocketVoidRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.BlackHoleModel.LAYER,
                net.noiilive.hahueuh.client.model.BlackHoleModel::createBodyLayer);
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.YinSealModel.LAYER,
                net.noiilive.hahueuh.client.model.YinSealModel::createBodyLayer);
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.MinyaSpikeModel.LAYER,
                net.noiilive.hahueuh.client.model.MinyaSpikeModel::createBodyLayer);
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.MinyaRingModel.LAYER,
                net.noiilive.hahueuh.client.model.MinyaRingModel::createBodyLayer);

        event.registerLayerDefinition(net.noiilive.hahueuh.client.EmmSwirlLayer.DEFAULT_LAYER,
                () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                        net.minecraft.client.model.PlayerModel.createMesh(
                                new net.minecraft.client.model.geom.builders.CubeDeformation(0.25f), false),
                        64, 64));
        event.registerLayerDefinition(net.noiilive.hahueuh.client.EmmSwirlLayer.SLIM_LAYER,
                () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                        net.minecraft.client.model.PlayerModel.createMesh(
                                new net.minecraft.client.model.geom.builders.CubeDeformation(0.25f), true),
                        64, 64));
    }

    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        addEmmSwirlLayer(event, net.minecraft.client.resources.PlayerSkin.Model.WIDE, false);
        addEmmSwirlLayer(event, net.minecraft.client.resources.PlayerSkin.Model.SLIM, true);
    }

    private static void addEmmSwirlLayer(EntityRenderersEvent.AddLayers event,
                                         net.minecraft.client.resources.PlayerSkin.Model skin, boolean slim) {
        if (!(event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer)) {
            return;
        }
        renderer.addLayer(new net.noiilive.hahueuh.client.EmmSwirlLayer(renderer, event.getEntityModels(), slim));
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "death_fade"),
                HahUeuhClient::renderDeathFade);

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ability_hud"),
                net.noiilive.hahueuh.client.AbilityHud::render);

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "mana_od_bar"),
                net.noiilive.hahueuh.client.ManaOdBarHud::render);

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sensory_deprivation"),
                net.noiilive.hahueuh.client.SensoryDeprivationClient::renderOverlay);
    }

    @SubscribeEvent
    static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        DeathFadeState.reset();
        net.noiilive.hahueuh.network.DomainRenderState.clear();
        net.noiilive.hahueuh.network.EmtRenderState.clear();
        net.noiilive.hahueuh.network.RemoteUnseenHands.clear();
        net.noiilive.hahueuh.network.ClientLionsHeartState.clear();
        net.noiilive.hahueuh.network.ClientLittleKingState.clear();
        net.noiilive.hahueuh.network.ClientFingerHighlightState.clear();
        net.noiilive.hahueuh.network.ClientMaterialPhaseState.clear();
        net.noiilive.hahueuh.network.ClientDualWieldState.clear();
        net.noiilive.hahueuh.api.AbilityCooldowns.reset();
        net.noiilive.hahueuh.client.MorningstarClient.reset();
        net.noiilive.hahueuh.client.GuiltywhipClient.reset();
        AbilitySlots.reset();
        AbilityClient.resetChargeManaState();
        net.noiilive.hahueuh.network.ClientMurakState.clear();
        net.noiilive.hahueuh.client.MurakClient.reset();
    }

    private static void renderDeathFade(GuiGraphics graphics, DeltaTracker deltaTracker) {
        float alpha = DeathFadeState.advanceAndGetAlpha();
        if (alpha <= 0.001f) return;
        int a = (int) (alpha * 255f) & 0xFF;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), a << 24);
    }
}
