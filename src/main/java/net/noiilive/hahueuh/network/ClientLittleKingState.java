package net.noiilive.hahueuh.network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientLittleKingState {
    private static final Set<Integer> highlighted = ConcurrentHashMap.newKeySet();

    private ClientLittleKingState() {}

    public static void set(Iterable<Integer> ids) {
        highlighted.clear();
        for (Integer id : ids) highlighted.add(id);
    }

    public static Set<Integer> highlighted() {
        return highlighted;
    }

    public static boolean isEmpty() {
        return highlighted.isEmpty();
    }

    public static void clear() {
        highlighted.clear();
    }
}
