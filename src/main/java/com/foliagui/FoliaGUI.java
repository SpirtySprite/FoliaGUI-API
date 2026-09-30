package com.foliagui;

import com.foliagui.gui.GuiTheme;
import com.foliagui.internal.DefaultFoliaGUIService;
import com.foliagui.item.GuiItem;
import com.foliagui.scheduler.Scheduler;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point of the library.
 *
 * <p>There are two ways to use it:
 * <ul>
 *   <li>{@link #create(Plugin)} returns an independent {@link FoliaGUIService}. Keep it, bind your GUIs
 *       to it with {@code builder.service(...)} or {@code gui.service(...)}, and call
 *       {@link FoliaGUIService#close()} when your plugin disables. Several plugins can do this side by
 *       side without sharing any state.</li>
 *   <li>{@link #init(Plugin)} creates one default service that every GUI without an explicit service
 *       uses, which keeps single-plugin setups to one line.</li>
 * </ul>
 */
public final class FoliaGUI {

    public static final String VERSION = readVersion();

    private static volatile FoliaGUIService defaultService;
    private static volatile GuiTheme pendingTheme = new GuiTheme();

    private FoliaGUI() {
    }

    public static @NotNull FoliaGUIService create(@NotNull Plugin owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner plugin cannot be null");
        }
        return new DefaultFoliaGUIService(owner);
    }

    public static synchronized void init(@NotNull Plugin owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner plugin cannot be null");
        }
        FoliaGUIService existing = defaultService;
        if (existing != null && !existing.isClosed()) {
            if (existing.plugin() != owner) {
                owner.getLogger().warning("FoliaGUI is already initialised for plugin '"
                        + existing.plugin().getName() + "'; this call from '" + owner.getName()
                        + "' was ignored. Use FoliaGUI.create(plugin) to get an independent instance.");
            }
            return;
        }
        FoliaGUIService created = create(owner);
        created.theme(pendingTheme);
        defaultService = created;
    }

    public static synchronized void shutdown() {
        FoliaGUIService service = defaultService;
        if (service == null) {
            return;
        }
        defaultService = null;
        service.close();
    }

    public static @NotNull FoliaGUIService service() {
        FoliaGUIService service = defaultService;
        if (service == null || service.isClosed()) {
            throw new FoliaGUINotInitialisedException();
        }
        return service;
    }

    public static boolean isInitialised() {
        FoliaGUIService service = defaultService;
        return service != null && !service.isClosed();
    }

    public static @NotNull GuiTheme theme() {
        FoliaGUIService service = defaultService;
        return service != null && !service.isClosed() ? service.theme() : pendingTheme;
    }

    public static void theme(@NotNull GuiTheme replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("theme cannot be null");
        }
        pendingTheme = replacement;
        FoliaGUIService service = defaultService;
        if (service != null && !service.isClosed()) {
            service.theme(replacement);
        }
    }

    public static @NotNull Plugin plugin() {
        return service().plugin();
    }

    public static @NotNull Scheduler scheduler() {
        return service().scheduler();
    }

    public static @NotNull NamespacedKey itemKey() {
        return GuiItem.IDENTITY_KEY;
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
