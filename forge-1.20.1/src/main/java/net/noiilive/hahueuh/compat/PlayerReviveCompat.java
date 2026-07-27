package net.noiilive.hahueuh.compat;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class PlayerReviveCompat {
    public static final String MOD_ID = "playerrevive";
    private static final String BLEEDING_FLAG = "playerrevive:bleeding";

    private static Boolean loaded;
    private static Method reviveMethod;
    private static boolean reviveLookupDone;

    private PlayerReviveCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded(MOD_ID);
        }
        return loaded;
    }

    public static boolean isBleeding(Entity entity) {
        if (!isLoaded() || entity == null) return false;
        return entity.getPersistentData().getBoolean(BLEEDING_FLAG);
    }

    public static void forceRevive(ServerPlayer player) {
        if (player == null || !isBleeding(player)) return;
        Method revive = resolveReviveMethod();
        if (revive == null) return;
        try {
            revive.invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            HahUeuh.LOGGER.warn("PlayerRevive compat: failed to force-revive {}",
                    player.getGameProfile().getName(), e);
        }
    }

    private static Method resolveReviveMethod() {
        if (reviveLookupDone) return reviveMethod;
        reviveLookupDone = true;
        try {
            Class<?> serverClass = Class.forName("team.creative.playerrevive.server.PlayerReviveServer");
            reviveMethod = serverClass.getMethod("revive", net.minecraft.world.entity.player.Player.class);
        } catch (ReflectiveOperationException | RuntimeException e) {
            HahUeuh.LOGGER.warn("PlayerRevive compat: could not resolve PlayerReviveServer.revive — "
                    + "downed players will not be auto-revived on rollback", e);
        }
        return reviveMethod;
    }
}
