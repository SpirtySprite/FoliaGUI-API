package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiIntegrationTest {

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

    private static void openAndFlush(BaseGui gui, PlayerMock viewer) {
        gui.open(viewer);
        server.getScheduler().performOneTick();
    }

    @Test
    void clickingAGuiItemFiresItsAction() {
        AtomicInteger clicks = new AtomicInteger();
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        gui.setItem(0, ItemBuilder.of(Material.DIAMOND).asGuiItem(event -> clicks.incrementAndGet()));
        openAndFlush(gui, player);

        player.simulateInventoryClick(0);

        assertEquals(1, clicks.get());
    }

    @Test
    void guiItemsAreProtectedFromBeingTakenEvenInAStorageGui() {
        StorageGui gui = StorageGui.builder().rows(1).title("&8Storage").create();
        gui.setItem(0, ItemBuilder.of(Material.BARRIER).asGuiItem());
        openAndFlush(gui, player);

        InventoryClickEvent event = player.simulateInventoryClick(0);

        assertTrue(event.isCancelled(), "a GuiItem slot must stay locked even in a GUI that clears all modifiers");
    }

    @Test
    void freeSlotsInAStorageGuiStayOpen() {
        StorageGui gui = StorageGui.builder().rows(1).title("&8Storage").create();
        openAndFlush(gui, player);

        InventoryClickEvent event = player.simulateInventoryClick(0);

        assertFalse(event.isCancelled(), "an empty slot in a StorageGui must stay free to use");
    }

    @Test
    void editableGuiItemsOptOutOfTheAutomaticProtectionAStorageGuiWouldOtherwiseApply() {
        StorageGui gui = StorageGui.builder().rows(1).title("&8Storage").create();
        GuiItem item = ItemBuilder.of(Material.EMERALD).asGuiItem();
        item.editable(true);
        gui.setItem(0, item);
        openAndFlush(gui, player);

        InventoryClickEvent event = player.simulateInventoryClick(0);

        assertFalse(event.isCancelled(), "an editable GuiItem should opt out of automatic slot protection");
    }

    @Test
    void cooldownDropsARapidSecondClick() {
        AtomicInteger clicks = new AtomicInteger();
        GuiItem item = ItemBuilder.of(Material.DIAMOND).asGuiItem(event -> clicks.incrementAndGet());
        item.cooldown(200);
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        gui.setItem(0, item);
        openAndFlush(gui, player);

        player.simulateInventoryClick(0);
        player.simulateInventoryClick(0);

        assertEquals(1, clicks.get(), "the second click landed inside the cooldown window and should be dropped");
    }

    @Test
    void addItemLandsInTheFirstEmptySlot() {
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        gui.setItem(0, ItemBuilder.of(Material.BARRIER).asGuiItem());
        GuiItem added = ItemBuilder.of(Material.EMERALD).asGuiItem();
        gui.addItem(added);

        assertEquals(added, gui.getGuiItem(1));
    }

    @Test
    void removeItemBySlotClearsIt() {
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        gui.setItem(0, ItemBuilder.of(Material.BARRIER).asGuiItem());
        gui.removeItem(0);

        assertNull(gui.getGuiItem(0));
    }

    @Test
    void guiManagerTracksTheOpenGui() {
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        openAndFlush(gui, player);

        assertTrue(GuiManager.hasGuiOpen(player));
        assertEquals(gui, GuiManager.getOpenGui(player));
        assertTrue(GuiManager.openGuisOfType(Gui.class).contains(gui));
        assertTrue(GuiManager.viewersOf(gui).contains(player));
    }
}
