package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

public final class BookOfLifeAging {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final long DAY_TICKS = 24000L;

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLifespan.ensureRolled(player);
            if (!player.hasData(ModAttachments.PLAYER_OD_CURRENT.get())) {
                player.setData(ModAttachments.PLAYER_OD_CURRENT.get(), BookOfLifeStats.maxOd(player));
            }
            GateDefectiveState.ensureRolled(player);
            GateStrain.ensureRolled(player);
            processAgeUp(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            processAgeUp(player);
        }
    }

    private void processAgeUp(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        int currentDay = (int) (server.overworld().getDayTime() / DAY_TICKS);

        int lastDay = player.getData(ModAttachments.PLAYER_AGE_LAST_DAY.get());
        if (lastDay < 0) {
            if (!player.hasData(ModAttachments.PLAYER_AGE.get())) {
                player.setData(ModAttachments.PLAYER_AGE.get(), ConfigPlayer.STARTING_AGE.getAsInt());
            }
            player.setData(ModAttachments.PLAYER_AGE_LAST_DAY.get(), currentDay);
            return;
        }

        if (!ConfigPlayer.AGING_ENABLED.get()) {
            player.setData(ModAttachments.PLAYER_AGE_LAST_DAY.get(), currentDay);
            return;
        }

        if (HahUeuh.LIONS_HEART.isActiveOrPersisted(player.getUUID())) {
            player.setData(ModAttachments.PLAYER_AGE_LAST_DAY.get(), currentDay);
            return;
        }

        int elapsedDays = currentDay - lastDay;
        if (elapsedDays <= 0) return;

        int interval = ConfigPlayer.AGE_UP_INTERVAL_DAYS.getAsInt();
        int agedUp = elapsedDays / interval;
        if (agedUp <= 0) return;

        int newAge = player.getData(ModAttachments.PLAYER_AGE.get()) + agedUp;
        player.setData(ModAttachments.PLAYER_AGE.get(), newAge);
        player.setData(ModAttachments.PLAYER_AGE_LAST_DAY.get(), lastDay + agedUp * interval);

        checkOldAge(player);
    }

    public static void checkOldAge(ServerPlayer player) {
        int lifespan = player.getData(ModAttachments.PLAYER_LIFESPAN.get());
        if (lifespan < 0) return;
        int age = player.getData(ModAttachments.PLAYER_AGE.get());
        if (age < lifespan) return;

        UUID uuid = player.getUUID();
        if (HahUeuh.SNAPSHOT_MANAGER.isDomainProtected(uuid)) {
            player.kill();
            return;
        }

        if (HahUeuh.SNAPSHOT_MANAGER.isReturnByDeathActive(uuid)) {
            HahUeuh.SNAPSHOT_MANAGER.forceNormalDeath(player);
            player.setData(ModAttachments.PLAYER_AGE.get(), ConfigPlayer.STARTING_AGE.getAsInt());
            player.displayClientMessage(Component.translatable("hahueuh.message.old_age_reset")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        MinecraftServer server = player.getServer();
        boolean hardcore = server != null && server.isHardcore();
        if (!hardcore) {
            player.setData(ModAttachments.PLAYER_AGE.get(), ConfigPlayer.STARTING_AGE.getAsInt());
            player.displayClientMessage(Component.translatable("hahueuh.message.old_age_reset")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        player.kill();
    }
}
