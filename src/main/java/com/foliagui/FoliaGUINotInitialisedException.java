package com.foliagui;

import org.bukkit.plugin.Plugin;

/** Thrown when FoliaGUI is used before {@link FoliaGUI#init(Plugin)} (or after {@link FoliaGUI#shutdown()}). */
public final class FoliaGUINotInitialisedException extends IllegalStateException {

    FoliaGUINotInitialisedException() {
        super("FoliaGUI.init(plugin) must be called before using the library");
    }
}
