package com.foliagui.gui;

import com.foliagui.builder.gui.StorageGuiBuilder;
import com.foliagui.item.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StorageGui extends BaseGui {

    public StorageGui(int rows, @NotNull Component title) {
        super(rows, title);
        clearInteractionModifiers();
    }

    public static @NotNull StorageGuiBuilder builder() {
        return new StorageGuiBuilder();
    }

    public @NotNull ItemStack[] getStorageContents() {
        ItemStack[] raw = getInventory().getContents();
        ItemStack[] storage = new ItemStack[raw.length];
        for (int slot = 0; slot < raw.length; slot++) {
            if (getGuiItem(slot) == null) {
                storage[slot] = raw[slot];
            }
        }
        return storage;
    }

    public @NotNull List<ItemStack> getStoredItems() {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : getStorageContents()) {
            if (stack != null && !stack.getType().isAir()) {
                items.add(stack);
            }
        }
        return items;
    }

    public @NotNull StorageGui setStorageContents(@Nullable ItemStack @NotNull [] contents) {
        for (int slot = 0; slot < contents.length && slot < getSize(); slot++) {
            if (getGuiItem(slot) == null) {
                getInventory().setItem(slot, contents[slot]);
            }
        }
        return this;
    }
}
