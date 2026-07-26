package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantGuiTest {

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
    void buildingWithNoRecipesThrows() {
        assertThrows(IllegalStateException.class, () -> MerchantGui.builder().title("&8Empty").build());
    }

    @Test
    void openingRegistersASession() {
        MerchantGui.builder()
                .title("&8Blacksmith")
                .addRecipe(new ItemStack(Material.DIAMOND_SWORD), List.of(new ItemStack(Material.EMERALD, 5)))
                .open(player);
        server.getScheduler().performOneTick();

        assertTrue(MerchantGui.hasSession(player));
    }

    @Test
    void hasNoSessionByDefault() {
        assertFalse(MerchantGui.hasSession(player));
    }

    @Test
    void clearSessionsIsSafeWithNothingPending() {
        MerchantGui.clearSessions();
        assertFalse(MerchantGui.hasSession(player));
    }
}
