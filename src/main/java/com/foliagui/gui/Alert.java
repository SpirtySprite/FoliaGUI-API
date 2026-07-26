package com.foliagui.gui;

import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/** Single-button "OK" popup; an acknowledgement rather than {@link Confirmation}'s yes/no choice. */
public final class Alert {

    private Alert() {
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "&8Notice";
        private GuiItem okItem = ItemBuilder.of(Material.LIME_CONCRETE)
                .name("&aOK").lore("&7Click to dismiss").asGuiItem();
        private Consumer<Player> onAcknowledge = player -> {
        };

        /** Legacy color codes. */
        public @NotNull Builder title(@NotNull String title) {
            this.title = title;
            return this;
        }

        /** Click action gets overwritten by the dialog. */
        public @NotNull Builder okItem(@NotNull GuiItem item) {
            this.okItem = item;
            return this;
        }

        public @NotNull Builder onAcknowledge(@NotNull Consumer<Player> onAcknowledge) {
            this.onAcknowledge = onAcknowledge;
            return this;
        }

        public @NotNull Gui build() {
            Gui gui = Gui.of(3, title);
            gui.filler().fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").asGuiItem());

            okItem.setAction(event -> {
                Player player = (Player) event.getWhoClicked();
                gui.close(player);
                onAcknowledge.accept(player);
            });

            gui.setItem(2, 5, okItem);
            return gui;
        }

        public void open(@NotNull Player player) {
            build().open(player);
        }
    }
}
