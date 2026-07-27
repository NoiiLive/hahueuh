package net.noiilive.hahueuh.magic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

public final class Spell {
    private final ResourceLocation id;
    private final ResourceLocation cooldownId;
    private final IntSupplier totalMana;
    private final IntSupplier manaPerTick;
    private final IntSupplier cooldownSeconds;
    private final Predicate<ServerPlayer> canCast;
    private final Consumer<ServerPlayer> onComplete;

    public Spell(ResourceLocation id, IntSupplier totalMana, IntSupplier manaPerTick, IntSupplier cooldownSeconds,
                 Predicate<ServerPlayer> canCast, Consumer<ServerPlayer> onComplete) {
        this.id = id;
        this.cooldownId = id;
        this.totalMana = totalMana;
        this.manaPerTick = manaPerTick;
        this.cooldownSeconds = cooldownSeconds;
        this.canCast = canCast;
        this.onComplete = onComplete;
    }

    public ResourceLocation id() { return id; }
    public ResourceLocation cooldownId() { return cooldownId; }
    public int totalMana() { return Math.max(1, totalMana.getAsInt()); }
    public int manaPerTick() { return Math.max(1, manaPerTick.getAsInt()); }
    public int cooldownSeconds() { return Math.max(0, cooldownSeconds.getAsInt()); }
    public boolean canCast(ServerPlayer player) { return canCast.test(player); }
    public void onComplete(ServerPlayer player) { onComplete.accept(player); }
}
