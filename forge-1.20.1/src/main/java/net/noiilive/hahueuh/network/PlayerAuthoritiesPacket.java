package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.api.AuthorityRegistry;
import net.noiilive.hahueuh.api.OwnershipState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PlayerAuthoritiesPacket {
    private final List<String> ownedAuthorityIds;

    public PlayerAuthoritiesPacket(List<String> ownedAuthorityIds) {
        this.ownedAuthorityIds = ownedAuthorityIds;
    }

    public PlayerAuthoritiesPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.ownedAuthorityIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ownedAuthorityIds.add(buf.readUtf());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(ownedAuthorityIds.size());
        for (String id : ownedAuthorityIds) {
            buf.writeUtf(id);
        }
    }

    public static void handle(PlayerAuthoritiesPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            for (var authority : AuthorityRegistry.all()) {
                OwnershipState.setAuthorityOwned(authority.id(),
                        packet.ownedAuthorityIds.contains(authority.id().toString()));
            }
            for (String raw : packet.ownedAuthorityIds) {
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id != null) OwnershipState.setAuthorityOwned(id, true);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
