package com.foliagui.util;

public final class Slot {

    public static final int ROW_WIDTH = 9;

    private Slot() {
    }

    public static int of(int row, int column) {
        if (column < 1 || column > ROW_WIDTH) {
            throw new IllegalArgumentException("column must be between 1 and " + ROW_WIDTH + ", got " + column);
        }
        if (row < 1) {
            throw new IllegalArgumentException("row must be >= 1, got " + row);
        }
        return (row - 1) * ROW_WIDTH + (column - 1);
    }

    public static int rowOf(int slot) {
        return slot / ROW_WIDTH + 1;
    }

    public static int columnOf(int slot) {
        return slot % ROW_WIDTH + 1;
    }
}
