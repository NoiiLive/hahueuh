package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.magic.SpellRegistry;

import java.util.function.Supplier;

public class CastSpellPacket {
    private final ResourceLocation spellId;

    public CastSpellPacket(ResourceLocation spellId) {
        this.spellId = spellId;
    }

    public CastSpellPacket(FriendlyByteBuf buf) {
        this.spellId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(spellId);
    }

    public static void handle(CastSpellPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                SpellRegistry.get(packet.spellId)
                        .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(sender, spell));
            }
        });
        ctx.setPacketHandled(true);
    }
}
