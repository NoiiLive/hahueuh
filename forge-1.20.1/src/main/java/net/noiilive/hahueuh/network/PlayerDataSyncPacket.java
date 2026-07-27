package net.noiilive.hahueuh.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PlayerDataSyncPacket {
    private final UUID playerUuid;
    private final CompoundTag data;

    public PlayerDataSyncPacket(UUID playerUuid, CompoundTag data) {
        this.playerUuid = playerUuid;
        this.data = data;
    }

    public PlayerDataSyncPacket(FriendlyByteBuf buf) {
        this.playerUuid = buf.readUUID();
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUuid);
        buf.writeNbt(data);
    }

    public static void handle(PlayerDataSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPlayerData.accept(packet.playerUuid, packet.data)));
        ctx.setPacketHandled(true);
    }
}
