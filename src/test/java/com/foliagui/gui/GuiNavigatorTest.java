package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiNavigatorTest {

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
        GuiNavigator.clear(player);
    }

    private static Gui menu(String title) {
        return Gui.builder().rows(1).title(title).create();
    }

    @Test
    void hasNoHistoryByDefault() {
        assertFalse(GuiNavigator.hasHistory(player));
    }

    @Test
    void openingAFirstMenuDoesNotCreateHistory() {
        Gui hub = menu("&8Hub");
        GuiNavigator.open(player, hub);
        server.getScheduler().performOneTick();

        assertFalse(GuiNavigator.hasHistory(player));
    }

    @Test
    void navigatingForwardThenBackReturnsToThePreviousMenu() {
        Gui hub = menu("&8Hub");
        Gui sub = menu("&8Sub");

        GuiNavigator.open(player, hub);
        server.getScheduler().performOneTick();

        GuiNavigator.open(player, sub);
        server.getScheduler().performOneTick();

        assertTrue(GuiNavigator.hasHistory(player));

        boolean wentBack = GuiNavigator.back(player);
        server.getScheduler().performOneTick();

        assertTrue(wentBack);
        assertFalse(GuiNavigator.hasHistory(player));
    }

    @Test
    void backWithNoHistoryReturnsFalse() {
        assertFalse(GuiNavigator.back(player));
    }

    @Test
    void clearDropsTheStack() {
        Gui hub = menu("&8Hub");
        Gui sub = menu("&8Sub");
        GuiNavigator.open(player, hub);
        server.getScheduler().performOneTick();
        GuiNavigator.open(player, sub);
        server.getScheduler().performOneTick();

        GuiNavigator.clear(player);

        assertFalse(GuiNavigator.hasHistory(player));
    }
}
