package net.noiilive.hahueuh.compat;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CreateRollbackCompat {
    private static final String MOD_ID = "create";

    private static Boolean available;

    private static Object railways;
    private static Method levelLoaded;
    private static Method playerLogin;
    private static Field trainsField;
    private static Constructor<?> removeTrainPacket;

    private CreateRollbackCompat() {}

    public static boolean isPresent() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    private static synchronized boolean ensureResolved() {
        if (available != null) return available;
        if (!isPresent()) { available = false; return false; }
        try {
            Class<?> createClass = Class.forName("com.simibubi.create.Create");
            Field railwaysField = createClass.getField("RAILWAYS");
            railways = railwaysField.get(null);
            levelLoaded = railways.getClass().getMethod("levelLoaded", LevelAccessor.class);
            playerLogin = railways.getClass().getMethod("playerLogin", Player.class);
            trainsField = railways.getClass().getField("trains");
            removeTrainPacket = Class.forName("com.simibubi.create.content.trains.entity.RemoveTrainPacket")
                    .getConstructor(UUID.class);
            available = true;
            HahUeuh.LOGGER.info("Create rollback compat: API resolved — train revert is active");
        } catch (Throwable t) {
            HahUeuh.LOGGER.warn("Create is present but its rollback-compat API couldn't be resolved; "
                    + "trains will NOT revert on rollback (Create version mismatch?)", t);
            available = false;
        }
        return available;
    }

    public static void reloadAndResync(MinecraftServer server) {
        if (server == null || !ensureResolved()) return;

        Set<UUID> staleIds = new HashSet<>();
        try {
            for (Object id : ((Map<?, ?>) trainsField.get(railways)).keySet()) {
                if (id instanceof UUID uuid) staleIds.add(uuid);
            }
            levelLoaded.invoke(railways, server.overworld());
        } catch (Throwable t) {
            HahUeuh.LOGGER.error("Create rollback compat failed while reloading trains "
                    + "(the core rollback continues regardless)", t);
            return;
        }

        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                for (UUID id : staleIds) {
                    if (removeTrainPacket.newInstance(id) instanceof CustomPacketPayload payload) {
                        PacketDistributor.sendToPlayer(player, payload);
                    }
                }
                playerLogin.invoke(railways, player);
            }
            HahUeuh.LOGGER.info("Create rollback compat: re-synced the railway ({} stale train(s) dropped) to {} player(s)",
                    staleIds.size(), server.getPlayerList().getPlayers().size());
        } catch (Throwable t) {
            HahUeuh.LOGGER.error("Create rollback compat failed while re-syncing railway data to clients "
                    + "(the core rollback continues regardless)", t);
        }
    }
}
