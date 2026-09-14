package com.foliagui;

import com.foliagui.scheduler.Scheduler;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface FoliaGUIService {

    @NotNull Plugin plugin();

    @NotNull Scheduler scheduler();

    @NotNull NamespacedKey itemKey();
}
