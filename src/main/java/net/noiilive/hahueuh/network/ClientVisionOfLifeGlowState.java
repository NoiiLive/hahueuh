package net.noiilive.hahueuh.network;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientVisionOfLifeGlowState {
    public enum Category { HOSTILE, PASSIVE, PLAYER, WITCH_FACTOR }

    private static final Set<Integer> hostile = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> passive = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> player = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> witchFactor = ConcurrentHashMap.newKeySet();

    private ClientVisionOfLifeGlowState() {}

    public static void set(List<Integer> hostileIds, List<Integer> passiveIds, List<Integer> playerIds,
                            List<Integer> witchFactorIds) {
        hostile.clear();
        hostile.addAll(hostileIds);
        passive.clear();
        passive.addAll(passiveIds);
        player.clear();
        player.addAll(playerIds);
        witchFactor.clear();
        witchFactor.addAll(witchFactorIds);
    }

    public static Set<Integer> hostile() {
        return hostile;
    }

    public static Set<Integer> passive() {
        return passive;
    }

    public static Set<Integer> player() {
        return player;
    }

    public static Set<Integer> witchFactor() {
        return witchFactor;
    }

    public static boolean isEmpty() {
        return hostile.isEmpty() && passive.isEmpty() && player.isEmpty() && witchFactor.isEmpty();
    }

    /** {@code null} if this entity isn't currently tracked by Vision of Life at all. Holding a Witch Factor
     *  takes priority over any other category — the server only ever puts an entity's id in one of these
     *  four lists to begin with, but checking it first keeps that priority explicit here too. */
    public static Category categoryOf(int entityId) {
        if (witchFactor.contains(entityId)) return Category.WITCH_FACTOR;
        if (hostile.contains(entityId)) return Category.HOSTILE;
        if (passive.contains(entityId)) return Category.PASSIVE;
        if (player.contains(entityId)) return Category.PLAYER;
        return null;
    }

    public static void clear() {
        hostile.clear();
        passive.clear();
        player.clear();
        witchFactor.clear();
    }
}
