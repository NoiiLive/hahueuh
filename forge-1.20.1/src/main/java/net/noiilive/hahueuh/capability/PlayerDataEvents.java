package net.noiilive.hahueuh.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.network.PlayerDataSyncPacket;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID)
public final class PlayerDataEvents {
    private static final ResourceLocation PLAYER_DATA_ID = new ResourceLocation(HahUeuh.MODID, "player_data");
    private static final ResourceLocation CHUNK_DATA_ID = new ResourceLocation(HahUeuh.MODID, "chunk_data");
    private static final ResourceLocation MOB_WITCH_FACTOR_ID = new ResourceLocation(HahUeuh.MODID, "mob_witch_factor");

    private PlayerDataEvents() {}

    @SubscribeEvent
    public static void onAttachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player && !event.getCapabilities().containsKey(PLAYER_DATA_ID)) {
            event.addCapability(PLAYER_DATA_ID, new PlayerDataProvider());
        }
        if (event.getObject() instanceof net.minecraft.world.entity.Mob
                && !event.getCapabilities().containsKey(MOB_WITCH_FACTOR_ID)) {
            event.addCapability(MOB_WITCH_FACTOR_ID, new MobWitchFactorProvider());
        }
    }

    @SubscribeEvent
    public static void onAttachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getCapabilities().containsKey(CHUNK_DATA_ID)) {
            event.addCapability(CHUNK_DATA_ID, new ChunkDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            PlayerData from = PlayerData.getOrDefault(original);
            event.getEntity().getCapability(ModCapabilities.PLAYER_DATA)
                    .ifPresent(to -> to.copyFrom(from));
        } finally {
            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            net.noiilive.hahueuh.GateStrain.ensureRolled(player);
            net.noiilive.hahueuh.PlayerLifespan.ensureRolled(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer tracked
                && event.getEntity() instanceof ServerPlayer viewer) {
            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                    new PlayerDataSyncPacket(tracked.getUUID(), PlayerData.get(tracked).serializeNBT()));
        }
    }

    public static void sync(ServerPlayer player) {
        PlayerDataSyncPacket packet =
                new PlayerDataSyncPacket(player.getUUID(), PlayerData.get(player).serializeNBT());
        ModNetworking.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }
}
