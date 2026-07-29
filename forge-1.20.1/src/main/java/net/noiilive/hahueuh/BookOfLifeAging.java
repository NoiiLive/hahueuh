package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.capability.PlayerDataEvents;

import java.util.UUID;

public final class BookOfLifeAging {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final long DAY_TICKS = 24000L;

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLifespan.ensureRolled(player);
            GateDefectiveState.ensureRolled(player);
            GateStrain.ensureRolled(player);
            PlayerStats.ensureRolled(player);
            processAgeUp(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            processAgeUp(player);
        }
    }

    private void processAgeUp(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        int currentDay = (int) (server.overworld().getDayTime() / DAY_TICKS);

        PlayerData data = PlayerData.get(player);
        int lastDay = data.getAgeLastDay();
        if (lastDay < 0) {
            data.setAgeLastDay(currentDay);
            PlayerDataEvents.sync(player);
            return;
        }

        if (!ConfigPlayer.AGING_ENABLED.get()) {
            data.setAgeLastDay(currentDay);
            PlayerDataEvents.sync(player);
            return;
        }

        if (HahUeuh.LIONS_HEART.isActiveOrPersisted(player.getUUID())) {
            data.setAgeLastDay(currentDay);
            PlayerDataEvents.sync(player);
            return;
        }

        int elapsedDays = currentDay - lastDay;
        if (elapsedDays <= 0) return;

        int interval = ConfigPlayer.AGE_UP_INTERVAL_DAYS.get();
        int agedUp = elapsedDays / interval;
        if (agedUp <= 0) return;

        data.setAge(data.getAge() + agedUp);
        data.setAgeLastDay(lastDay + agedUp * interval);
        PlayerDataEvents.sync(player);

        checkOldAge(player);
    }

    public static void checkOldAge(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        int lifespan = data.getLifespan();
        if (lifespan < 0) return;
        if (data.getAge() < lifespan) return;

        UUID uuid = player.getUUID();
        if (HahUeuh.SNAPSHOT_MANAGER.isDomainProtected(uuid)) {
            player.kill();
            return;
        }

        if (HahUeuh.SNAPSHOT_MANAGER.isReturnByDeathActive(uuid)) {
            HahUeuh.SNAPSHOT_MANAGER.forceNormalDeath(player);
            data.setAge(ConfigPlayer.STARTING_AGE.get());
            PlayerDataEvents.sync(player);
            player.displayClientMessage(Component.translatable("hahueuh.message.old_age_reset")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        MinecraftServer server = player.getServer();
        boolean hardcore = server != null && server.isHardcore();
        if (!hardcore) {
            data.setAge(ConfigPlayer.STARTING_AGE.get());
            PlayerDataEvents.sync(player);
            player.displayClientMessage(Component.translatable("hahueuh.message.old_age_reset")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        player.kill();
    }
}
