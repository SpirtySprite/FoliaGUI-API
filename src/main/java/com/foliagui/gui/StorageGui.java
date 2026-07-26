package com.foliagui.gui;

import com.foliagui.builder.gui.StorageGuiBuilder;
import com.foliagui.item.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets players freely move items in and out. All interaction modifiers are cleared by default, so unlike
 * a {@link Gui} it does not lock its slots. Persist {@link #getStorageContents()} yourself if it needs to
 * survive a restart.
 */
public class StorageGui extends BaseGui {

    public StorageGui(int rows, @NotNull Component title) {
        super(rows, title);
        clearInteractionModifiers();
    }

    public static @NotNull StorageGuiBuilder builder() {
        return new StorageGuiBuilder();
    }

    /** Contents of every slot that is not a fixed GUI item; indexes match inventory slots. */
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

    /** Places raw storage items directly into the inventory (bypassing the GUI item map). */
    public @NotNull StorageGui setStorageContents(@Nullable ItemStack @NotNull [] contents) {
        for (int slot = 0; slot < contents.length && slot < getSize(); slot++) {
            if (getGuiItem(slot) == null) {
                getInventory().setItem(slot, contents[slot]);
            }
        }
        return this;
    }
}
