package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Static shortcuts over {@link GuiNavigation}. {@link #open} uses the service the target GUI is bound
 * to; the remaining methods use the default service. For an explicit service use
 * {@code service.navigation()}.
 */
public final class GuiNavigator {

    private GuiNavigator() {
    }

    private static GuiNavigation navigation() {
        return FoliaGUI.service().navigation();
    }

    public static void open(@NotNull HumanEntity player, @NotNull BaseGui next) {
        next.service().navigation().open(player, next);
    }

    public static void maxDepth(int depth) {
        navigation().maxDepth(depth);
    }

    public static int depth(@NotNull HumanEntity player) {
        return FoliaGUI.isInitialised() ? navigation().depth(player) : 0;
    }

    public static void backOrClose(@NotNull HumanEntity player) {
        navigation().backOrClose(player);
    }

    public static boolean back(@NotNull HumanEntity player) {
        return FoliaGUI.isInitialised() && navigation().back(player);
    }

    public static boolean hasHistory(@NotNull HumanEntity player) {
        return FoliaGUI.isInitialised() && navigation().hasHistory(player);
    }

    public static void clear(@NotNull HumanEntity player) {
        if (FoliaGUI.isInitialised()) {
            navigation().clear(player);
        }
    }

    public static void clearAll() {
        if (FoliaGUI.isInitialised()) {
            navigation().clearAll();
        }
    }
}
