package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.FoliaGUINotInitialisedException;
import com.foliagui.FoliaGUIService;
import com.foliagui.builder.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiServiceTest {

    private static ServerMock server;
    private static int counter;

    private Plugin defaultOwner;
    private Plugin otherOwner;
    private FoliaGUIService other;
    private PlayerMock player;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        counter++;
        defaultOwner = MockBukkit.createMockPlugin("DefaultOwner" + counter);
        otherOwner = MockBukkit.createMockPlugin("OtherOwner" + counter);
        FoliaGUI.init(defaultOwner);
        other = FoliaGUI.create(otherOwner);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        other.close();
        FoliaGUI.shutdown();
    }

    private void open(BaseGui gui) {
        gui.open(player);
        server.getScheduler().performOneTick();
    }

    @Test
    void servicesAreIndependentInstances() {
        assertNotSame(FoliaGUI.service(), other);
        assertSame(defaultOwner, FoliaGUI.service().plugin());
        assertSame(otherOwner, other.plugin());
        assertNotSame(FoliaGUI.service().guis(), other.guis());
        assertNotSame(FoliaGUI.service().navigation(), other.navigation());
    }

    @Test
    void aGuiBoundToAnExplicitServiceIsTrackedOnlyByThatService() {
        Gui gui = Gui.builder().rows(1).title("&8Other").service(other).create();
        open(gui);

        assertSame(gui, other.guis().getOpenGui(player));
        assertNull(FoliaGUI.service().guis().getOpenGui(player));
        assertEquals(1, other.guis().openCount());
        assertEquals(0, FoliaGUI.service().guis().openCount());
    }

    @Test
    void aClickOnAnExplicitlyBoundGuiIsHandledExactlyOnce() {
        AtomicInteger clicks = new AtomicInteger();
        Gui gui = Gui.builder().rows(1).title("&8Other").service(other).create();
        gui.setItem(0, ItemBuilder.of(Material.DIAMOND).asGuiItem(event -> clicks.incrementAndGet()));
        open(gui);

        player.simulateInventoryClick(0);

        assertEquals(1, clicks.get(), "both services have a listener, only the owning one may act");
    }

    @Test
    void aClickOnADefaultBoundGuiIsHandledExactlyOnce() {
        AtomicInteger clicks = new AtomicInteger();
        Gui gui = Gui.builder().rows(1).title("&8Default").create();
        gui.setItem(0, ItemBuilder.of(Material.DIAMOND).asGuiItem(event -> clicks.incrementAndGet()));
        open(gui);

        player.simulateInventoryClick(0);

        assertEquals(1, clicks.get());
        assertSame(gui, FoliaGUI.service().guis().getOpenGui(player));
        assertNull(other.guis().getOpenGui(player));
    }

    @Test
    void closingOneServiceLeavesTheOtherWorking() {
        AtomicInteger clicks = new AtomicInteger();
        Gui keep = Gui.builder().rows(1).title("&8Keep").create();
        keep.setItem(0, ItemBuilder.of(Material.EMERALD).asGuiItem(event -> clicks.incrementAndGet()));
        open(keep);

        other.close();

        assertTrue(other.isClosed());
        assertFalse(FoliaGUI.service().isClosed());
        player.simulateInventoryClick(0);
        assertEquals(1, clicks.get());
    }

    @Test
    void navigationHistoryIsPerService() {
        Gui first = Gui.builder().rows(1).title("&8First").service(other).create();
        Gui second = Gui.builder().rows(1).title("&8Second").service(other).create();
        open(first);
        other.navigation().open(player, second);
        server.getScheduler().performOneTick();

        assertTrue(other.navigation().hasHistory(player));
        assertFalse(FoliaGUI.service().navigation().hasHistory(player));
    }

    @Test
    void chatPromptsBelongToTheServiceThatAskedThem() {
        AtomicReference<String> answer = new AtomicReference<>();
        ChatPrompt.ask(other, player, "&eSay something:", 0, answer::set);

        assertTrue(ChatPrompt.hasSession(other, player));
        assertFalse(ChatPrompt.hasSession(player), "the default service must not see the other service's prompt");

        player.chat("hello");
        server.getScheduler().waitAsyncEventsFinished();
        server.getScheduler().performOneTick();

        assertTrue(answer.get() != null && answer.get().contains("hello"));
        assertFalse(ChatPrompt.hasSession(other, player));
    }

    @Test
    void initFromASecondPluginKeepsTheFirstDefault() {
        FoliaGUI.init(otherOwner);

        assertSame(defaultOwner, FoliaGUI.service().plugin());
    }

    @Test
    void afterShutdownGuisWithoutAnExplicitServiceFailClearly() {
        Gui unbound = Gui.builder().rows(1).title("&8Unbound").create();

        FoliaGUI.shutdown();

        assertFalse(FoliaGUI.isInitialised());
        assertThrows(FoliaGUINotInitialisedException.class, unbound::service);
    }

    @Test
    void anExplicitServiceKeepsWorkingWithoutAnyDefault() {
        FoliaGUI.shutdown();
        AtomicInteger clicks = new AtomicInteger();
        Gui gui = Gui.builder().rows(1).title("&8Explicit").service(other).create();
        gui.setItem(0, ItemBuilder.of(Material.DIAMOND).asGuiItem(event -> clicks.incrementAndGet()));
        open(gui);

        player.simulateInventoryClick(0);

        assertEquals(1, clicks.get());
    }

    @Test
    void themeIsPerService() {
        GuiTheme custom = new GuiTheme();
        other.theme(custom);

        assertSame(custom, other.theme());
        assertNotSame(custom, FoliaGUI.service().theme());
    }
}
