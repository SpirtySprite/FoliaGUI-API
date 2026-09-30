package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Static shortcuts over the default service's {@link GuiRegistry}. For an explicit service use
 * {@code service.guis()}.
 */
public final class GuiManager {

    private GuiManager() {
    }

    private static @Nullable GuiRegistry registry() {
        return FoliaGUI.isInitialised() ? FoliaGUI.service().guis() : null;
    }

    public static @Nullable BaseGui getOpenGui(@NotNull HumanEntity player) {
        GuiRegistry registry = registry();
        return registry == null ? null : registry.getOpenGui(player);
    }

    public static boolean hasGuiOpen(@NotNull HumanEntity player) {
        GuiRegistry registry = registry();
        return registry != null && registry.hasGuiOpen(player);
    }

    public static boolean hasAnyScreenOpen(@NotNull HumanEntity player) {
        GuiRegistry registry = registry();
        return registry != null && registry.hasAnyScreenOpen(player);
    }

    public static int openCount() {
        GuiRegistry registry = registry();
        return registry == null ? 0 : registry.openCount();
    }

    public static void refresh(@NotNull BaseGui gui) {
        gui.update();
    }

    public static void closeAll() {
        GuiRegistry registry = registry();
        if (registry != null) {
            registry.closeAll();
        }
    }

    public static @NotNull Collection<BaseGui> openGuis() {
        GuiRegistry registry = registry();
        return registry == null ? List.of() : registry.openGuis();
    }

    public static @NotNull List<Player> viewersOf(@NotNull BaseGui gui) {
        return gui.service().guis().viewersOf(gui);
    }

    public static @NotNull List<BaseGui> openGuisOfType(@NotNull Class<? extends BaseGui> type) {
        GuiRegistry registry = registry();
        return registry == null ? List.of() : registry.openGuisOfType(type);
    }

    public static void closeAll(@NotNull Predicate<BaseGui> filter) {
        GuiRegistry registry = registry();
        if (registry != null) {
            registry.closeAll(filter);
        }
    }

    public static void clearAll() {
        GuiRegistry registry = registry();
        if (registry != null) {
            registry.clearAll();
        }
    }
}
