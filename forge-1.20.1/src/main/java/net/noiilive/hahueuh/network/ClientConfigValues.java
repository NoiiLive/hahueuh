package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.ConfigMagic;
import net.noiilive.hahueuh.ConfigMagicYin;
import net.noiilive.hahueuh.ConfigPlayer;
import net.noiilive.hahueuh.ConfigSloth;

public final class ClientConfigValues {
    private ClientConfigValues() {}

    private static volatile ConfigSyncPacket synced;

    public static void accept(ConfigSyncPacket packet) {
        synced = packet;
    }

    public static void clear() {
        synced = null;
    }

    public static double alShamakRange() {
        ConfigSyncPacket p = synced;
        return p != null ? p.alShamakRange : ConfigMagicYin.AL_SHAMAK_RANGE.get();
    }

    public static double ulMinyaRange() {
        ConfigSyncPacket p = synced;
        return p != null ? p.ulMinyaRange : ConfigMagicYin.UL_MINYA_RANGE.get();
    }

    public static double murakFlightImpulse() {
        ConfigSyncPacket p = synced;
        return p != null ? p.murakFlightImpulse : ConfigMagicYin.MURAK_FLIGHT_IMPULSE.get();
    }

    public static double murakFlightDrag() {
        ConfigSyncPacket p = synced;
        return p != null ? p.murakFlightDrag : ConfigMagicYin.MURAK_FLIGHT_DRAG.get();
    }

    public static double murakGustStrength() {
        ConfigSyncPacket p = synced;
        return p != null ? p.murakGustStrength : ConfigMagicYin.MURAK_GUST_STRENGTH.get();
    }

    public static double murakFlightMaxSpeed() {
        ConfigSyncPacket p = synced;
        return p != null ? p.murakFlightMaxSpeed : ConfigMagicYin.MURAK_FLIGHT_MAX_SPEED.get();
    }

    public static int murakGustIntervalSeconds() {
        ConfigSyncPacket p = synced;
        return p != null ? p.murakGustIntervalSeconds : ConfigMagicYin.MURAK_GUST_INTERVAL_SECONDS.get();
    }

    public static double slothMaxDistance() {
        ConfigSyncPacket p = synced;
        return p != null ? p.slothMaxDistance : ConfigSloth.SLOTH_MAX_DISTANCE.get();
    }

    public static int gateStrainDamaged() {
        ConfigSyncPacket p = synced;
        return p != null ? p.gateStrainDamaged : ConfigMagic.GATE_STRAIN_DAMAGED.get();
    }

    public static int gateStrainDestroyed() {
        ConfigSyncPacket p = synced;
        return p != null ? p.gateStrainDestroyed : ConfigMagic.GATE_STRAIN_DESTROYED.get();
    }

    public static int statProgressPerLevel() {
        ConfigSyncPacket p = synced;
        return p != null ? p.statProgressPerLevel : ConfigPlayer.STAT_PROGRESS_PER_LEVEL.get();
    }
}
