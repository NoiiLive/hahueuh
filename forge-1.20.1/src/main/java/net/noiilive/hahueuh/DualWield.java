package net.noiilive.hahueuh;

import net.noiilive.hahueuh.mixin.AttackStrengthTickerAccessor;
import net.noiilive.hahueuh.network.AttackStrengthSyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.noiilive.hahueuh.network.ModNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DualWield {
    private static final int MAIN = 0;
    private static final int OFF = 1;
    private static final int IDLE_TICKS = 1000;

    private static final class Grip {
        final long[] lastSwing;

        Grip(long now) {
            lastSwing = new long[]{now - IDLE_TICKS, now - IDLE_TICKS};
        }
    }

    private final Map<UUID, Grip> grips = new ConcurrentHashMap<>();
    private final Set<UUID> striking = ConcurrentHashMap.newKeySet();

    private record Mod(Attribute attribute, AttributeModifier modifier) {}

    public static boolean isDualWielding(Player player) {
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        Item kukri = ModItems.BOWEL_HUNTER_KUKRI.get();
        Item white = ModItems.BOWEL_HUNTER_WHITE_BLADE.get();
        Item black = ModItems.BOWEL_HUNTER_BLACK_BLADE.get();
        if (main == kukri && off == kukri) return true;
        return (main == white && off == black) || (main == black && off == white);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (striking.contains(player.getUUID())) return;
        if (!isDualWielding(player)) return;

        event.setCanceled(true);
        strike(player, event.getTarget());
    }

    private void strike(ServerPlayer player, Entity target) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        long now = server.getTickCount();
        Grip grip = grips.computeIfAbsent(player.getUUID(), uuid -> new Grip(now));

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        int hand = player.swingingArm == InteractionHand.OFF_HAND ? OFF : MAIN;
        int charge = (int) Math.min(now - grip.lastSwing[hand], IDLE_TICKS);

        if (hand == MAIN) {
            attackWith(player, target, charge);
        } else {
            attackWithOffhand(player, target, main, off, charge);
        }
        grip.lastSwing[hand] = now;

        syncIndicator(player, main, off, grip, now, hand);
    }

    private void attackWith(ServerPlayer player, Entity target, int charge) {
        ((AttackStrengthTickerAccessor) player).hahueuh$setAttackStrengthTicker(charge);
        UUID uuid = player.getUUID();
        striking.add(uuid);
        try {
            player.attack(target);
        } finally {
            striking.remove(uuid);
            if (target.invulnerableTime > INVULNERABILITY_TICKS) {
                target.invulnerableTime = INVULNERABILITY_TICKS;
            }
        }
    }

    private void attackWithOffhand(ServerPlayer player, Entity target, ItemStack main, ItemStack off, int charge) {
        List<Mod> mainMods = mainHandModifiers(main);
        List<Mod> offMods = mainHandModifiers(off);

        removeModifiers(player, mainMods);
        applyModifiers(player, offMods);
        player.setItemInHand(InteractionHand.MAIN_HAND, off);
        player.setItemInHand(InteractionHand.OFF_HAND, main);
        try {
            attackWith(player, target, charge);
        } finally {
            ItemStack swung = player.getMainHandItem();
            ItemStack idle = player.getOffhandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, idle);
            player.setItemInHand(InteractionHand.OFF_HAND, swung);
            removeModifiers(player, offMods);
            applyModifiers(player, mainMods);
        }
    }

    private void syncIndicator(ServerPlayer player, ItemStack main, ItemStack off, Grip grip, long now, int justUsed) {
        int next = justUsed == OFF ? MAIN : OFF;
        boolean nextIsOff = next == OFF;
        float ready = readiness(nextIsOff ? off : main, now - grip.lastSwing[next]);
        ModNetworking.sendToPlayer(player, new AttackStrengthSyncPacket(nextIsOff, ready));
    }

    public static void applyIndicator(Player player, float scale) {
        float ticks = scale * player.getCurrentItemAttackStrengthDelay();
        ((AttackStrengthTickerAccessor) player).hahueuh$setAttackStrengthTicker(Math.round(ticks));
    }

    private static float readiness(ItemStack weapon, long ticksSince) {
        return Mth.clamp((ticksSince + 0.5f) / attackDelayTicks(weapon), 0f, 1f);
    }

    public static float attackDelayTicks(ItemStack weapon) {
        double[] speed = {Attributes.ATTACK_SPEED.getDefaultValue()};
        weapon.getAttributeModifiers(EquipmentSlot.MAINHAND).forEach((attribute, modifier) -> {
            if (attribute == Attributes.ATTACK_SPEED
                    && modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                speed[0] += modifier.getAmount();
            }
        });
        return (float) (20.0 / Math.max(0.1, speed[0]));
    }

    private static List<Mod> mainHandModifiers(ItemStack stack) {
        List<Mod> mods = new ArrayList<>();
        stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .forEach((attribute, modifier) -> mods.add(new Mod(attribute, modifier)));
        return mods;
    }

    private static void removeModifiers(ServerPlayer player, List<Mod> mods) {
        for (Mod mod : mods) {
            AttributeInstance instance = player.getAttributes().getInstance(mod.attribute());
            if (instance != null) instance.removeModifier(mod.modifier().getId());
        }
    }

    private static void applyModifiers(ServerPlayer player, List<Mod> mods) {
        for (Mod mod : mods) {
            AttributeInstance instance = player.getAttributes().getInstance(mod.attribute());
            if (instance != null) {
                instance.removeModifier(mod.modifier().getId());
                instance.addTransientModifier(mod.modifier());
            }
        }
    }

    private static final int INVULNERABILITY_TICKS = 15;

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        grips.remove(uuid);
        striking.remove(uuid);
    }
}
