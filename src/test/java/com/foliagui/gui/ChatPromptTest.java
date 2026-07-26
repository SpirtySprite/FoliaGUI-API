package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPromptTest {

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
        ChatPrompt.clearAll();
    }

    @Test
    void answeringDeliversTheMessageToTheCallback() {
        AtomicReference<String> received = new AtomicReference<>();
        ChatPrompt.ask(player, "&eSay something:", 0, received::set);

        assertTrue(ChatPrompt.hasSession(player));

        player.chat("hello world");
        server.getScheduler().waitAsyncEventsFinished();
        server.getScheduler().performOneTick();

        assertFalse(ChatPrompt.hasSession(player));
        assertTrue(received.get() != null && received.get().contains("hello world"));
    }

    @Test
    void cancelDropsTheSessionWithoutInvokingTheCallback() {
        AtomicReference<String> received = new AtomicReference<>();
        ChatPrompt.ask(player, "&eSay something:", 0, received::set);

        ChatPrompt.cancel(player);

        assertFalse(ChatPrompt.hasSession(player));
        assertNull(received.get());
    }

    @Test
    void hasNoSessionByDefault() {
        assertFalse(ChatPrompt.hasSession(player));
    }

    @Test
    void clearAllDropsPendingSessionsWithoutInvokingCallbacks() {
        AtomicReference<String> received = new AtomicReference<>();
        ChatPrompt.ask(player, "&eSay something:", 0, received::set);

        ChatPrompt.clearAll();

        assertFalse(ChatPrompt.hasSession(player));
        assertNull(received.get());
    }
}
