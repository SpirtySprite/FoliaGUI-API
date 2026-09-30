package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.FoliaGUIService;
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

public final class ChatPrompt {

    private final FoliaGUIService service;
    private final Consumer<String> callback;
    private volatile TaskHandle timeoutTask;

    private ChatPrompt(@NotNull FoliaGUIService service, @NotNull Consumer<String> callback) {
        this.service = service;
        this.callback = callback;
    }

    public static boolean hasSession(@NotNull HumanEntity player) {
        return FoliaGUI.isInitialised() && FoliaGUI.service().sessions().chat.has(player);
    }

    public static boolean hasSession(@NotNull FoliaGUIService service, @NotNull HumanEntity player) {
        return service.sessions().chat.has(player);
    }

    public static void ask(@NotNull Player player, @NotNull String prompt, long timeoutTicks,
                            @NotNull Consumer<String> callback) {
        ask(FoliaGUI.service(), player, prompt, timeoutTicks, callback);
    }

    public static void ask(@NotNull FoliaGUIService service, @NotNull Player player, @NotNull String prompt,
                            long timeoutTicks, @NotNull Consumer<String> callback) {
        GuiSessions sessions = service.sessions();
        ensureRegistered(service, sessions);
        BaseGui open = service.guis().getOpenGui(player);
        if (open != null) {
            open.close(player);
        }

        ChatPrompt session = new ChatPrompt(service, callback);
        ChatPrompt replaced = sessions.chat.put(player, session);
        if (replaced != null) {
            replaced.abandon(player);
        }
        player.sendMessage(Text.of(prompt));

        if (timeoutTicks > 0) {
            TaskHandle[] handle = new TaskHandle[1];
            handle[0] = service.scheduler().runForEntityTimer(player, () -> {
                handle[0].cancel();
                if (sessions.chat.remove(player) == session) {
                    callback.accept(null);
                }
            }, null, timeoutTicks, timeoutTicks);
            session.timeoutTask = handle[0];
        }
    }

    private void abandon(Player player) {
        TaskHandle pendingTimeout = timeoutTask;
        if (pendingTimeout != null) {
            pendingTimeout.cancel();
        }
        service.scheduler().runForEntity(player, () -> callback.accept(null), null);
    }

    public static void cancel(@NotNull Player player) {
        if (FoliaGUI.isInitialised()) {
            cancel(FoliaGUI.service(), player);
        }
    }

    public static void cancel(@NotNull FoliaGUIService service, @NotNull Player player) {
        ChatPrompt session = service.sessions().chat.remove(player);
        if (session != null && session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }
    }

    public static void clearAll() {
        if (FoliaGUI.isInitialised()) {
            clear(FoliaGUI.service().sessions());
        }
    }

    static void clear(GuiSessions sessions) {
        for (ChatPrompt session : sessions.chat.values()) {
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
        }
        sessions.chat.clear();
        Listener handler = sessions.chatHandler;
        if (handler != null) {
            HandlerList.unregisterAll(handler);
            sessions.chatHandler = null;
        }
    }

    private static synchronized void ensureRegistered(FoliaGUIService service, GuiSessions sessions) {
        if (sessions.chatHandler == null) {
            Handler handler = new Handler(service, sessions);
            Bukkit.getPluginManager().registerEvents(handler, service.plugin());
            sessions.chatHandler = handler;
        }
    }

    private static final class Handler implements Listener {

        private final FoliaGUIService service;
        private final GuiSessions sessions;

        private Handler(FoliaGUIService service, GuiSessions sessions) {
            this.service = service;
            this.sessions = sessions;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
        public void onChat(@NotNull AsyncChatEvent event) {
            Player player = event.getPlayer();
            ChatPrompt session = sessions.chat.remove(player);
            if (session == null) {
                return;
            }
            event.setCancelled(true);
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
            String text = PlainTextComponentSerializer.plainText().serialize(event.message());
            service.scheduler().runForEntity(player, () -> session.callback.accept(text), null);
        }

        @EventHandler
        public void onQuit(@NotNull PlayerQuitEvent event) {
            cancel(service, event.getPlayer());
        }
    }
}
