package com.foliagui;

import com.foliagui.scheduler.Scheduler;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * ServicesManager-friendly view of the running {@link FoliaGUI} instance, for consumers who'd rather
 * look this up as a service than use the static singleton directly. Registered by {@link FoliaGUI#init(Plugin)}.
 */
public interface FoliaGUIService {

    @NotNull Plugin plugin();

    @NotNull Scheduler scheduler();

    @NotNull NamespacedKey itemKey();
}
