package com.foliagui.event;

import com.foliagui.gui.BaseGui;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a FoliaGUI {@link BaseGui} is about to open for a player, mirroring the underlying (also
 * cancellable) {@code InventoryOpenEvent}. Cancelling this event cancels the open.
 */
public class GuiOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final BaseGui gui;
    private boolean cancelled;

    public GuiOpenEvent(@NotNull Player player, @NotNull BaseGui gui) {
        this.player = player;
        this.gui = gui;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull BaseGui getGui() {
        return gui;
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
