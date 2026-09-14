package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.event.GuiClickEvent;
import com.foliagui.event.GuiCloseEvent;
import com.foliagui.event.GuiOpenEvent;
import com.foliagui.item.GuiItem;
import com.foliagui.util.ItemStackSerializer;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiSurfaceTest {

    private static ServerMock server;
    private static Plugin plugin;
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

    private static void openAndFlush(BaseGui gui, PlayerMock viewer) {
        gui.open(viewer);
        server.getScheduler().performOneTick();
    }

    @Test
    void setItemRejectsNullGuiItem() {
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        assertThrows(NullPointerException.class, () -> gui.setItem(0, null));
    }

    @Test
    void addItemRejectsNullArray() {
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        assertThrows(NullPointerException.class, () -> gui.addItem((GuiItem[]) null));
    }

    @Test
    void removeItemRejectsNullGuiItem() {
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        assertThrows(NullPointerException.class, () -> gui.removeItem((GuiItem) null));
    }

    @Test
    void guiOpenEventCancellationBlocksTheOpen() {
        Listener canceller = new Listener() {
            @EventHandler
            public void onOpen(GuiOpenEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(canceller, plugin);
        try {
            Gui gui = Gui.builder().rows(1).title("&8Test").create();
            openAndFlush(gui, player);
            assertFalse(GuiManager.hasGuiOpen(player), "a cancelled GuiOpenEvent should prevent the open");
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(canceller);
        }
    }

    @Test
    void guiClickEventFiresWithResolvedItem() {
        AtomicReference<GuiClickEvent> captured = new AtomicReference<>();
        Listener listener = new Listener() {
            @EventHandler
            public void onGuiClick(GuiClickEvent event) {
                captured.set(event);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);
        try {
            Gui gui = Gui.builder().rows(1).title("&8Test").create();
            gui.setItem(0, ItemBuilder.of(Material.DIAMOND).asGuiItem());
            openAndFlush(gui, player);

            player.simulateInventoryClick(0);

            assertNotNull(captured.get());
            assertEquals(0, captured.get().getSlot());
            assertNotNull(captured.get().getClickedItem());
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
    }

    @Test
    void guiCloseEventFiresOnClose() {
        AtomicBoolean fired = new AtomicBoolean(false);
        Listener listener = new Listener() {
            @EventHandler
            public void onGuiClose(GuiCloseEvent event) {
                fired.set(true);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);
        try {
            Gui gui = Gui.builder().rows(1).title("&8Test").create();
            openAndFlush(gui, player);
            player.closeInventory();
            assertTrue(fired.get());
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
    }

    @Test
    void clickTypeHandlersDispatchSeparately() {
        AtomicInteger left = new AtomicInteger();
        AtomicInteger right = new AtomicInteger();
        AtomicInteger shift = new AtomicInteger();
        GuiItem item = new GuiItem(new ItemStack(Material.STONE));
        item.onLeftClick(e -> left.incrementAndGet());
        item.onRightClick(e -> right.incrementAndGet());
        item.onShiftClick(e -> shift.incrementAndGet());

        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        gui.setItem(0, item);
        openAndFlush(gui, player);

        player.simulateInventoryClick(player.getOpenInventory(), ClickType.LEFT, 0);
        player.simulateInventoryClick(player.getOpenInventory(), ClickType.RIGHT, 0);
        player.simulateInventoryClick(player.getOpenInventory(), ClickType.SHIFT_LEFT, 0);

        assertEquals(1, left.get());
        assertEquals(1, right.get());
        assertEquals(1, shift.get());
    }

    @Test
    void requirePermissionBlocksClicksWithoutIt() {
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger denied = new AtomicInteger();
        GuiItem item = new GuiItem(new ItemStack(Material.STONE), e -> allowed.incrementAndGet());
        item.requirePermission("foliagui.test", p -> denied.incrementAndGet());

        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        gui.setItem(0, item);
        openAndFlush(gui, player);

        player.simulateInventoryClick(0);
        assertEquals(0, allowed.get());
        assertEquals(1, denied.get());

        player.addAttachment(plugin, "foliagui.test", true);
        player.simulateInventoryClick(0);
        assertEquals(1, allowed.get());
        assertEquals(1, denied.get());
    }

    @Test
    void cycleItemAdvancesAndWrapsAround() {
        CycleItem<String> cycle = CycleItem.of(List.of("A", "B", "C"), name -> new ItemStack(Material.PAPER));
        assertEquals("A", cycle.current());
        assertEquals("B", cycle.advance());
        assertEquals("C", cycle.advance());
        assertEquals("A", cycle.advance());
    }

    @Test
    void cycleItemGuiItemAdvancesOnClick() {
        CycleItem<String> cycle = CycleItem.of(List.of("On", "Off"), name -> new ItemStack(Material.LEVER));
        Gui gui = Gui.builder().rows(1).title("&8Test").create();
        gui.setItem(0, cycle.asGuiItem(gui, 0));
        openAndFlush(gui, player);

        assertEquals("On", cycle.current());
        player.simulateInventoryClick(0);
        assertEquals("Off", cycle.current());
    }

    @Test
    void forceOpenReopensAfterAPlayerInitiatedClose() {
        Gui gui = Gui.builder().rows(1).title("&8Force").create();
        gui.setForceOpen(true);
        openAndFlush(gui, player);
        assertTrue(GuiManager.hasGuiOpen(player));

        player.closeInventory();
        server.getScheduler().performTicks(2);

        assertTrue(GuiManager.hasGuiOpen(player), "force-open should have reopened the GUI");
    }

    @Test
    void closeApiBypassesForceOpen() {
        Gui gui = Gui.builder().rows(1).title("&8Force").create();
        gui.setForceOpen(true);
        openAndFlush(gui, player);

        gui.close(player);
        server.getScheduler().performOneTick();

        assertFalse(GuiManager.hasGuiOpen(player), "gui.close() should not be reopened by force-open");
    }

    @Test
    void itemStackSerializerRoundTrips() {
        ItemStack[] original = new ItemStack[5];
        original[1] = ItemBuilder.of(Material.DIAMOND_SWORD, 1).name("&bTest Sword").build();
        original[3] = new ItemStack(Material.EMERALD, 12);

        String encoded = ItemStackSerializer.toBase64(original);
        ItemStack[] decoded = ItemStackSerializer.fromBase64(encoded);

        assertEquals(original.length, decoded.length);
        assertEquals(original[1], decoded[1]);
        assertEquals(original[3], decoded[3]);
        assertEquals(null, decoded[0]);
    }
}
