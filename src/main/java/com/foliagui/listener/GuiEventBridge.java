package com.foliagui.listener;

import com.foliagui.event.GuiClickEvent;
import com.foliagui.event.GuiCloseEvent;
import com.foliagui.event.GuiOpenEvent;
import com.foliagui.gui.BaseGui;
import com.foliagui.item.GuiItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Fires the public {@link GuiOpenEvent}/{@link GuiClickEvent}/{@link GuiCloseEvent} alongside FoliaGUI's own
 * per-GUI callbacks, so other plugins can observe or veto activity through a normal Bukkit listener. Split
 * out of {@link GuiListener} purely to keep that class focused on the raw inventory events.
 */
final class GuiEventBridge {

    private GuiEventBridge() {
    }

    /** True if no listener cancelled the {@link GuiOpenEvent}. */
    static boolean fireOpen(Player player, BaseGui gui) {
        GuiOpenEvent event = new GuiOpenEvent(player, gui);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    /** True if the underlying click should end up cancelled. */
    static boolean fireClick(Player player, BaseGui gui, InventoryClickEvent event, @Nullable GuiItem item) {
        GuiClickEvent guiClickEvent = new GuiClickEvent(player, gui, event, item);
        Bukkit.getPluginManager().callEvent(guiClickEvent);
        return guiClickEvent.isCancelled();
    }

    static void fireClose(Player player, BaseGui gui) {
        Bukkit.getPluginManager().callEvent(new GuiCloseEvent(player, gui));
    }
}
