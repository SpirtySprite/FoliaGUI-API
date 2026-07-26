package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiThemeTest {

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

    @Test
    void borderReturnsAFreshItemEachCall() {
        GuiTheme theme = new GuiTheme();
        GuiItem first = theme.border();
        GuiItem second = theme.border();
        assertNotSame(first, second);
    }

    @Test
    void applyBorderPaintsTheEdgeOfTheGui() {
        GuiTheme theme = new GuiTheme();
        Gui gui = Gui.builder().rows(3).title("&8Themed").create();

        theme.applyBorder(gui);

        assertTrue(gui.getGuiItem(0) != null, "top-left corner should be filled");
        assertTrue(gui.getGuiItem(13) == null, "center of the middle row is not part of the border");
    }

    @Test
    void backButtonNavigatesViaGuiNavigator() {
        GuiTheme theme = new GuiTheme();
        Gui hub = Gui.builder().rows(1).title("&8Hub").create();
        Gui sub = Gui.builder().rows(1).title("&8Sub").create();
        sub.setItem(0, theme.backButton());

        GuiNavigator.open(player, hub);
        server.getScheduler().performOneTick();
        GuiNavigator.open(player, sub);
        server.getScheduler().performOneTick();

        player.simulateInventoryClick(0);
        server.getScheduler().performOneTick();

        assertFalse(GuiNavigator.hasHistory(player));
    }

    @Test
    void closeButtonClosesTheGuiAndClearsHistory() {
        GuiTheme theme = new GuiTheme();
        Gui gui = Gui.builder().rows(1).title("&8Menu").create();
        gui.setItem(0, theme.closeButton(gui));

        gui.open(player);
        server.getScheduler().performOneTick();

        player.simulateInventoryClick(0);
        server.getScheduler().performOneTick();

        assertFalse(GuiManager.hasGuiOpen(player));
    }

    @Test
    void overridingTheBorderItemFactoryIsHonoured() {
        GuiTheme theme = new GuiTheme()
                .border(() -> ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").asGuiItem());

        assertEquals(Material.BLACK_STAINED_GLASS_PANE, theme.border().getItemStack().getType());
    }
}
