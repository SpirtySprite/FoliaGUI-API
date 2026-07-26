package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.inventory.InventoryMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MockBukkit's {@code MenuType.create(...)} mock doesn't return a real {@code AnvilView}, so the actual
 * anvil rendering path (the part that needs a genuine view) can't be exercised here. These tests cover what
 * doesn't depend on that: the session bookkeeping, and that AnvilGui correctly stays out of the way of
 * unrelated GUI activity.
 */
class AnvilGuiTest {

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

    @Test
    void hasNoSessionByDefault() {
        assertFalse(AnvilGui.hasSession(player));
    }

    @Test
    void clearSessionsIsSafeWithNothingPending() {
        AnvilGui.clearSessions();
        assertFalse(AnvilGui.hasSession(player));
    }

    @Test
    void handleCloseReturnsFalseWithoutAnActiveSession() {
        InventoryMock inventory = server.createInventory(null, 9);
        InventoryCloseEvent event = new InventoryCloseEvent(player.openInventory(inventory));
        assertFalse(AnvilGui.handleClose(event));
    }

    @Test
    void unrelatedGuiActivityDoesNotCreateAnAnvilSession() {
        Gui gui = Gui.builder().rows(1).title("&8Unrelated").create();
        gui.open(player);
        server.getScheduler().performOneTick();

        player.simulateInventoryClick(0);

        assertFalse(AnvilGui.hasSession(player));
    }

    @Test
    void builderForceOpenIsFluent() {
        AnvilGui.Builder builder = AnvilGui.builder().title("&8Test").forceOpen(true);
        assertTrue(builder instanceof AnvilGui.Builder);
    }
}
