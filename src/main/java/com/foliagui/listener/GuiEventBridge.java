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

final class GuiEventBridge {

    private GuiEventBridge() {
    }

    static boolean fireOpen(Player player, BaseGui gui) {
        GuiOpenEvent event = new GuiOpenEvent(player, gui);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    static boolean fireClick(Player player, BaseGui gui, InventoryClickEvent event, @Nullable GuiItem item) {
        GuiClickEvent guiClickEvent = new GuiClickEvent(player, gui, event, item);
        Bukkit.getPluginManager().callEvent(guiClickEvent);
        return guiClickEvent.isCancelled();
    }

    static void fireClose(Player player, BaseGui gui) {
        Bukkit.getPluginManager().callEvent(new GuiCloseEvent(player, gui));
    }
}
