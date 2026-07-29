package net.noiilive.hahueuh.mixin;

import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(PlayerList.class)
public interface PlayerListAdvancementsAccessor {
    @Accessor("advancements")
    Map<UUID, PlayerAdvancements> hahueuh$getAdvancements();
}
