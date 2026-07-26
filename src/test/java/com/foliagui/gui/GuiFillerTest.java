package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuiFillerTest {

    private static ServerMock server;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
        FoliaGUI.init(MockBukkit.createMockPlugin("FoliaGUITest"));
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    private static GuiItem pane() {
        return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").asGuiItem();
    }

    @Test
    void fillOnlyTouchesEmptySlots() {
        Gui gui = Gui.builder().rows(1).title("&8Fill").create();
        GuiItem preset = ItemBuilder.of(Material.DIAMOND).asGuiItem();
        gui.setItem(4, preset);

        gui.filler().fill(pane());

        for (int slot = 0; slot < gui.getSize(); slot++) {
            assertNotNull(gui.getGuiItem(slot));
        }
        assertNotNull(gui.getGuiItem(4));
        assertEquals(Material.DIAMOND, gui.getGuiItem(4).getItemStack().getType());
    }

    @Test
    void fillBorderPaintsOnlyTheEdgeOfA3RowGui() {
        Gui gui = Gui.builder().rows(3).title("&8Border").create();
        gui.filler().fillBorder(pane());

        assertNotNull(gui.getGuiItem(0));
        assertNotNull(gui.getGuiItem(8));
        assertNotNull(gui.getGuiItem(18));
        assertNotNull(gui.getGuiItem(26));
        assertNull(gui.getGuiItem(13));
    }

    @Test
    void fillCornersPaintsOnlyTheFourCorners() {
        Gui gui = Gui.builder().rows(3).title("&8Corners").create();
        gui.filler().fillCorners(pane());

        assertNotNull(gui.getGuiItem(0));
        assertNotNull(gui.getGuiItem(8));
        assertNotNull(gui.getGuiItem(18));
        assertNotNull(gui.getGuiItem(26));
        assertNull(gui.getGuiItem(1));
        assertNull(gui.getGuiItem(9));
    }

    @Test
    void fillRowAndFillColumnStayWithinBounds() {
        Gui gui = Gui.builder().rows(3).title("&8Row").create();
        gui.filler().fillRow(2, pane());

        for (int column = 1; column <= 9; column++) {
            assertNotNull(gui.getGuiItem((2 - 1) * 9 + (column - 1)));
        }
        assertNull(gui.getGuiItem(0));
        assertNull(gui.getGuiItem(18));
    }

    @Test
    void fillBetweenFillsTheInclusiveRange() {
        Gui gui = Gui.builder().rows(1).title("&8Between").create();
        gui.filler().fillBetween(2, 5, pane());

        assertNull(gui.getGuiItem(1));
        for (int slot = 2; slot <= 5; slot++) {
            assertNotNull(gui.getGuiItem(slot));
        }
        assertNull(gui.getGuiItem(6));
    }

    @Test
    void patternPlacesItemsAccordingToTheCharacterMap() {
        Gui gui = Gui.builder().rows(1).title("&8Pattern").create();
        GuiItem border = pane();
        gui.filler().pattern(Map.of('X', border), "X       X");

        assertNotNull(gui.getGuiItem(0));
        assertNotNull(gui.getGuiItem(8));
        assertNull(gui.getGuiItem(4));
    }
}
