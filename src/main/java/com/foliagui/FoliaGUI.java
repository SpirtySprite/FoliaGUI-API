package com.foliagui;

import com.foliagui.gui.AnvilGui;
import com.foliagui.gui.ChatPrompt;
import com.foliagui.gui.GuiManager;
import com.foliagui.gui.GuiNavigator;
import com.foliagui.gui.MerchantGui;
import com.foliagui.gui.SignGui;
import com.foliagui.listener.GuiListener;
import com.foliagui.scheduler.PaperFoliaScheduler;
import com.foliagui.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FoliaGUI {

    public static final String VERSION = readVersion();

    private static volatile Plugin plugin;
    private static volatile Scheduler scheduler;
    private static volatile NamespacedKey itemKey;
    private static volatile boolean initialised;
    private static Listener registeredListener;
    private static FoliaGUIService registeredService;

    private FoliaGUI() {
    }

    public static synchronized void init(@NotNull Plugin owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner plugin cannot be null");
        }
        if (initialised) {
            return;
        }
        plugin = owner;
        scheduler = new PaperFoliaScheduler(owner);
        itemKey = new NamespacedKey(owner, "foliagui-item");
        registeredListener = new GuiListener();
        owner.getServer().getPluginManager().registerEvents(registeredListener, owner);
        registeredService = new FoliaGUIService() {
            @Override
            public @NotNull Plugin plugin() {
                return FoliaGUI.plugin();
            }

            @Override
            public @NotNull Scheduler scheduler() {
                return FoliaGUI.scheduler();
            }

            @Override
            public @NotNull NamespacedKey itemKey() {
                return FoliaGUI.itemKey();
            }
        };
        Bukkit.getServicesManager().register(FoliaGUIService.class, registeredService, owner, ServicePriority.Normal);
        initialised = true;
    }

    public static synchronized void shutdown() {
        if (!initialised) {
            return;
        }
        GuiManager.closeAll();
        GuiManager.clearAll();
        AnvilGui.clearSessions();
        SignGui.clearSessions();
        MerchantGui.clearSessions();
        GuiNavigator.clearAll();
        ChatPrompt.clearAll();
        if (registeredListener != null) {
            HandlerList.unregisterAll(registeredListener);
        }
        if (registeredService != null) {
            Bukkit.getServicesManager().unregister(FoliaGUIService.class, registeredService);
        }
        plugin = null;
        scheduler = null;
        itemKey = null;
        registeredListener = null;
        registeredService = null;
        initialised = false;
    }

    public static boolean isInitialised() {
        return initialised;
    }

    public static @NotNull Plugin plugin() {
        ensureReady();
        return plugin;
    }

    public static @NotNull Scheduler scheduler() {
        ensureReady();
        return scheduler;
    }

    public static @NotNull NamespacedKey itemKey() {
        ensureReady();
        return itemKey;
    }

    private static void ensureReady() {
        if (!initialised) {
            throw new FoliaGUINotInitialisedException();
        }
    }

    private static @NotNull String readVersion() {
        try (InputStream in = FoliaGUI.class.getResourceAsStream("/foliagui-version.properties")) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "unknown");
        } catch (IOException e) {
            Logger.getLogger(FoliaGUI.class.getName()).log(Level.WARNING, "Failed to read FoliaGUI version metadata", e);
            return "unknown";
        }
    }
}
