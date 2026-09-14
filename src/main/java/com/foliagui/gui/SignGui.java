package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.scheduler.TaskHandle;
import com.foliagui.util.Text;
import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SignGui {

    private static final Side SIDE = Side.FRONT;
    private static final BlockData SIGN_BLOCK = Material.OAK_SIGN.createBlockData();

    private static final SessionRegistry<SignGui> SESSIONS = new SessionRegistry<>();

    private final List<Component> lines;
    private final List<String> initialText;
    private final Function<Player, Location> position;
    private final BiConsumer<Player, List<String>> onComplete;
    private final long timeoutTicks;

    private volatile Location openedAt;
    private volatile BlockData original;
    private volatile TaskHandle timeoutTask;

    private SignGui(Builder builder) {
        this.lines = builder.lines;
        this.initialText = builder.lines.stream()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .collect(Collectors.toList());
        this.position = builder.position;
        this.onComplete = builder.onComplete;
        this.timeoutTicks = builder.timeoutTicks;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static boolean hasSession(@NotNull HumanEntity player) {
        return SESSIONS.has(player);
    }

    public void open(@NotNull Player player) {
        FoliaGUI.scheduler().runForEntity(player, () -> {
            Location pos = position.apply(player);
            this.openedAt = pos;
            this.original = pos.getBlock().getBlockData();
            player.sendBlockChange(pos, SIGN_BLOCK);
            player.sendSignChange(pos, lines);
            SESSIONS.put(player, this);
            player.openVirtualSign(pos, SIDE);

            if (timeoutTicks > 0) {
                TaskHandle[] handle = new TaskHandle[1];
                handle[0] = FoliaGUI.scheduler().runForEntityTimer(player, () -> {
                    handle[0].cancel();
                    if (SESSIONS.remove(player) == this) {
                        revert(player);
                    }
                }, null, timeoutTicks, timeoutTicks);
                this.timeoutTask = handle[0];
            }
        }, null);
    }

    @ApiStatus.Internal
    public static boolean handleSignChange(@NotNull UncheckedSignChangeEvent event) {
        Player player = event.getPlayer();
        SignGui gui = SESSIONS.remove(player);
        if (gui == null) {
            return false;
        }
        event.setCancelled(true);
        if (gui.timeoutTask != null) {
            gui.timeoutTask.cancel();
        }
        List<String> raw = event.lines().stream()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .collect(Collectors.toList());
        List<String> text = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String line = raw.get(i);
            boolean untouched = i < gui.initialText.size() && line.equals(gui.initialText.get(i));
            text.add(untouched ? "" : line);
        }
        gui.revert(player);
        gui.onComplete.accept(player, text);
        return true;
    }

    @ApiStatus.Internal
    public static void handleQuit(@NotNull Player player) {
        SignGui gui = SESSIONS.remove(player);
        if (gui != null && gui.timeoutTask != null) {
            gui.timeoutTask.cancel();
        }
    }

    public static void clearSessions() {
        for (SignGui gui : SESSIONS.values()) {
            if (gui.timeoutTask != null) {
                gui.timeoutTask.cancel();
            }
        }
        SESSIONS.clear();
    }

    private void revert(@NotNull Player player) {
        if (openedAt != null) {
            player.sendBlockChange(openedAt, original != null ? original : openedAt.getBlock().getBlockData());
        }
    }

    public static final class Builder {
        private List<Component> lines = defaultLines();
        private Function<Player, Location> position = player -> {
            Location below = player.getLocation().add(0, -3, 0);
            int floor = player.getWorld().getMinHeight();
            if (below.getBlockY() < floor) {
                below.setY(floor);
            }
            return below;
        };
        private BiConsumer<Player, List<String>> onComplete = (player, text) -> {
        };
        private long timeoutTicks = 20L * 60;

        private static @NotNull List<Component> defaultLines() {
            List<Component> lines = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                lines.add(Component.empty());
            }
            return lines;
        }

        public @NotNull Builder lines(@NotNull String... lines) {
            List<Component> parsed = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                parsed.add(i < lines.length ? Text.of(lines[i]) : Component.empty());
            }
            this.lines = parsed;
            return this;
        }

        public @NotNull Builder line(int lineNumber, @NotNull String text) {
            if (lineNumber < 1 || lineNumber > 4) {
                throw new IllegalArgumentException("lineNumber must be 1-4, was " + lineNumber);
            }
            this.lines.set(lineNumber - 1, Text.of(text));
            return this;
        }

        public @NotNull Builder position(@NotNull Function<Player, Location> position) {
            this.position = position;
            return this;
        }

        public @NotNull Builder onComplete(@NotNull BiConsumer<Player, List<String>> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        public @NotNull Builder timeout(long timeoutTicks) {
            this.timeoutTicks = timeoutTicks;
            return this;
        }

        public @NotNull SignGui build() {
            return new SignGui(this);
        }

        public void open(@NotNull Player player) {
            build().open(player);
        }
    }
}
