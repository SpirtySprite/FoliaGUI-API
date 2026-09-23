package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.item.GuiItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AsyncContent {

    private static final Logger LOGGER = Logger.getLogger(AsyncContent.class.getName());

    private AsyncContent() {
    }

    public static <T> void load(@NotNull BaseGui gui, @NotNull Player player,
                                 @NotNull Supplier<T> fetch, @NotNull Consumer<T> onLoaded) {
        load(gui, player, fetch, onLoaded, null);
    }

    public static <T> void load(@NotNull BaseGui gui, @NotNull Player player, @NotNull Supplier<T> fetch,
                                 @NotNull Consumer<T> onLoaded, @Nullable Consumer<Throwable> onError) {
        int loadingSlot = centre(gui);
        boolean showLoading = gui.getGuiItem(loadingSlot) == null;
        if (showLoading) {
            gui.setItem(loadingSlot, FoliaGUI.theme().loading());
        }
        gui.open(player);
        FoliaGUI.scheduler().runAsync(() -> {
            T result;
            try {
                result = fetch.get();
            } catch (Throwable failure) {
                LOGGER.log(Level.WARNING, "Async GUI content failed to load", failure);
                FoliaGUI.scheduler().runForEntity(player, () -> {
                    if (showLoading) {
                        gui.setItem(loadingSlot, FoliaGUI.theme().loadFailed());
                        gui.update();
                    }
                    if (onError != null) {
                        onError.accept(failure);
                    }
                }, null);
                return;
            }
            FoliaGUI.scheduler().runForEntity(player, () -> {
                if (showLoading) {
                    gui.removeItem(loadingSlot);
                }
                try {
                    onLoaded.accept(result);
                } catch (RuntimeException failure) {
                    LOGGER.log(Level.WARNING, "Async GUI content failed to render", failure);
                }
                gui.update();
            }, null);
        });
    }

    public static <T> void loadPages(@NotNull PaginatedGui gui, @NotNull Player player,
                                      @NotNull Supplier<List<T>> fetch, @NotNull Function<T, GuiItem> renderer) {
        load(gui, player, fetch, entries -> {
            List<T> snapshot = new java.util.ArrayList<>(entries);
            gui.setPageItemSupplier(snapshot.size(), index -> renderer.apply(snapshot.get(index)));
        }, null);
    }

    static int centre(@NotNull BaseGui gui) {
        int rows = gui.getRows();
        if (rows <= 0) {
            return gui.getSize() / 2;
        }
        return ((rows - 1) / 2) * 9 + 4;
    }
}
