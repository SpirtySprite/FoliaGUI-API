package com.foliagui.gui;

import com.foliagui.item.GuiItem;
import com.foliagui.util.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class GuiFiller {

    private final BaseGui gui;

    GuiFiller(@NotNull BaseGui gui) {
        this.gui = gui;
    }

    public @NotNull GuiFiller fill(@NotNull GuiItem item) {
        for (int slot = 0; slot < gui.getSize(); slot++) {
            if (gui.getGuiItem(slot) == null) {
                gui.setItem(slot, item);
            }
        }
        return this;
    }

    public @NotNull GuiFiller fillTop(@NotNull GuiItem item) {
        return fillRow(1, item);
    }

    public @NotNull GuiFiller fillBottom(@NotNull GuiItem item) {
        if (gui.getRows() > 0) {
            fillRow(gui.getRows(), item);
        }
        return this;
    }

    public @NotNull GuiFiller fillRow(int row, @NotNull GuiItem item) {
        if (gui.getRows() <= 0) {
            return this;
        }
        for (int column = 1; column <= Slot.ROW_WIDTH; column++) {
            setIfEmpty(Slot.of(row, column), item);
        }
        return this;
    }

    public @NotNull GuiFiller fillColumn(int column, @NotNull GuiItem item) {
        if (gui.getRows() <= 0) {
            return this;
        }
        for (int row = 1; row <= gui.getRows(); row++) {
            setIfEmpty(Slot.of(row, column), item);
        }
        return this;
    }

    public @NotNull GuiFiller fillBorder(@NotNull GuiItem item) {
        int rows = gui.getRows();
        if (rows <= 0) {
            return this;
        }
        fillRow(1, item);
        fillRow(rows, item);
        fillColumn(1, item);
        fillColumn(Slot.ROW_WIDTH, item);
        return this;
    }

    public @NotNull GuiFiller fillCorners(@NotNull GuiItem item) {
        int rows = gui.getRows();
        if (rows <= 0) {
            return this;
        }
        setIfEmpty(Slot.of(1, 1), item);
        setIfEmpty(Slot.of(1, Slot.ROW_WIDTH), item);
        setIfEmpty(Slot.of(rows, 1), item);
        setIfEmpty(Slot.of(rows, Slot.ROW_WIDTH), item);
        return this;
    }

    public @NotNull GuiFiller fillBetween(int from, int to, @NotNull GuiItem item) {
        int lo = Math.max(0, Math.min(from, to));
        int hi = Math.min(gui.getSize() - 1, Math.max(from, to));
        for (int slot = lo; slot <= hi; slot++) {
            setIfEmpty(slot, item);
        }
        return this;
    }

    public @NotNull GuiFiller pattern(@NotNull Map<Character, GuiItem> key, @NotNull String... rows) {
        for (int r = 0; r < rows.length && r < gui.getRows(); r++) {
            String line = rows[r];
            for (int c = 0; c < line.length() && c < Slot.ROW_WIDTH; c++) {
                GuiItem item = key.get(line.charAt(c));
                if (item != null) {
                    setIfEmpty(Slot.of(r + 1, c + 1), item);
                }
            }
        }
        return this;
    }

    private void setIfEmpty(int slot, @NotNull GuiItem item) {
        if (gui.getGuiItem(slot) == null) {
            gui.setItem(slot, item);
        }
    }
}
