package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class Confirmation {

    private Confirmation() {
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "&8Are you sure?";
        private GuiItem confirmItem = ItemBuilder.of(Material.LIME_CONCRETE)
                .name("&aConfirm").lore("&7Click to confirm").asGuiItem();
        private GuiItem cancelItem = ItemBuilder.of(Material.RED_CONCRETE)
                .name("&cCancel").lore("&7Click to cancel").asGuiItem();
        private Consumer<Player> onConfirm = player -> {
        };
        private Consumer<Player> onCancel = player -> {
        };
        private long expireTicks;
        private Consumer<Player> onExpire = player -> {
        };

        public @NotNull Builder title(@NotNull String title) {
            this.title = title;
            return this;
        }

        public @NotNull Builder confirmItem(@NotNull GuiItem item) {
            this.confirmItem = item;
            return this;
        }

        public @NotNull Builder cancelItem(@NotNull GuiItem item) {
            this.cancelItem = item;
            return this;
        }

        public @NotNull Builder onConfirm(@NotNull Consumer<Player> onConfirm) {
            this.onConfirm = onConfirm;
            return this;
        }

        public @NotNull Builder onCancel(@NotNull Consumer<Player> onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        public @NotNull Builder expireAfter(long ticks, @NotNull Consumer<Player> onExpire) {
            this.expireTicks = Math.max(0, ticks);
            this.onExpire = onExpire;
            return this;
        }

        public @NotNull Gui build() {
            Gui gui = Gui.of(3, title);
            gui.filler().fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").asGuiItem());

            java.util.concurrent.atomic.AtomicBoolean decided = new java.util.concurrent.atomic.AtomicBoolean();
            GuiItem confirm = confirmItem.withItemStack(confirmItem.getItemStack());
            GuiItem cancel = cancelItem.withItemStack(cancelItem.getItemStack());
            confirm.setAction(event -> {
                Player player = (Player) event.getWhoClicked();
                if (!decided.compareAndSet(false, true)) {
                    return;
                }
                gui.close(player);
                onConfirm.accept(player);
            });
            cancel.setAction(event -> {
                Player player = (Player) event.getWhoClicked();
                if (!decided.compareAndSet(false, true)) {
                    return;
                }
                gui.close(player);
                onCancel.accept(player);
            });

            gui.setItem(2, 3, confirm);
            gui.setItem(2, 7, cancel);
            return gui;
        }

        public void open(@NotNull Player player) {
            Gui gui = build();
            gui.open(player);
            if (expireTicks > 0) {
                FoliaGUI.scheduler().runForEntityLater(player, () -> {
                    if (GuiManager.getOpenGui(player) == gui) {
                        gui.close(player);
                        onExpire.accept(player);
                    }
                }, null, expireTicks);
            }
        }
    }
}
