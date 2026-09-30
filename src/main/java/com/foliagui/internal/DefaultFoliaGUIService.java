package com.foliagui.internal;

import com.foliagui.FoliaGUIService;
import com.foliagui.gui.GuiNavigation;
import com.foliagui.gui.GuiRegistry;
import com.foliagui.gui.GuiSessions;
import com.foliagui.gui.GuiTheme;
import com.foliagui.item.GuiItem;
import com.foliagui.listener.GuiListener;
import com.foliagui.scheduler.PaperFoliaScheduler;
import com.foliagui.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class DefaultFoliaGUIService implements FoliaGUIService {

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final GuiRegistry guis;
    private final GuiNavigation navigation;
    private final GuiSessions sessions = new GuiSessions();
    private final GuiListener listener;
    private volatile GuiTheme theme = new GuiTheme();
    private volatile boolean closed;

    public DefaultFoliaGUIService(@NotNull Plugin owner) {
        this.plugin = Objects.requireNonNull(owner, "owner plugin cannot be null");
        this.scheduler = new PaperFoliaScheduler(owner);
        this.guis = new GuiRegistry(this);
        this.navigation = new GuiNavigation(this);
        this.listener = new GuiListener(this);
        owner.getServer().getPluginManager().registerEvents(listener, owner);
        Bukkit.getServicesManager().register(FoliaGUIService.class, this, owner, ServicePriority.Normal);
    }

    @Override
    public @NotNull Plugin plugin() {
        return plugin;
    }

    @Override
    public @NotNull Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public @NotNull NamespacedKey itemKey() {
        return GuiItem.IDENTITY_KEY;
    }

    @Override
    public @NotNull GuiTheme theme() {
        return theme;
    }

    @Override
    public void theme(@NotNull GuiTheme replacement) {
        this.theme = Objects.requireNonNull(replacement, "theme cannot be null");
    }

    @Override
    public @NotNull GuiRegistry guis() {
        return guis;
    }

    @Override
    public @NotNull GuiNavigation navigation() {
        return navigation;
    }

    @Override
    public @NotNull GuiSessions sessions() {
        return sessions;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        guis.closeAll();
        guis.clearAll();
        sessions.clearAll();
        navigation.clearAll();
        HandlerList.unregisterAll(listener);
        Bukkit.getServicesManager().unregister(FoliaGUIService.class, this);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
