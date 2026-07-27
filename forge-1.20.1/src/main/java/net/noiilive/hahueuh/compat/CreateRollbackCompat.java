package net.noiilive.hahueuh.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.noiilive.hahueuh.HahUeuh;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CreateRollbackCompat {
    private static final String MOD_ID = "create";

    private static Boolean available;

    private static Object railways;
    private static Method levelLoaded;
    private static Method playerLogin;
    private static Field trainsField;
    private static Constructor<?> trainPacket;
    private static Method getChannel;

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

            Class<?> trainClass = Class.forName("com.simibubi.create.content.trains.entity.Train");
            trainPacket = Class.forName("com.simibubi.create.content.trains.entity.TrainPacket")
                    .getConstructor(trainClass, boolean.class);
            getChannel = Class.forName("com.simibubi.create.AllPackets").getMethod("getChannel");

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

        List<Object> staleTrains = new ArrayList<>();
        try {
            staleTrains.addAll(((Map<?, ?>) trainsField.get(railways)).values());
            levelLoaded.invoke(railways, server.overworld());
        } catch (Throwable t) {
            HahUeuh.LOGGER.error("Create rollback compat failed while reloading trains "
                    + "(the core rollback continues regardless)", t);
            return;
        }

        try {
            SimpleChannel channel = (SimpleChannel) getChannel.invoke(null);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                for (Object train : staleTrains) {
                    channel.send(PacketDistributor.PLAYER.with(() -> player), trainPacket.newInstance(train, false));
                }
                playerLogin.invoke(railways, player);
            }
            HahUeuh.LOGGER.info("Create rollback compat: re-synced the railway ({} stale train(s) dropped) to {} player(s)",
                    staleTrains.size(), server.getPlayerList().getPlayers().size());
        } catch (Throwable t) {
            HahUeuh.LOGGER.error("Create rollback compat failed while re-syncing railway data to clients "
                    + "(the core rollback continues regardless)", t);
        }
    }
}
