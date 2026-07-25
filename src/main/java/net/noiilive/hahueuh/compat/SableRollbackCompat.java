package net.noiilive.hahueuh.compat;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SableRollbackCompat {
    private static final String MOD_ID = "sable";

    private static Boolean available;

    private static Method getContainer;
    private static Method getAllSubLevels;
    private static Method removeSubLevel;
    private static Method processRemovals;
    private static Method getHoldingChunkMap;
    private static Method initialize;
    private static Method updateChunkStatus;
    private static Method processChanges;
    private static Method inBounds;
    private static Object unloadedReason;

    private SableRollbackCompat() {}

    public static boolean isPresent() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized boolean ensureResolved() {
        if (available != null) return available;
        if (!isPresent()) { available = false; return false; }
        try {
            Class<?> containerBase = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Class<?> serverContainer = Class.forName("dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer");
            Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
            Class<?> reasonClass = Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason");
            Class<?> holdingMapClass = Class.forName("dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap");

            getContainer = containerBase.getMethod("getContainer", ServerLevel.class);
            getAllSubLevels = containerBase.getMethod("getAllSubLevels");
            removeSubLevel = containerBase.getMethod("removeSubLevel", subLevelClass, reasonClass);
            processRemovals = containerBase.getMethod("processSubLevelRemovals");
            getHoldingChunkMap = serverContainer.getMethod("getHoldingChunkMap");
            initialize = serverContainer.getMethod("initialize");
            updateChunkStatus = holdingMapClass.getMethod("updateChunkStatus", ChunkPos.class, boolean.class);
            processChanges = holdingMapClass.getMethod("processChanges");
            inBounds = containerBase.getMethod("inBounds", ChunkPos.class);
            unloadedReason = Enum.valueOf((Class<? extends Enum>) reasonClass.asSubclass(Enum.class), "UNLOADED");
            available = true;
            HahUeuh.LOGGER.info("Sable rollback compat: API resolved — sub-level revert is active");
        } catch (Throwable t) {
            HahUeuh.LOGGER.warn("Sable is present but its rollback-compat API couldn't be resolved; "
                    + "physics sub-levels will NOT revert on rollback (Sable version mismatch?)", t);
            available = false;
        }
        return available;
    }

    public static void unloadAllSubLevels(MinecraftServer server) {
        if (server == null || !ensureResolved()) return;
        int unloaded = 0;
        try {
            for (ServerLevel level : server.getAllLevels()) {
                Object container = getContainer.invoke(null, level);
                if (container == null) continue;
                List<?> subLevels = new ArrayList<>((List<?>) getAllSubLevels.invoke(container));
                if (subLevels.isEmpty()) continue;
                for (Object subLevel : subLevels) {
                    removeSubLevel.invoke(container, subLevel, unloadedReason);
                    unloaded++;
                }
                processRemovals.invoke(container);
            }
            HahUeuh.LOGGER.info("Sable rollback compat: unloaded {} active sub-level(s) before the file restore", unloaded);
        } catch (Throwable t) {
            HahUeuh.LOGGER.error("Sable rollback compat failed while unloading sub-levels; physics objects "
                    + "may not revert this rollback (the core rollback continues regardless)", t);
        }
    }

    public static void reactivateSubLevels(ServerLevel level, Collection<ChunkPos> loadedChunks) {
        if (level == null || !ensureResolved()) return;
        try {
            Object container = getContainer.invoke(null, level);
            if (container == null) return;

            initialize.invoke(container);
            Object holdingMap = getHoldingChunkMap.invoke(container);
            if (holdingMap == null) return;

            int replayed = 0;
            for (ChunkPos pos : loadedChunks) {
                if (Boolean.TRUE.equals(inBounds.invoke(container, pos))) continue;
                updateChunkStatus.invoke(holdingMap, pos, true);
                replayed++;
            }
            processChanges.invoke(holdingMap);

            int active = ((List<?>) getAllSubLevels.invoke(container)).size();
            HahUeuh.LOGGER.info("Sable rollback compat: level={} replayed {} loaded chunk(s); {} sub-level(s) now active",
                    level.dimension().location(), replayed, active);
        } catch (Throwable t) {
            HahUeuh.LOGGER.error("Sable rollback compat failed while reactivating sub-levels in {}; physics objects "
                    + "may stay hidden until the area is reloaded (the core rollback continues regardless)",
                    level.dimension().location(), t);
        }
    }
}
