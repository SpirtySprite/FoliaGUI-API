package com.foliagui.item;

import org.bukkit.event.Event;

/** A callback fired for a GUI interaction, parameterized on the triggering Bukkit event (usually {@code InventoryClickEvent}). */
@FunctionalInterface
public interface GuiAction<T extends Event> {

    void execute(T event);
}
