package net.noiilive.hahueuh.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModEntities;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HahUeuhClientSetup {
    private HahUeuhClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                    net.noiilive.hahueuh.ModMenus.BOOK_OF_WISDOM_BIND.get(),
                    net.noiilive.hahueuh.client.gui.BookOfWisdomBindScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    net.noiilive.hahueuh.ModMenus.EFFICIENT_ENCHANTING.get(),
                    net.noiilive.hahueuh.client.gui.EfficientEnchantingScreen::new);

            net.minecraft.client.renderer.item.ItemProperties.register(
                    net.noiilive.hahueuh.ModItems.DRAGON_SWORD_REID.get(),
                    new net.minecraft.resources.ResourceLocation(HahUeuh.MODID, "sheathed"),
                    (stack, level, entity, seed) ->
                            net.noiilive.hahueuh.DragonSwordReidItem.isSheathed(stack) ? 1.0f : 0.0f);

            net.minecraft.client.renderer.item.ItemProperties.register(
                    net.noiilive.hahueuh.ModItems.DRAGON_SWORD_REID.get(),
                    new net.minecraft.resources.ResourceLocation(HahUeuh.MODID, "blocking"),
                    (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
        });
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.UnseenHandModel.LAYER,
                net.noiilive.hahueuh.client.model.UnseenHandModel::createBodyLayer);
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.UnseenTendrilModel.LAYER,
                net.noiilive.hahueuh.client.model.UnseenTendrilModel::createBodyLayer);
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.FootprintModel.LAYER,
                net.noiilive.hahueuh.client.model.FootprintModel::createBodyLayer);
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.MinyaSpikeModel.LAYER,
                net.noiilive.hahueuh.client.model.MinyaSpikeModel::createBodyLayer);
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.MinyaRingModel.LAYER,
                net.noiilive.hahueuh.client.model.MinyaRingModel::createBodyLayer);
        event.registerLayerDefinition(net.noiilive.hahueuh.client.model.BlackHoleModel.LAYER,
                net.noiilive.hahueuh.client.model.BlackHoleModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WITCH_FACTOR.get(), WitchFactorRenderer::new);
        event.registerEntityRenderer(ModEntities.FROZEN_OBJECT_PROJECTILE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.MINYA_SPIKE.get(), MinyaSpikeRenderer::new);
        event.registerEntityRenderer(ModEntities.MINYA_RING.get(), MinyaRingRenderer::new);
        event.registerBlockEntityRenderer(net.noiilive.hahueuh.ModBlocks.POCKET_VOID_BE.get(),
                net.noiilive.hahueuh.client.PocketVoidRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
    }
}
