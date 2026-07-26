package com.foliagui.event;

import com.foliagui.gui.BaseGui;
import com.foliagui.item.GuiItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired for every click inside a FoliaGUI {@link BaseGui}, wrapping the underlying {@link InventoryClickEvent}
 * with the resolved {@link GuiItem} and owning GUI. Starts with whatever cancellation state the interaction
 * modifiers already decided; cancelling here is copied back onto the underlying event but doesn't stop
 * FoliaGUI's own slot/item actions from running, only the resulting item-movement.
 */
public class GuiClickEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final BaseGui gui;
    private final InventoryClickEvent inventoryClickEvent;
    private final GuiItem clickedItem;
    private boolean cancelled;

    public GuiClickEvent(@NotNull Player player, @NotNull BaseGui gui,
                          @NotNull InventoryClickEvent inventoryClickEvent, @Nullable GuiItem clickedItem) {
        this.player = player;
        this.gui = gui;
        this.inventoryClickEvent = inventoryClickEvent;
        this.clickedItem = clickedItem;
        this.cancelled = inventoryClickEvent.isCancelled();
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull BaseGui getGui() {
        return gui;
    }

    public @NotNull InventoryClickEvent getInventoryClickEvent() {
        return inventoryClickEvent;
    }

    /** Null if the clicked slot is empty. */
    public @Nullable GuiItem getClickedItem() {
        return clickedItem;
    }

    /** 0-indexed. */
    public int getSlot() {
        return inventoryClickEvent.getSlot();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
