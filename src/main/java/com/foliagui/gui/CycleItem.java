package com.foliagui.gui;

import com.foliagui.item.GuiItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** A button that cycles through a fixed list of values on each click (On/Off/Auto, difficulty, color, ...). */
public final class CycleItem<T> {

    private final List<T> values;
    private final Function<T, ItemStack> renderer;
    private final AtomicInteger index = new AtomicInteger();

    private CycleItem(@NotNull List<T> values, @NotNull Function<T, ItemStack> renderer) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values cannot be empty");
        }
        this.values = List.copyOf(values);
        this.renderer = renderer;
    }

    public static <T> @NotNull CycleItem<T> of(@NotNull List<T> values, @NotNull Function<T, ItemStack> renderer) {
        return new CycleItem<>(values, renderer);
    }

    public @NotNull T current() {
        return values.get(index.get());
    }

    /** Wraps around. */
    public @NotNull T advance() {
        int next = index.updateAndGet(i -> (i + 1) % values.size());
        return values.get(next);
    }

    public void reset() {
        index.set(0);
    }

    public @NotNull ItemStack render() {
        return renderer.apply(current());
    }

    /** On click, advances and pushes the re-rendered item via {@link BaseGui#updateItem(int, ItemStack)}. */
    public @NotNull GuiItem asGuiItem(@NotNull BaseGui gui, int slot) {
        return new GuiItem(render(), event -> {
            advance();
            gui.updateItem(slot, render());
        });
    }
}
