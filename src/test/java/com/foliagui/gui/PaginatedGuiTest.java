package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.item.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginatedGuiTest {

    private static ServerMock server;

    @BeforeAll
    static void setUp() {
        server = MockBukkit.mock();
        FoliaGUI.init(MockBukkit.createMockPlugin("FoliaGUITest"));
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void supplierOnlyBuildsVisibleItemsAndCachesVisitedPages() {
        PlayerMock player = server.addPlayer();
        AtomicInteger rendered = new AtomicInteger();
        PaginatedGui gui = new PaginatedGui(6, Component.text("Marché"), 45);
        gui.setPageItemSupplier(50_000, index -> {
            rendered.incrementAndGet();
            return new GuiItem(Material.STONE);
        });

        gui.open(player);
        server.getScheduler().performOneTick();

        assertEquals(50_000, gui.getPageItemsCount());
        assertEquals(1_112, gui.getPagesCount());
        assertEquals(45, rendered.get());
        assertTrue(gui.next());
        server.getScheduler().performOneTick();
        assertEquals(90, rendered.get());
        assertTrue(gui.previous());
        server.getScheduler().performOneTick();
        assertEquals(90, rendered.get());
    }
}
