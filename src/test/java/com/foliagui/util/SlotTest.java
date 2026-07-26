package com.foliagui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlotTest {

    @Test
    void convertsRowColumnToFlatSlot() {
        assertEquals(0, Slot.of(1, 1));
        assertEquals(8, Slot.of(1, 9));
        assertEquals(9, Slot.of(2, 1));
        assertEquals(26, Slot.of(3, 9));
    }

    @Test
    void rejectsColumnOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Slot.of(1, 0));
        assertThrows(IllegalArgumentException.class, () -> Slot.of(1, 10));
    }

    @Test
    void rejectsRowBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> Slot.of(0, 1));
    }

    @Test
    void rowOfAndColumnOfRoundTripEveryChestSlot() {
        for (int row = 1; row <= 6; row++) {
            for (int column = 1; column <= Slot.ROW_WIDTH; column++) {
                int slot = Slot.of(row, column);
                assertEquals(row, Slot.rowOf(slot), "row for slot " + slot);
                assertEquals(column, Slot.columnOf(slot), "column for slot " + slot);
            }
        }
    }
}
