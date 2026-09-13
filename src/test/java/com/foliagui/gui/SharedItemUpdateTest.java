package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class SharedItemUpdateTest {

    private static ServerMock server;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
        FoliaGUI.init(MockBukkit.createMockPlugin("FoliaGUISharedTest"));
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @Test
    void updatingOneGuiLeavesASharedItemUntouched() {
        GuiItem shared = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).asGuiItem();
        Gui first = Gui.builder().rows(1).title("&8First").create();
        Gui second = Gui.builder().rows(1).title("&8Second").create();
        first.setItem(0, shared);
        second.setItem(0, shared);

        first.updateItem(0, new ItemStack(Material.DIAMOND));

        assertEquals(Material.BLACK_STAINED_GLASS_PANE, shared.getItemStack().getType());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, second.getGuiItem(0).getItemStack().getType());
        assertEquals(Material.DIAMOND, first.getGuiItem(0).getItemStack().getType());
        assertNotSame(shared, first.getGuiItem(0));
    }

    @Test
    void updatedItemKeepsItsClickAction() {
        AtomicInteger clicks = new AtomicInteger();
        PlayerMock player = server.addPlayer();
        Gui gui = Gui.builder().rows(1).title("&8Action").create();
        gui.setItem(0, ItemBuilder.of(Material.STONE).asGuiItem(event -> clicks.incrementAndGet()));
        gui.open(player);
        server.getScheduler().performOneTick();

        gui.updateItem(0, new ItemStack(Material.EMERALD));
        player.simulateInventoryClick(0);

        assertEquals(1, clicks.get());
        assertEquals(Material.EMERALD, gui.getInventory().getItem(0).getType());
    }

    @Test
    void updatesBeforeOpeningApplyImmediately() {
        Gui gui = Gui.builder().rows(1).title("&8Early").create();
        gui.setItem(0, ItemBuilder.of(Material.STONE).asGuiItem());
        gui.update();

        assertEquals(Material.STONE, gui.getInventory().getItem(0).getType());
    }
}
