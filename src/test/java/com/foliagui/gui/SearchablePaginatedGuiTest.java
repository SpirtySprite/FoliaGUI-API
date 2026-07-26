package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchablePaginatedGuiTest {

    private static ServerMock server;
    private PlayerMock player;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
        FoliaGUI.init(MockBukkit.createMockPlugin("FoliaGUITest"));
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUpPlayer() {
        player = server.addPlayer();
    }

    private static GuiItem item(Material material) {
        return ItemBuilder.of(material).asGuiItem();
    }

    @Test
    void searchFiltersDownToMatchingItems() {
        SearchablePaginatedGui gui = new SearchablePaginatedGui(3, Component.text("Items"), 0);
        gui.addSearchableItem(item(Material.DIAMOND_SWORD), "Diamond Sword");
        gui.addSearchableItem(item(Material.IRON_SWORD), "Iron Sword");
        gui.addSearchableItem(item(Material.DIAMOND_PICKAXE), "Diamond Pickaxe");

        gui.open(player);
        server.getScheduler().performOneTick();
        assertEquals(3, countVisibleItems(gui));

        gui.search("diamond");
        server.getScheduler().performOneTick();

        assertEquals(2, countVisibleItems(gui));
        assertEquals("diamond", gui.getCurrentSearchTerm());
    }

    @Test
    void clearSearchRestoresEveryItem() {
        SearchablePaginatedGui gui = new SearchablePaginatedGui(3, Component.text("Items"), 0);
        gui.addSearchableItem(item(Material.DIAMOND_SWORD), "Diamond Sword");
        gui.addSearchableItem(item(Material.IRON_SWORD), "Iron Sword");

        gui.search("iron");
        server.getScheduler().performOneTick();
        assertEquals(1, countVisibleItems(gui));

        gui.clearSearch();
        server.getScheduler().performOneTick();

        assertEquals(2, countVisibleItems(gui));
        assertEquals("", gui.getCurrentSearchTerm());
    }

    @Test
    void noMatchesLeavesTheGuiEmptyWithoutError() {
        SearchablePaginatedGui gui = new SearchablePaginatedGui(3, Component.text("Items"), 0);
        gui.addSearchableItem(item(Material.DIAMOND_SWORD), "Diamond Sword");

        gui.search("nonexistent-term");
        server.getScheduler().performOneTick();

        assertEquals(0, countVisibleItems(gui));
    }

    @Test
    void promptSearchAppliesTheAnsweredTerm() {
        SearchablePaginatedGui gui = new SearchablePaginatedGui(3, Component.text("Items"), 0);
        gui.addSearchableItem(item(Material.DIAMOND_SWORD), "Diamond Sword");
        gui.addSearchableItem(item(Material.IRON_SWORD), "Iron Sword");
        gui.open(player);
        server.getScheduler().performOneTick();

        gui.promptSearch(player);
        player.chat("iron");
        server.getScheduler().waitAsyncEventsFinished();
        for (int i = 0; i < 5; i++) {
            server.getScheduler().performOneTick();
        }

        assertEquals("iron", gui.getCurrentSearchTerm());
        assertTrue(countVisibleItems(gui) == 1);
    }

    private static int countVisibleItems(SearchablePaginatedGui gui) {
        int count = 0;
        for (int slot = 0; slot < gui.getSize(); slot++) {
            if (gui.itemAt(slot) != null) {
                count++;
            }
        }
        return count;
    }
}
