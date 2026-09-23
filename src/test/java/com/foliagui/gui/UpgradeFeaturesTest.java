package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import com.foliagui.util.Slot;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeFeaturesTest {

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

    private void openAndFlush(BaseGui gui) {
        gui.open(player);
        server.getScheduler().performOneTick();
    }

    @Test
    void cooldownIsTrackedPerPlayer() {
        PlayerMock other = server.addPlayer();
        GuiItem shared = ItemBuilder.of(Material.DIAMOND).asGuiItem();
        shared.cooldown(200);
        assertTrue(shared.tryClick(player.getUniqueId()));
        assertFalse(shared.tryClick(player.getUniqueId()));
        assertTrue(shared.tryClick(other.getUniqueId()), "another player must not be blocked by someone else's click");
        assertTrue(shared.remainingCooldownMillis(player.getUniqueId()) > 0);
    }

    @Test
    void navigatorHistoryIsBoundedAndSkipsDuplicates() {
        Gui first = Gui.of(1, "&8A");
        Gui second = Gui.of(1, "&8B");
        openAndFlush(first);
        for (int round = 0; round < 50; round++) {
            GuiNavigator.open(player, second);
            server.getScheduler().performOneTick();
            GuiNavigator.open(player, first);
            server.getScheduler().performOneTick();
        }
        assertTrue(GuiNavigator.depth(player) <= 2, "flipping between two menus must not grow the history");
        GuiNavigator.maxDepth(3);
        for (int index = 0; index < 10; index++) {
            GuiNavigator.open(player, Gui.of(1, "&8" + index));
            server.getScheduler().performOneTick();
        }
        assertEquals(3, GuiNavigator.depth(player));
        GuiNavigator.maxDepth(32);
    }

    @Test
    void pageControlsFollowThePage() {
        PaginatedGui gui = new PaginatedGui(3, Component.text("Shop"), 0);
        gui.pageControls(true);
        gui.addPageItem(IntStream.range(0, 40).mapToObj(index -> new GuiItem(Material.STONE)).toList());
        openAndFlush(gui);

        int previous = Slot.of(3, 1);
        int next = Slot.of(3, 9);
        assertEquals(18, gui.pageSlots().size());
        assertEquals(3, gui.getPagesCount());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, gui.getInventory().getItem(previous).getType());
        assertEquals(Material.ARROW, gui.getInventory().getItem(next).getType());

        player.simulateInventoryClick(next);
        server.getScheduler().performOneTick();
        assertEquals(2, gui.getCurrentPage());
        assertEquals(Material.ARROW, gui.getInventory().getItem(previous).getType());
        assertEquals(2, gui.getInventory().getItem(Slot.of(3, 5)).getAmount());
    }

    @Test
    void pageViewFiltersAndSorts() {
        PaginatedGui gui = new PaginatedGui(2, Component.text("Market"), 0);
        List<Integer> prices = new ArrayList<>(List.of(50, 10, 30, 20, 40));
        PageView<Integer> view = gui.view(prices, price -> new GuiItem(Material.GOLD_INGOT));
        assertEquals(5, view.size());
        view.filter(price -> price >= 20).sort(Comparator.naturalOrder());
        assertEquals(List.of(20, 30, 40, 50), view.visible());
        assertEquals(4, gui.getPageItemsCount());
        view.entries(List.of(1, 2));
        assertEquals(List.of(), view.visible());
        view.filter(null);
        assertEquals(List.of(1, 2), view.visible());
    }

    @Test
    void confirmationRunsOnlyOnceEvenWhenClickedTwice() {
        AtomicInteger confirmed = new AtomicInteger();
        Confirmation.Builder builder = Confirmation.builder().onConfirm(p -> confirmed.incrementAndGet());
        Gui first = builder.build();
        Gui second = builder.build();
        openAndFlush(first);
        player.simulateInventoryClick(Slot.of(2, 3));
        player.simulateInventoryClick(Slot.of(2, 3));
        assertEquals(1, confirmed.get());
        assertFalse(first.getGuiItem(Slot.of(2, 3)) == second.getGuiItem(Slot.of(2, 3)),
                "two confirmations built from one builder must not share button items");
    }

    @Test
    void quantityPickerClampsAndConfirms() {
        AtomicInteger chosen = new AtomicInteger();
        Gui gui = QuantityGui.builder().display(new ItemStack(Material.DIAMOND)).range(1, 100).initial(5)
                .onConfirm(chosen::set).build();
        openAndFlush(gui);
        player.simulateInventoryClick(Slot.of(2, 9));
        player.simulateInventoryClick(Slot.of(2, 9));
        player.simulateInventoryClick(Slot.of(2, 1));
        server.getScheduler().performOneTick();
        player.simulateInventoryClick(Slot.of(3, 4));
        assertEquals(36, chosen.get());
        assertEquals(36, gui.getInventory().getItem(Slot.of(2, 5)).getAmount());
    }

    @Test
    void asyncContentShowsTheFailureItemWhenLoadingThrows() {
        Gui gui = Gui.of(3, "&8Async");
        AtomicInteger errors = new AtomicInteger();
        AsyncContent.load(gui, player, () -> {
            throw new IllegalStateException("database offline");
        }, value -> { }, failure -> errors.incrementAndGet());
        for (int tick = 0; tick < 5; tick++) {
            server.getScheduler().performOneTick();
        }
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performOneTick();
        assertEquals(1, errors.get());
        GuiItem centre = gui.getGuiItem(AsyncContent.centre(gui));
        assertNotNull(centre);
        assertEquals(Material.BARRIER, centre.getItemStack().getType());
    }

    @Test
    void asyncPagesFillThePaginatedGui() {
        PaginatedGui gui = new PaginatedGui(3, Component.text("Async"), 0);
        AsyncContent.loadPages(gui, player, () -> List.of("a", "b", "c"), value -> new GuiItem(Material.PAPER));
        server.getScheduler().waitAsyncTasksFinished();
        server.getScheduler().performOneTick();
        server.getScheduler().performOneTick();
        assertEquals(3, gui.getPageItemsCount());
        assertEquals(Material.PAPER, gui.getInventory().getItem(0).getType());
    }

    @Test
    void tickActionRunsOnTheUpdateInterval() {
        AtomicInteger ticks = new AtomicInteger();
        Gui gui = Gui.of(1, "&8Live");
        gui.onTick(2, current -> ticks.incrementAndGet());
        openAndFlush(gui);
        for (int tick = 0; tick < 10; tick++) {
            server.getScheduler().performOneTick();
        }
        assertTrue(ticks.get() >= 3, "tick action ran " + ticks.get() + " times");
    }

    @Test
    void textParsesLegacyAndMiniMessageTogether() {
        Component parsed = Text.parse("&6Gold <blue>Blue");
        assertEquals("Gold Blue", Text.plain(parsed));
        assertEquals("\\<red>", Text.escape("<red>"));
        ItemStack named = ItemBuilder.of(Material.STONE).nameAny("&aHello <bold>there").build();
        assertEquals("Hello there", Text.plain(named.getItemMeta().displayName()));
    }

    @Test
    void arrowsCanShowThePageTheyBelongTo() {
        GuiTheme previous = FoliaGUI.theme();
        FoliaGUI.theme(new GuiTheme().nextButtonItem(page -> ItemBuilder.of(Material.ARROW)
                .amount(page.getCurrentPage()).asGuiItem()));
        try {
            PaginatedGui gui = new PaginatedGui(2, Component.text("Pages"), 0);
            gui.pageControls();
            gui.addPageItem(IntStream.range(0, 60).mapToObj(index -> new GuiItem(Material.STONE)).toList());
            openAndFlush(gui);
            int next = Slot.of(2, 9);
            assertEquals(1, gui.getInventory().getItem(next).getAmount());
            player.simulateInventoryClick(next);
            server.getScheduler().performOneTick();
            assertEquals(2, gui.getInventory().getItem(next).getAmount());
        } finally {
            FoliaGUI.theme(previous);
        }
    }
}
