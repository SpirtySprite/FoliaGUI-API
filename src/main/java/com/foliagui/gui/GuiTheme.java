package com.foliagui.gui;

import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Bundles the border, back-button, and close-button items most menus repeat. Each call builds a fresh
 * {@link GuiItem}, so a single instance is safe to share and reuse across every menu in a plugin.
 */
public final class GuiTheme {

    private static final Component BLANK_NAME = Text.label(" ");
    private static final Component BACK_NAME = Text.label("&e« Back");
    private static final Component CLOSE_NAME = Text.label("&cClose");

    private Supplier<GuiItem> border =
            () -> ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(BLANK_NAME).asGuiItem();
    private Supplier<GuiItem> backBase =
            () -> ItemBuilder.of(Material.ARROW).name(BACK_NAME).asGuiItem();
    private Supplier<GuiItem> closeBase =
            () -> ItemBuilder.of(Material.BARRIER).name(CLOSE_NAME).asGuiItem();

    public @NotNull GuiTheme border(@NotNull Supplier<GuiItem> border) {
        this.border = border;
        return this;
    }

    public @NotNull GuiTheme backButtonItem(@NotNull Supplier<GuiItem> backBase) {
        this.backBase = backBase;
        return this;
    }

    public @NotNull GuiTheme closeButtonItem(@NotNull Supplier<GuiItem> closeBase) {
        this.closeBase = closeBase;
        return this;
    }

    public @NotNull GuiItem border() {
        return border.get();
    }

    public void applyBorder(@NotNull BaseGui gui) {
        gui.filler().fillBorder(border());
    }

    /** Wires the click action to {@link GuiNavigator#back}. */
    public @NotNull GuiItem backButton() {
        GuiItem item = backBase.get();
        item.setAction(event -> GuiNavigator.back(event.getWhoClicked()));
        return item;
    }

    public @NotNull GuiItem closeButton(@NotNull BaseGui gui) {
        GuiItem item = closeBase.get();
        item.setAction(event -> {
            GuiNavigator.clear(event.getWhoClicked());
            gui.close(event.getWhoClicked());
        });
        return item;
    }
}
