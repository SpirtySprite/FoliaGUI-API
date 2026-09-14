package com.foliagui.animation;

import com.foliagui.FoliaGUI;
import com.foliagui.gui.BaseGui;
import com.foliagui.scheduler.TaskHandle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class GuiAnimation {

    private GuiAnimation() {
    }

    public static @NotNull TaskHandle play(@NotNull BaseGui gui, @NotNull Player player, long periodTicks,
                                           @NotNull Consumer<BaseGui> frame) {
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
