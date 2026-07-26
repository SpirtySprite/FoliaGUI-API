package com.foliagui.gui;

import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;

public enum GuiType {

    WORKBENCH(InventoryType.WORKBENCH, 9),

    HOPPER(InventoryType.HOPPER, 5),

    DISPENSER(InventoryType.DISPENSER, 9),

    BREWING(InventoryType.BREWING, 5);

    private final InventoryType inventoryType;
    private final int size;

    GuiType(@NotNull InventoryType inventoryType, int size) {
        this.inventoryType = inventoryType;
        this.size = size;
    }

    public @NotNull InventoryType getInventoryType() {
        return inventoryType;
    }

    public int getSize() {
        return size;
    }
}
