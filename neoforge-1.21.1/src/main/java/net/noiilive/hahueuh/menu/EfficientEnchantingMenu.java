package net.noiilive.hahueuh.menu;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModMenus;
import net.noiilive.hahueuh.network.EfficientEnchantOptionsPayload;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class EfficientEnchantingMenu extends AbstractContainerMenu {
    public static final int XP_COST = 3;
    public static final int LAPIS_COST = 3;
    private static final int ITEM_SLOT = 0;
    private static final int LAPIS_SLOT = 1;
    private static final int INV_START = 2;
    private static final int INV_END = 38;

    private final ContainerLevelAccess access;
    private final Player player;
    private final Container enchantSlots = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            EfficientEnchantingMenu.this.slotsChanged(this);
        }
    };

    private List<EfficientEnchantOptionsPayload.Option> clientOptions = List.of();

    public EfficientEnchantingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public EfficientEnchantingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenus.EFFICIENT_ENCHANTING.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;

        addSlot(new Slot(enchantSlots, 0, 15, 47) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(enchantSlots, 1, 35, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == enchantSlots && player instanceof ServerPlayer serverPlayer) {
            HahUeuh.EFFICIENT_ENCHANTING.pushOptions(serverPlayer, this);
        }
    }

    public ContainerLevelAccess access() {
        return access;
    }

    public ItemStack itemSlot() {
        return enchantSlots.getItem(ITEM_SLOT);
    }

    public void setItemSlot(ItemStack stack) {
        enchantSlots.setItem(ITEM_SLOT, stack);
    }

    public ItemStack lapisSlot() {
        return enchantSlots.getItem(LAPIS_SLOT);
    }

    public void setLapisSlot(ItemStack stack) {
        enchantSlots.setItem(LAPIS_SLOT, stack);
    }

    public void setClientOptions(List<EfficientEnchantOptionsPayload.Option> options) {
        this.clientOptions = options;
    }

    public List<EfficientEnchantOptionsPayload.Option> clientOptions() {
        return clientOptions;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Blocks.ENCHANTING_TABLE);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, enchantSlots));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == ITEM_SLOT || index == LAPIS_SLOT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.LAPIS_LAZULI)) {
            if (!moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (slots.get(ITEM_SLOT).getItem().isEmpty() && slots.get(ITEM_SLOT).mayPlace(stack)) {
            ItemStack single = stack.copy();
            single.setCount(1);
            stack.shrink(1);
            slots.get(ITEM_SLOT).set(single);
        } else if (index < 29) {
            if (!moveItemStackTo(stack, 29, INV_END, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, INV_START, 29, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }
}
