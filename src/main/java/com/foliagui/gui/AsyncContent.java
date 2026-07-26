package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * If the player closes the GUI before the fetch completes, {@code onLoaded} still runs. Check
 * {@link BaseGui#isOpenFor} yourself if populating is expensive.
 */
public final class AsyncContent {

    private AsyncContent() {
    }

    public static <T> void load(@NotNull BaseGui gui, @NotNull Player player,
                                 @NotNull Supplier<T> fetch, @NotNull Consumer<T> onLoaded) {
        gui.open(player);
        FoliaGUI.scheduler().runAsync(() -> {
            T result = fetch.get();
            FoliaGUI.scheduler().runForEntity(player, () -> onLoaded.accept(result), null);
        });
    }
}
