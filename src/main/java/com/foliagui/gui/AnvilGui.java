package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class AnvilGui {

    private static final int RESULT_SLOT = 2;

    private static final SessionRegistry<AnvilGui> SESSIONS = new SessionRegistry<>();

    private final Component title;
    private final ItemStack leftItem;
    private final ItemStack rightItem;
    private final BiFunction<Player, String, Response> onComplete;
    private final Consumer<Player> onClose;
    private final boolean forceOpen;
    private final Set<UUID> allowedCloses = ConcurrentHashMap.newKeySet();

    private AnvilGui(Builder builder) {
        this.title = builder.title;
        this.leftItem = builder.leftItem;
        this.rightItem = builder.rightItem;
        this.onComplete = builder.onComplete;
        this.onClose = builder.onClose;
        this.forceOpen = builder.forceOpen;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static boolean hasSession(@NotNull HumanEntity player) {
        return SESSIONS.has(player);
    }

    public void open(@NotNull Player player) {
        FoliaGUI.scheduler().runForEntity(player, () -> {
            AnvilView view = MenuType.ANVIL.create(player, title);
            view.setRepairCost(0);
            view.setMaximumRepairCost(Integer.MAX_VALUE);
            Inventory top = view.getTopInventory();
            top.setItem(0, leftItem);
            if (rightItem != null) {
                top.setItem(1, rightItem);
            }
            SESSIONS.put(player, this);
            player.openInventory(view);
        }, null);
    }

    @ApiStatus.Internal
    public static boolean handleClick(@NotNull InventoryClickEvent event) {
        AnvilGui gui = SESSIONS.get(event.getWhoClicked());
        if (gui == null || !(event.getView() instanceof AnvilView view)) {
            return false;
        }
        event.setCancelled(true);

        Inventory clicked = event.getClickedInventory();
        if (clicked != null && clicked.equals(view.getTopInventory()) && event.getSlot() == RESULT_SLOT) {
            String text = view.getRenameText();
            Player player = (Player) event.getWhoClicked();
            Response response = gui.onComplete.apply(player, text == null ? "" : text);
            gui.apply(player, view, response);
        }
        return true;
    }

    @ApiStatus.Internal
    public static boolean handleDrag(@NotNull InventoryDragEvent event) {
        AnvilGui gui = SESSIONS.get(event.getWhoClicked());
        if (gui == null) {
            return false;
        }
        event.setCancelled(true);
        return true;
    }

    public static void clearSessions() {
        SESSIONS.clear();
    }

    @ApiStatus.Internal
    public static boolean handleClose(@NotNull InventoryCloseEvent event) {
        HumanEntity player = event.getPlayer();
        AnvilGui gui = SESSIONS.remove(player);
        if (gui == null) {
            return false;
        }
        boolean allowed = gui.allowedCloses.remove(player.getUniqueId());
        if (gui.onClose != null) {
            gui.onClose.accept((Player) player);
        }
        if (gui.forceOpen && !allowed) {
            FoliaGUI.scheduler().runForEntity(player, () -> gui.open((Player) player), null);
        }
        return true;
    }

    private void apply(@NotNull Player player, @NotNull AnvilView view, @NotNull Response response) {
        if (response.close) {
            allowedCloses.add(player.getUniqueId());
            FoliaGUI.scheduler().runForEntity(player, player::closeInventory, null);
        } else if (response.newText != null) {
            ItemStack left = view.getTopInventory().getItem(0);
            if (left != null) {
                ItemMeta meta = left.getItemMeta();
                if (meta != null) {
                    meta.displayName(Text.label(response.newText));
                    left.setItemMeta(meta);
                    view.getTopInventory().setItem(0, left);
                }
            }
        }
    }

    public static final class Response {
        private final boolean close;
        private final String newText;

        private Response(boolean close, @Nullable String newText) {
            this.close = close;
            this.newText = newText;
        }

        public static @NotNull Response close() {
            return new Response(true, null);
        }

        public static @NotNull Response keepOpen() {
            return new Response(false, null);
        }

        public static @NotNull Response text(@NotNull String newText) {
            return new Response(false, newText);
        }
    }

    public static final class Builder {
        private Component title = Component.empty();
        private ItemStack leftItem = defaultInput("");
        private ItemStack rightItem;
        private BiFunction<Player, String, Response> onComplete = (player, text) -> Response.close();
        private Consumer<Player> onClose;
        private boolean forceOpen;

        private static ItemStack defaultInput(@NotNull String text) {
            ItemStack stack = new ItemStack(Material.PAPER);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Text.label(text));
                stack.setItemMeta(meta);
            }
            return stack;
        }

        public @NotNull Builder title(@NotNull String title) {
            this.title = Text.of(title);
            return this;
        }

        public @NotNull Builder title(@NotNull Component title) {
            this.title = title;
            return this;
        }

        public @NotNull Builder text(@NotNull String text) {
            this.leftItem = defaultInput(text);
            return this;
        }

        public @NotNull Builder itemLeft(@NotNull ItemStack item) {
            this.leftItem = item;
            return this;
        }

        public @NotNull Builder itemRight(@NotNull ItemStack item) {
            this.rightItem = item;
            return this;
        }

        public @NotNull Builder onComplete(@NotNull BiFunction<Player, String, Response> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        public @NotNull Builder onClose(@NotNull Consumer<Player> onClose) {
            this.onClose = onClose;
            return this;
        }

        public @NotNull Builder forceOpen(boolean forceOpen) {
            this.forceOpen = forceOpen;
            return this;
        }

        public @NotNull AnvilGui build() {
            return new AnvilGui(this);
        }

        public void open(@NotNull Player player) {
            build().open(player);
        }
    }
}
