package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.event.GuiClickEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedSlotTest {

    private static ServerMock server;
    private static org.bukkit.plugin.Plugin plugin;
    private PlayerMock player;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("FoliaGUITest");
        FoliaGUI.init(plugin);
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUpPlayer() {
        player = server.addPlayer();
    }

    private StorageGui openStorageWithProtectedItem() {
        StorageGui gui = StorageGui.builder().rows(1).title("&8Storage").create();
        gui.setItem(0, ItemBuilder.of(Material.BARRIER).asGuiItem());
        gui.open(player);
        server.getScheduler().performOneTick();
        return gui;
    }

    @Test
    void aGuiClickListenerCannotUncancelAClickOnAProtectedSlot() {
        Listener uncancel = new Listener() {
            @EventHandler
            public void onGuiClick(GuiClickEvent event) {
                event.setCancelled(false);
            }
        };
        server.getPluginManager().registerEvents(uncancel, plugin);
        try {
            openStorageWithProtectedItem();

            InventoryClickEvent event = player.simulateInventoryClick(0);

            assertTrue(event.isCancelled(),
                    "a GuiClickEvent listener must not be able to unlock a protected slot");
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(uncancel);
        }
    }

    @ParameterizedTest
    @EnumSource(value = ClickType.class,
            names = {"LEFT", "RIGHT", "SHIFT_LEFT", "SHIFT_RIGHT", "DROP", "CONTROL_DROP", "DOUBLE_CLICK"})
    void everyTakeOrMoveClickTypeIsCancelledOnAProtectedSlot(ClickType type) {
        openStorageWithProtectedItem();

        InventoryClickEvent event = player.simulateInventoryClick(player.getOpenInventory(), type, 0);

        assertTrue(event.isCancelled(), type + " must not be able to take a protected item");
    }
}
