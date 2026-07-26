package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.scheduler.TaskHandle;
import com.foliagui.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Captures a player's next chat message instead of showing a GUI. Closes any open GUI, cancels the chat
 * line, and delivers the text to the callback on the player's region thread.
 */
public final class ChatPrompt {

    private static final SessionRegistry<ChatPrompt> PENDING = new SessionRegistry<>();
    private static volatile Listener registeredHandler;

    private final Consumer<String> callback;
    private volatile TaskHandle timeoutTask;

    private ChatPrompt(@NotNull Consumer<String> callback) {
        this.callback = callback;
    }

    public static boolean hasSession(@NotNull HumanEntity player) {
        return PENDING.has(player);
    }

    /** {@code timeoutTicks} of {@code 0} waits indefinitely; otherwise callback runs once with {@code null} on timeout. */
    public static void ask(@NotNull Player player, @NotNull String prompt, long timeoutTicks,
                            @NotNull Consumer<String> callback) {
        ensureRegistered();
        BaseGui open = GuiManager.getOpenGui(player);
        if (open != null) {
            open.close(player);
        }

        ChatPrompt session = new ChatPrompt(callback);
        PENDING.put(player, session);
        player.sendMessage(Text.of(prompt));

        if (timeoutTicks > 0) {
            // self-cancelling timer, needs to stay cancellable if the player answers first
            TaskHandle[] handle = new TaskHandle[1];
            handle[0] = FoliaGUI.scheduler().runForEntityTimer(player, () -> {
                handle[0].cancel();
                if (PENDING.remove(player) == session) {
                    callback.accept(null);
                }
            }, null, timeoutTicks, timeoutTicks);
            session.timeoutTask = handle[0];
        }
    }

    /** Does not invoke the callback. */
    public static void cancel(@NotNull Player player) {
        ChatPrompt session = PENDING.remove(player);
        if (session != null && session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }
    }

    /** Unregisters the backing listener so {@link #ask} re-registers cleanly on the next {@code FoliaGUI.init}. */
    public static void clearAll() {
        for (ChatPrompt session : PENDING.values()) {
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
        }
        PENDING.clear();
        if (registeredHandler != null) {
            HandlerList.unregisterAll(registeredHandler);
            registeredHandler = null;
        }
    }

    private static synchronized void ensureRegistered() {
        if (registeredHandler == null) {
            Handler handler = new Handler();
            Bukkit.getPluginManager().registerEvents(handler, FoliaGUI.plugin());
            registeredHandler = handler;
        }
    }

    private static final class Handler implements Listener {

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
        public void onChat(@NotNull AsyncChatEvent event) {
            Player player = event.getPlayer();
            ChatPrompt session = PENDING.remove(player);
            if (session == null) {
                return;
            }
            event.setCancelled(true);
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
            String text = PlainTextComponentSerializer.plainText().serialize(event.message());
            // chat events fire off the region thread, hop back before calling back
            FoliaGUI.scheduler().runForEntity(player, () -> session.callback.accept(text), null);
        }

        @EventHandler
        public void onQuit(@NotNull PlayerQuitEvent event) {
            cancel(event.getPlayer());
        }
    }
}
