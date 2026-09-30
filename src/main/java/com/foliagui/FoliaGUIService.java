package com.foliagui;

import com.foliagui.gui.GuiNavigation;
import com.foliagui.gui.GuiRegistry;
import com.foliagui.gui.GuiSessions;
import com.foliagui.gui.GuiTheme;
import com.foliagui.scheduler.Scheduler;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * One independent FoliaGUI instance, owned by one plugin.
 *
 * <p>Everything FoliaGUI remembers at runtime (open GUIs, navigation history, anvil, sign, merchant
 * and chat sessions, the theme, the event listener) lives in a service. Create one with
 * {@link FoliaGUI#create(Plugin)} and hand it to the GUIs you build, or call
 * {@link FoliaGUI#init(Plugin)} once and use the static helpers, which work on a default service.
 */
public interface FoliaGUIService {

    @NotNull Plugin plugin();

    @NotNull Scheduler scheduler();

    @NotNull NamespacedKey itemKey();

    @NotNull GuiTheme theme();

    void theme(@NotNull GuiTheme replacement);

    /** The GUIs this service currently has open, per player. */
    @NotNull GuiRegistry guis();

    /** Back-navigation history for this service's GUIs. */
    @NotNull GuiNavigation navigation();

    @ApiStatus.Internal
    @NotNull GuiSessions sessions();

    /**
     * Closes every open GUI, forgets all state, and unregisters the listener. Safe to call more than
     * once. GUIs bound to a closed service should not be opened again.
     */
    void close();

    boolean isClosed();
}
