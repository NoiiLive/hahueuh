package net.noiilive.hahueuh;

import net.noiilive.hahueuh.menu.BookOfWisdomBindMenu;
import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.noiilive.hahueuh.network.GreedVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BookOfWisdomCopy {
    private final Map<UUID, InteractionHand> openHand = new ConcurrentHashMap<>();
    private MinecraftServer server;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        openHand.clear();
        this.server = null;
    }

    public void openBindMenu(ServerPlayer player) {
        InteractionHand hand = handHolding(player, this::isUnboundCopy);
        if (hand == null) return;

        openHand.put(player.getUUID(), hand);
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new BookOfWisdomBindMenu(id, inv),
                Component.translatable("hahueuh.gui.book_of_wisdom.title")));
    }

    public void bind(ServerPlayer player, int abilityOrdinal) {
        BoundVisionAbility ability = BoundVisionAbility.byOrdinal(abilityOrdinal);
        if (ability == null) return;
        if (!(player.containerMenu instanceof BookOfWisdomBindMenu menu)) return;

        InteractionHand hand = openHand.get(player.getUUID());
        if (hand == null || !isUnboundCopy(player.getItemInHand(hand))) return;

        ItemStack inserted = menu.inputSlot();
        if (!isOwnMemories(inserted, player)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.book_of_wisdom_needs_memories")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(player.getUUID())
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(player.getUUID()) != GreedVariant.ECHIDNA) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        ItemStack copy = player.getItemInHand(hand).copy();
        copy.set(ModDataComponents.BOUND_VISION_ABILITY.get(), ability.ordinal());
        player.setItemInHand(hand, copy);

        menu.setInputSlot(ItemStack.EMPTY);
        if (!player.getInventory().add(inserted)) {
            player.drop(inserted, false);
        }

        player.closeContainer();
        openHand.remove(player.getUUID());
        player.displayClientMessage(Component.translatable("hahueuh.message.book_of_wisdom_bound",
                Component.translatable(ability.translationKey)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    public void activateBoundAbility(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.BOOK_OF_WISDOM_COPY.get())) continue;
            Integer bound = stack.get(ModDataComponents.BOUND_VISION_ABILITY.get());
            if (bound == null) continue;
            BoundVisionAbility ability = BoundVisionAbility.byOrdinal(bound);
            if (ability == null) continue;

            if (!player.isCreative()) {
                Long cooldownUntil = stack.get(ModDataComponents.VISION_COOLDOWN_UNTIL.get());
                long now = player.level().getGameTime();
                if (cooldownUntil != null && cooldownUntil > now) {
                    int seconds = (int) Math.ceil((cooldownUntil - now) / 20.0);
                    player.displayClientMessage(Component.translatable("hahueuh.message.book_of_wisdom_cooldown", seconds)
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
            }

            switch (ability) {
                case VISION_OF_LIFE -> HahUeuh.VISION_OF_LIFE.toggleViaBookOfWisdom(player);
                case VISION_OF_DANGER -> HahUeuh.VISION_OF_DANGER.toggleViaBookOfWisdom(player);
                case VISION_OF_INFORMATION -> { /* client-only GUI, nothing to do server-side */ }
            }
            return;
        }
    }

    public void startCooldownForWielder(ServerPlayer wielder, BoundVisionAbility ability) {
        if (wielder.isCreative()) return;
        int seconds = switch (ability) {
            case VISION_OF_LIFE -> ConfigGreed.VISION_OF_LIFE_COOLDOWN_SECONDS.getAsInt();
            case VISION_OF_DANGER -> ConfigGreed.VISION_OF_DANGER_COOLDOWN_SECONDS.getAsInt();
            case VISION_OF_INFORMATION -> 0;
        };
        if (seconds <= 0) return;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = wielder.getItemInHand(hand);
            if (!stack.is(ModItems.BOOK_OF_WISDOM_COPY.get())) continue;
            Integer bound = stack.get(ModDataComponents.BOUND_VISION_ABILITY.get());
            if (bound == null || bound != ability.ordinal()) continue;

            ItemStack updated = stack.copy();
            updated.set(ModDataComponents.VISION_COOLDOWN_UNTIL.get(), wielder.level().getGameTime() + seconds * 20L);
            wielder.setItemInHand(hand, updated);
            return;
        }
    }

    public boolean isHoldingBoundCopy(ServerPlayer player, BoundVisionAbility ability) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.BOOK_OF_WISDOM_COPY.get())) continue;
            Integer bound = stack.get(ModDataComponents.BOUND_VISION_ABILITY.get());
            if (bound != null && bound == ability.ordinal()) return true;
        }
        return false;
    }

    private boolean isUnboundCopy(ItemStack stack) {
        return stack.is(ModItems.BOOK_OF_WISDOM_COPY.get())
                && stack.get(ModDataComponents.BOUND_VISION_ABILITY.get()) == null;
    }

    private boolean isOwnMemories(ItemStack stack, ServerPlayer player) {
        return stack.is(ModItems.MEMORIES_OF_THE_WORLD.get())
                && player.getUUID().equals(stack.get(ModDataComponents.BOOK_OWNER.get()));
    }

    private InteractionHand handHolding(ServerPlayer player, java.util.function.Predicate<ItemStack> filter) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (filter.test(player.getItemInHand(hand))) return hand;
        }
        return null;
    }
}
