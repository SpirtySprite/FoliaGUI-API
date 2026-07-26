package com.foliagui.util;

/**
 * Utility for translating between chest-GUI {@code (row, column)} coordinates and flat inventory slot
 * indexes. Rows and columns are 1-indexed to match how humans describe chest layouts; the returned slot
 * is 0-indexed as Bukkit expects.
 */
public final class Slot {

    /** Chest inventories are always 9 columns wide. */
    public static final int ROW_WIDTH = 9;

    private Slot() {
    }

    /** Converts a 1-indexed {@code (row, column)} pair to a 0-indexed slot. Row starts at 1, column is 1-9. */
    public static int of(int row, int column) {
        if (column < 1 || column > ROW_WIDTH) {
            throw new IllegalArgumentException("column must be between 1 and " + ROW_WIDTH + ", got " + column);
        }
        if (row < 1) {
            throw new IllegalArgumentException("row must be >= 1, got " + row);
        }
        return (row - 1) * ROW_WIDTH + (column - 1);
    }

    /** 1-indexed. */
    public static int rowOf(int slot) {
        return slot / ROW_WIDTH + 1;
    }

    /** 1-indexed. */
    public static int columnOf(int slot) {
        return slot % ROW_WIDTH + 1;
    }
}
