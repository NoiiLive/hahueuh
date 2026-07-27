package net.noiilive.hahueuh.menu;

import net.noiilive.hahueuh.ModItems;
import net.noiilive.hahueuh.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BookOfWisdomBindMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOT = 0;
    private static final int INV_START = 1;
    private static final int INV_END = 37;

    private final Container inputContainer = new SimpleContainer(1);

    public BookOfWisdomBindMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.BOOK_OF_WISDOM_BIND.get(), containerId);

        addSlot(new Slot(inputContainer, 0, 79, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.MEMORIES_OF_THE_WORLD.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 176));
        }
    }

    public ItemStack inputSlot() {
        return inputContainer.getItem(0);
    }

    public void setInputSlot(ItemStack stack) {
        inputContainer.setItem(0, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, inputContainer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == INPUT_SLOT) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (slots.get(INPUT_SLOT).getItem().isEmpty() && slots.get(INPUT_SLOT).mayPlace(stack)) {
            ItemStack single = stack.copy();
            single.setCount(1);
            stack.shrink(1);
            slots.get(INPUT_SLOT).set(single);
        } else if (index < 28) {
            if (!moveItemStackTo(stack, 28, INV_END, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, INV_START, 28, false)) return ItemStack.EMPTY;
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
