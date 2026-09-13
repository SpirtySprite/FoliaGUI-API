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

/**
 * Text-input dialog built on Paper's virtual sign packets ({@code Player#openVirtualSign}); opens on the
 * player's region thread.
 * <p>
 * Unlike {@link AnvilGui}, this briefly renders a fake sign block at a {@link Location} (by default the
 * block beneath the player's feet), visible only to the editing player via {@code sendBlockChange}.
 * Nothing is placed in the real world and no other player ever sees it, but the editor themself does see
 * a sign flash in for the duration of the dialog, that's inherent to how the client ties the sign-edit
 * screen to a block position.
 * <p>
 * There is no reliable "player pressed Escape" signal for signs the way {@link AnvilGui} gets one from
 * {@code InventoryCloseEvent}, so {@code onComplete} is the only callback. A {@code timeout} safety net
 * reverts the fake block and drops the session if the player never confirms, so it doesn't linger
 * client-side forever.
 * <p>
 * Built on {@code @ApiStatus.Experimental} Paper API ({@link UncheckedSignChangeEvent}), which may change
 * between Paper releases.
 */
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
                // self-cancelling timer, needs to stay cancellable if the player submits first
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

    /** Used by {@code FoliaGUI.shutdown()}; skips the onComplete callback and any pending revert. */
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

        /** Legacy color codes, up to 4 lines; missing lines are left blank. */
        public @NotNull Builder lines(@NotNull String... lines) {
            List<Component> parsed = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                parsed.add(i < lines.length ? Text.of(lines[i]) : Component.empty());
            }
            this.lines = parsed;
            return this;
        }

        /** Sets a single line (1-4), legacy color codes, without touching the other 3. */
        public @NotNull Builder line(int lineNumber, @NotNull String text) {
            if (lineNumber < 1 || lineNumber > 4) {
                throw new IllegalArgumentException("lineNumber must be 1-4, was " + lineNumber);
            }
            this.lines.set(lineNumber - 1, Text.of(text));
            return this;
        }

        /**
         * Where the fake sign is placed, resolved fresh per {@link #open}. Defaults to 3 blocks beneath
         * the player's feet so solid ground occludes it from view, standing on an open cave/glass floor
         * will expose it. The client also enforces a distance limit, keep it reasonably close to the player.
         */
        public @NotNull Builder position(@NotNull Function<Player, Location> position) {
            this.position = position;
            return this;
        }

        /**
         * Called with 4 lines once the player submits the sign. Lines that still match what
         * {@link #lines} pre-filled (i.e. the player left them untouched) come back as {@code ""}
         * rather than the placeholder text, so this only reflects what was actually typed/changed.
         */
        public @NotNull Builder onComplete(@NotNull BiConsumer<Player, List<String>> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        /** Ticks before the fake sign is auto-reverted if the player never submits. {@code 0} disables the safety net. */
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
