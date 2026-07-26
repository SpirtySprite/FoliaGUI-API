package com.foliagui.gui;

import com.foliagui.item.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

/**
 * A {@link PaginatedGui} filtered by a search term. Items added via {@link #addSearchableItem} live in a
 * master list separate from the paged content; {@link #search(String)} repopulates the page with matches.
 */
public class SearchablePaginatedGui extends PaginatedGui {

    private final List<GuiItem> master = new CopyOnWriteArrayList<>();
    private final Map<GuiItem, String> searchKeys = new ConcurrentHashMap<>();
    private volatile BiPredicate<GuiItem, String> matcher = (item, term) -> {
        String key = searchKeys.get(item);
        return key != null && key.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
    };
    private volatile String currentTerm = "";

    public SearchablePaginatedGui(int rows, @NotNull Component title, int pageSize) {
        super(rows, title, pageSize);
    }

    public SearchablePaginatedGui(@NotNull GuiType type, @NotNull Component title, int pageSize) {
        super(type, title, pageSize);
    }

    /** Shown immediately unless a search is currently active and {@code searchKey} doesn't match it. */
    public @NotNull SearchablePaginatedGui addSearchableItem(@NotNull GuiItem item, @NotNull String searchKey) {
        master.add(item);
        searchKeys.put(item, searchKey);
        if (currentTerm.isBlank() || matcher.test(item, currentTerm)) {
            addPageItem(item);
        }
        return this;
    }

    /** Default matcher is case-insensitive substring match. */
    public @NotNull SearchablePaginatedGui matcher(@NotNull BiPredicate<GuiItem, String> matcher) {
        this.matcher = matcher;
        return this;
    }

    /** Blank {@code term} clears the filter. */
    public @NotNull SearchablePaginatedGui search(@NotNull String term) {
        this.currentTerm = term;
        clearPageItems();
        for (GuiItem item : master) {
            if (term.isBlank() || matcher.test(item, term)) {
                addPageItem(item);
            }
        }
        openPage(1);
        return this;
    }

    public @NotNull SearchablePaginatedGui clearSearch() {
        return search("");
    }

    public @NotNull String getCurrentSearchTerm() {
        return currentTerm;
    }

    /** The special term {@code "clear"} resets the filter instead of searching for it. */
    public void promptSearch(@NotNull Player player) {
        ChatPrompt.ask(player, "&eType a search term (or 'clear'):", 0, term -> {
            if (term == null) {
                return;
            }
            if (term.equalsIgnoreCase("clear")) {
                clearSearch();
            } else {
                search(term);
            }
            open(player);
        });
    }
}
