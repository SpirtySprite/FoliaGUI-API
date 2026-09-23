package com.foliagui.gui;

import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public final class GuiTheme {

    private static final Component BLANK_NAME = Text.label(" ");
    private static final Component BACK_NAME = Text.label("&e« Back");
    private static final Component CLOSE_NAME = Text.label("&cClose");
    private static final Component PREVIOUS_NAME = Text.label("&e« Previous page");
    private static final Component NEXT_NAME = Text.label("&eNext page »");
    private static final Component LOADING_NAME = Text.label("&7Loading…");
    private static final Component FAILED_NAME = Text.label("&cCould not load this menu");

    public record ThemeSound(@NotNull Sound sound, float volume, float pitch) {
        public void play(@NotNull HumanEntity viewer) {
            if (viewer instanceof Player player) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    private Supplier<GuiItem> border =
            () -> ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(BLANK_NAME).asGuiItem();
    private Supplier<GuiItem> filler =
            () -> ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(BLANK_NAME).asGuiItem();
    private Supplier<GuiItem> backBase =
            () -> ItemBuilder.of(Material.ARROW).name(BACK_NAME).asGuiItem();
    private Supplier<GuiItem> closeBase =
            () -> ItemBuilder.of(Material.BARRIER).name(CLOSE_NAME).asGuiItem();
    private Function<PaginatedGui, GuiItem> previousBase =
            gui -> ItemBuilder.of(Material.ARROW).name(PREVIOUS_NAME).asGuiItem();
    private Function<PaginatedGui, GuiItem> nextBase =
            gui -> ItemBuilder.of(Material.ARROW).name(NEXT_NAME).asGuiItem();
    private Function<PaginatedGui, GuiItem> pageIndicator = gui -> ItemBuilder.of(Material.PAPER)
            .name(Text.label("&fPage &e" + gui.getCurrentPage() + "&7/&e" + gui.getPagesCount()))
            .amount(Math.min(64, gui.getCurrentPage()))
            .asGuiItem();
    private Supplier<GuiItem> loading =
            () -> ItemBuilder.of(Material.CLOCK).name(LOADING_NAME).asGuiItem();
    private Supplier<GuiItem> loadFailed =
            () -> ItemBuilder.of(Material.BARRIER).name(FAILED_NAME).asGuiItem();
    private @Nullable ThemeSound clickSound = new ThemeSound(Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    private @Nullable ThemeSound successSound = new ThemeSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
    private @Nullable ThemeSound denySound = new ThemeSound(Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
    private @Nullable ThemeSound pageSound = new ThemeSound(Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.0f);

    public @NotNull GuiTheme border(@NotNull Supplier<GuiItem> border) {
        this.border = border;
        return this;
    }

    public @NotNull GuiTheme filler(@NotNull Supplier<GuiItem> filler) {
        this.filler = filler;
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

    public @NotNull GuiTheme previousButtonItem(@NotNull Supplier<GuiItem> previousBase) {
        this.previousBase = gui -> previousBase.get();
        return this;
    }

    public @NotNull GuiTheme previousButtonItem(@NotNull Function<PaginatedGui, GuiItem> previousBase) {
        this.previousBase = previousBase;
        return this;
    }

    public @NotNull GuiTheme nextButtonItem(@NotNull Supplier<GuiItem> nextBase) {
        this.nextBase = gui -> nextBase.get();
        return this;
    }

    public @NotNull GuiTheme nextButtonItem(@NotNull Function<PaginatedGui, GuiItem> nextBase) {
        this.nextBase = nextBase;
        return this;
    }

    public @NotNull GuiTheme pageIndicator(@NotNull Function<PaginatedGui, GuiItem> pageIndicator) {
        this.pageIndicator = pageIndicator;
        return this;
    }

    public @NotNull GuiTheme loadingItem(@NotNull Supplier<GuiItem> loading) {
        this.loading = loading;
        return this;
    }

    public @NotNull GuiTheme loadFailedItem(@NotNull Supplier<GuiItem> loadFailed) {
        this.loadFailed = loadFailed;
        return this;
    }

    public @NotNull GuiTheme clickSound(@Nullable ThemeSound sound) {
        this.clickSound = sound;
        return this;
    }

    public @NotNull GuiTheme successSound(@Nullable ThemeSound sound) {
        this.successSound = sound;
        return this;
    }

    public @NotNull GuiTheme denySound(@Nullable ThemeSound sound) {
        this.denySound = sound;
        return this;
    }

    public @NotNull GuiTheme pageSound(@Nullable ThemeSound sound) {
        this.pageSound = sound;
        return this;
    }

    public @NotNull GuiItem border() {
        return border.get();
    }

    public @NotNull GuiItem filler() {
        return filler.get();
    }

    public @NotNull GuiItem loading() {
        return loading.get();
    }

    public @NotNull GuiItem loadFailed() {
        return loadFailed.get();
    }

    public void applyBorder(@NotNull BaseGui gui) {
        gui.filler().fillBorder(border());
    }

    public void applyFiller(@NotNull BaseGui gui) {
        gui.filler().fill(filler());
    }

    public @NotNull GuiItem backButton() {
        GuiItem item = backBase.get();
        item.setAction(event -> {
            click(event.getWhoClicked());
            GuiNavigator.back(event.getWhoClicked());
        });
        return item;
    }

    public @NotNull GuiItem backButton(@NotNull Runnable back) {
        GuiItem item = backBase.get();
        item.setAction(event -> {
            click(event.getWhoClicked());
            back.run();
        });
        return item;
    }

    public @NotNull GuiItem closeButton(@NotNull BaseGui gui) {
        GuiItem item = closeBase.get();
        item.setAction(event -> {
            click(event.getWhoClicked());
            GuiNavigator.clear(event.getWhoClicked());
            gui.close(event.getWhoClicked());
        });
        return item;
    }

    public @NotNull GuiItem previousButton(@NotNull PaginatedGui gui) {
        GuiItem item = previousBase.apply(gui);
        item.setAction(event -> {
            if (gui.previous()) {
                page(event.getWhoClicked());
            }
        });
        return item;
    }

    public @NotNull GuiItem nextButton(@NotNull PaginatedGui gui) {
        GuiItem item = nextBase.apply(gui);
        item.setAction(event -> {
            if (gui.next()) {
                page(event.getWhoClicked());
            }
        });
        return item;
    }

    public @NotNull GuiItem pageIndicator(@NotNull PaginatedGui gui) {
        return pageIndicator.apply(gui);
    }

    public void click(@NotNull HumanEntity viewer) {
        play(clickSound, viewer);
    }

    public void success(@NotNull HumanEntity viewer) {
        play(successSound, viewer);
    }

    public void deny(@NotNull HumanEntity viewer) {
        play(denySound, viewer);
    }

    public void page(@NotNull HumanEntity viewer) {
        play(pageSound, viewer);
    }

    private static void play(@Nullable ThemeSound sound, @NotNull HumanEntity viewer) {
        if (sound != null) {
            sound.play(viewer);
        }
    }
}
