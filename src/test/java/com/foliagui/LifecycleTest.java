package com.foliagui;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Own test class (own JVM fork) so it can freely init/shutdown FoliaGUI without disturbing other tests. */
class LifecycleTest {

    @AfterEach
    void tearDown() {
        FoliaGUI.shutdown();
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void shutdownAllowsReinitialisation() {
        ServerMock server = MockBukkit.mock();
        Plugin pluginA = MockBukkit.createMockPlugin("PluginA");

        FoliaGUI.init(pluginA);
        assertTrue(FoliaGUI.isInitialised());
        assertEqualsPlugin(pluginA);

        FoliaGUI.shutdown();
        assertFalse(FoliaGUI.isInitialised());

        Plugin pluginB = MockBukkit.createMockPlugin("PluginB");
        FoliaGUI.init(pluginB);
        assertTrue(FoliaGUI.isInitialised());
        assertEqualsPlugin(pluginB);
    }

    @Test
    void registersAndUnregistersServicesManagerEntry() {
        MockBukkit.mock();
        Plugin plugin = MockBukkit.createMockPlugin("PluginA");

        FoliaGUI.init(plugin);
        FoliaGUIService service = org.bukkit.Bukkit.getServicesManager().load(FoliaGUIService.class);
        assertNotNull(service);
        assertEqualsPlugin(plugin);

        FoliaGUI.shutdown();
        assertNotNull(org.bukkit.Bukkit.getServicesManager()); // manager itself always exists
    }

    @Test
    void shutdownBeforeInitIsANoOp() {
        MockBukkit.mock();
        FoliaGUI.shutdown();
        assertFalse(FoliaGUI.isInitialised());
    }

    private static void assertEqualsPlugin(Plugin expected) {
        assertTrue(FoliaGUI.isInitialised());
        org.junit.jupiter.api.Assertions.assertEquals(expected, FoliaGUI.plugin());
    }
}
