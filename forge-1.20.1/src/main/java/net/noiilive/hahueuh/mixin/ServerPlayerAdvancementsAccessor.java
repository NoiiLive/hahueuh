package net.noiilive.hahueuh.mixin;

import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAdvancementsAccessor {
    @Mutable
    @Accessor("advancements")
    void hahueuh$setAdvancements(PlayerAdvancements value);
}
