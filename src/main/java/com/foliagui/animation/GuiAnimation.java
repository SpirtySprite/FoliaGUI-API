package com.foliagui.animation;

import com.foliagui.FoliaGUI;
import com.foliagui.gui.BaseGui;
import com.foliagui.scheduler.TaskHandle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Repeating animation bound to a GUI and its viewer. Each frame runs on the player's region thread and
 * stops itself once the player is no longer viewing that GUI.
 */
public final class GuiAnimation {

    private GuiAnimation() {
    }

    /** Runs {@code frame} every {@code periodTicks} (minimum 1) ticks for as long as {@code player} has {@code gui} open. */
    public static @NotNull TaskHandle play(@NotNull BaseGui gui, @NotNull Player player, long periodTicks,
                                           @NotNull Consumer<BaseGui> frame) {
        // boxed so the task body can cancel its own handle once the GUI closes
        final TaskHandle[] handle = new TaskHandle[1];
        handle[0] = FoliaGUI.scheduler().runForEntityTimer(player, () -> {
            if (!isViewing(player, gui)) {
                if (handle[0] != null) {
                    handle[0].cancel();
                }
                return;
            }
            frame.accept(gui);
        }, () -> {
            if (handle[0] != null) {
                handle[0].cancel();
            }
        }, periodTicks, periodTicks);
        return handle[0];
    }

    private static boolean isViewing(@NotNull Player player, @NotNull BaseGui gui) {
        return player.getOpenInventory().getTopInventory().getHolder() == gui;
    }
}
