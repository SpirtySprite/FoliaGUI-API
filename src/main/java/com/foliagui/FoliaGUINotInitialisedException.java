package com.foliagui;

import org.bukkit.plugin.Plugin;

public final class FoliaGUINotInitialisedException extends IllegalStateException {

    FoliaGUINotInitialisedException() {
        super("FoliaGUI.init(plugin) must be called before using the library");
    }
}
