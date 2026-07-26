package com.foliagui.event;

import com.foliagui.gui.BaseGui;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player's FoliaGUI {@link BaseGui} closes. Not cancellable, matching vanilla
 * {@code InventoryCloseEvent}. See {@link BaseGui#setForceOpen} to keep a player in a dialog.
 */
public class GuiCloseEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final BaseGui gui;

    public GuiCloseEvent(@NotNull Player player, @NotNull BaseGui gui) {
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
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
