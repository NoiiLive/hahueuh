package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.ConfigMagic;
import net.noiilive.hahueuh.ConfigMagicYin;
import net.noiilive.hahueuh.ConfigPlayer;
import net.noiilive.hahueuh.ConfigSloth;

import java.util.function.Supplier;

public class ConfigSyncPacket {
    public final double alShamakRange;
    public final double ulMinyaRange;
    public final double murakFlightImpulse;
    public final double murakFlightDrag;
    public final double murakGustStrength;
    public final double murakFlightMaxSpeed;
    public final int murakGustIntervalSeconds;
    public final double slothMaxDistance;
    public final int gateStrainDamaged;
    public final int gateStrainDestroyed;
    public final int statProgressPerLevel;

    public ConfigSyncPacket(double alShamakRange, double ulMinyaRange, double murakFlightImpulse,
                            double murakFlightDrag, double murakGustStrength, double murakFlightMaxSpeed,
                            int murakGustIntervalSeconds, double slothMaxDistance,
                            int gateStrainDamaged, int gateStrainDestroyed, int statProgressPerLevel) {
        this.alShamakRange = alShamakRange;
        this.ulMinyaRange = ulMinyaRange;
        this.murakFlightImpulse = murakFlightImpulse;
        this.murakFlightDrag = murakFlightDrag;
        this.murakGustStrength = murakGustStrength;
        this.murakFlightMaxSpeed = murakFlightMaxSpeed;
        this.murakGustIntervalSeconds = murakGustIntervalSeconds;
        this.slothMaxDistance = slothMaxDistance;
        this.gateStrainDamaged = gateStrainDamaged;
        this.gateStrainDestroyed = gateStrainDestroyed;
        this.statProgressPerLevel = statProgressPerLevel;
    }

    public static ConfigSyncPacket current() {
        return new ConfigSyncPacket(
                ConfigMagicYin.AL_SHAMAK_RANGE.get(),
                ConfigMagicYin.UL_MINYA_RANGE.get(),
                ConfigMagicYin.MURAK_FLIGHT_IMPULSE.get(),
                ConfigMagicYin.MURAK_FLIGHT_DRAG.get(),
                ConfigMagicYin.MURAK_GUST_STRENGTH.get(),
                ConfigMagicYin.MURAK_FLIGHT_MAX_SPEED.get(),
                ConfigMagicYin.MURAK_GUST_INTERVAL_SECONDS.get(),
                ConfigSloth.SLOTH_MAX_DISTANCE.get(),
                ConfigMagic.GATE_STRAIN_DAMAGED.get(),
                ConfigMagic.GATE_STRAIN_DESTROYED.get(),
                ConfigPlayer.STAT_PROGRESS_PER_LEVEL.get());
    }

    public ConfigSyncPacket(FriendlyByteBuf buf) {
        this.alShamakRange = buf.readDouble();
        this.ulMinyaRange = buf.readDouble();
        this.murakFlightImpulse = buf.readDouble();
        this.murakFlightDrag = buf.readDouble();
        this.murakGustStrength = buf.readDouble();
        this.murakFlightMaxSpeed = buf.readDouble();
        this.murakGustIntervalSeconds = buf.readVarInt();
        this.slothMaxDistance = buf.readDouble();
        this.gateStrainDamaged = buf.readVarInt();
        this.gateStrainDestroyed = buf.readVarInt();
        this.statProgressPerLevel = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(alShamakRange);
        buf.writeDouble(ulMinyaRange);
        buf.writeDouble(murakFlightImpulse);
        buf.writeDouble(murakFlightDrag);
        buf.writeDouble(murakGustStrength);
        buf.writeDouble(murakFlightMaxSpeed);
        buf.writeVarInt(murakGustIntervalSeconds);
        buf.writeDouble(slothMaxDistance);
        buf.writeVarInt(gateStrainDamaged);
        buf.writeVarInt(gateStrainDestroyed);
        buf.writeVarInt(statProgressPerLevel);
    }

    public static void handle(ConfigSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientConfigValues.accept(packet)));
        ctx.setPacketHandled(true);
    }
}
