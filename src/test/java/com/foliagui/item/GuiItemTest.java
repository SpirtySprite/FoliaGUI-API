package com.foliagui.item;

import com.foliagui.FoliaGUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiItemTest {

    @BeforeAll
    static void setUp() {
        ServerMock server = MockBukkit.mock();
        FoliaGUI.init(MockBukkit.createMockPlugin("FoliaGUITest"));
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void stampsAndResolvesItsOwnUuid() {
        GuiItem item = new GuiItem(Material.STONE);
        assertEquals(item.getUuid(), GuiItem.uuidOf(item.getItemStack()));
    }

    @Test
    void defersItsUuidUntilIdentityIsRequested() {
        GuiItem item = new GuiItem(Material.STONE);
        assertNull(GuiItem.uuidOf(item.getItemStack()));
    }

    @Test
    void preservesRequestedIdentityWhenItsStackChanges() {
        GuiItem item = new GuiItem(Material.STONE);
        java.util.UUID identity = item.getUuid();
        item.setItemStack(new ItemStack(Material.DIAMOND));
        assertEquals(identity, GuiItem.uuidOf(item.getItemStack()));
    }

    @Test
    void uuidOfReturnsNullForAnUnstampedStack() {
        assertNull(GuiItem.uuidOf(new ItemStack(Material.STONE)));
    }

    @Test
    void uuidOfReturnsNullForNull() {
        assertNull(GuiItem.uuidOf(null));
    }

    @Test
    void twoItemsWithTheSameStackAreNotEqual() {
        GuiItem a = new GuiItem(Material.DIAMOND);
        GuiItem b = new GuiItem(Material.DIAMOND);
        assertNotEquals(a, b);
    }

    @Test
    void isNotEditableByDefault() {
        assertFalse(new GuiItem(Material.STONE).isEditable());
    }

    @Test
    void editableFlagRoundTrips() {
        GuiItem item = new GuiItem(Material.STONE);
        item.editable(true);
        assertTrue(item.isEditable());
    }

    @Test
    void hasNoCooldownByDefault() {
        GuiItem item = new GuiItem(Material.STONE);
        assertEquals(0, item.getCooldownTicks());
        assertTrue(item.tryClick());
        assertTrue(item.tryClick(), "a second immediate click should still pass with no cooldown configured");
    }

    @Test
    void cooldownBlocksAnImmediateSecondClick() {
        GuiItem item = new GuiItem(Material.STONE).cooldown(200);
        assertEquals(200, item.getCooldownTicks());
        assertTrue(item.tryClick(), "the first click should always be allowed");
        assertFalse(item.tryClick(), "an immediate second click should be dropped");
    }

    @Test
    void zeroCooldownDisablesTheGuard() {
        GuiItem item = new GuiItem(Material.STONE).cooldown(20).cooldown(0);
        assertTrue(item.tryClick());
        assertTrue(item.tryClick());
    }
}
