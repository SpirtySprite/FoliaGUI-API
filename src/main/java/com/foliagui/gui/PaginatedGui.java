package com.foliagui.gui;

import com.foliagui.builder.gui.PaginatedGuiBuilder;
import com.foliagui.item.GuiItem;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/** Static items placed with {@code setItem} stay put on every page; {@link #addPageItem} items are paged automatically. */
public class PaginatedGui extends BaseGui {

    private final List<GuiItem> pageItems = Collections.synchronizedList(new ArrayList<>());
    private final Map<Integer, GuiItem> currentPage = new ConcurrentHashMap<>();
    private final Map<Integer, GuiItem> suppliedItems = new ConcurrentHashMap<>();
    private volatile List<Integer> cachedPageSlots;
    private final AtomicInteger pageNum = new AtomicInteger(); // 0-indexed
    private volatile int pageSize;     // 0 => auto (use all empty slots)
    private volatile int suppliedItemCount;
    private volatile IntFunction<GuiItem> pageItemSupplier;

    public PaginatedGui(int rows, @NotNull Component title, int pageSize) {
        super(rows, title);
        this.pageSize = Math.max(0, pageSize);
    }

    public PaginatedGui(@NotNull GuiType type, @NotNull Component title, int pageSize) {
        super(type, title);
        this.pageSize = Math.max(0, pageSize);
    }

    public static @NotNull PaginatedGuiBuilder builder() {
        return new PaginatedGuiBuilder();
    }

    public @NotNull PaginatedGui addPageItem(@NotNull GuiItem item) {
        pageItems.add(item);
        return this;
    }

    public @NotNull PaginatedGui addPageItem(@NotNull GuiItem... items) {
        for (GuiItem item : items) {
            pageItems.add(item);
        }
        return this;
    }

    public @NotNull PaginatedGui addPageItem(@NotNull Collection<GuiItem> items) {
        pageItems.addAll(items);
        return this;
    }

    /** Static items are unaffected. */
    public @NotNull PaginatedGui clearPageItems() {
        pageItems.clear();
        suppliedItems.clear();
        suppliedItemCount = 0;
        pageItemSupplier = null;
        return this;
    }

    public @NotNull List<GuiItem> getPageItems() {
        return pageItems;
    }

    public int getPageItemsCount() {
        return pageItemSupplier == null ? pageItems.size() : suppliedItemCount;
    }

    public @NotNull PaginatedGui setPageItemSupplier(int itemCount, @NotNull IntFunction<GuiItem> supplier) {
        pageItems.clear();
        suppliedItems.clear();
        suppliedItemCount = Math.max(0, itemCount);
        pageItemSupplier = Objects.requireNonNull(supplier, "supplier cannot be null");
        pageNum.set(0);
        return this;
    }

    /** {@code 0} means "use every empty slot". */
    public @NotNull PaginatedGui setPageSize(int pageSize) {
        this.pageSize = Math.max(0, pageSize);
        return this;
    }

    /** 1-indexed. */
    public int getCurrentPage() {
        return pageNum.get() + 1;
    }

    public int getPagesCount() {
        int perPage = perPage();
        if (perPage <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(getPageItemsCount() / (double) perPage));
    }

    public boolean hasNext() {
        return pageNum.get() + 1 < getPagesCount();
    }

    public boolean hasPrevious() {
        return pageNum.get() > 0;
    }

    /** Safe to call concurrently; if two callers race, at most one actually advances. */
    public boolean next() {
        int pageCount = getPagesCount();
        int previousValue = pageNum.getAndUpdate(current -> current + 1 < pageCount ? current + 1 : current);
        boolean advanced = previousValue + 1 < pageCount;
        if (advanced) {
            update();
        }
        return advanced;
    }

    /** Safe to call concurrently; if two callers race, at most one actually moves back. */
    public boolean previous() {
        int previousValue = pageNum.getAndUpdate(current -> current > 0 ? current - 1 : current);
        boolean moved = previousValue > 0;
        if (moved) {
            update();
        }
        return moved;
    }

    /** 1-indexed, clamped to the valid range. */
    public @NotNull PaginatedGui openPage(int page) {
        pageNum.set(Math.max(0, Math.min(page - 1, getPagesCount() - 1)));
        update();
        return this;
    }

    public @NotNull PaginatedGui openLastPage() {
        return openPage(getPagesCount());
    }

    /** {@code page} is 1-indexed. */
    public void open(@NotNull HumanEntity player, int page) {
        pageNum.set(Math.max(0, Math.min(page - 1, getPagesCount() - 1)));
        open(player);
    }

    /** Non-numeric input, or no answer within 20 seconds, leaves the current page unchanged. */
    public void promptJumpToPage(@NotNull Player player) {
        ChatPrompt.ask(player, "&eType a page number (1-" + getPagesCount() + "):", 20 * 20, input -> {
            if (input == null) {
                return;
            }
            try {
                openPage(Integer.parseInt(input.trim()));
            } catch (NumberFormatException e) {
                player.sendMessage(Text.of("&cThat's not a number."));
            }
            open(player);
        });
    }

    @Override
    protected void populateInventory() {
        List<Integer> slots = pageSlots();
        boolean[] pagedSlots = new boolean[getSize()];
        for (int slot : slots) {
            pagedSlots[slot] = true;
        }
        for (int slot = 0; slot < getSize(); slot++) {
            if (!pagedSlots[slot]) {
                applyItem(slot, getGuiItem(slot));
            }
        }
        int perPage = pageSize > 0 ? Math.min(pageSize, slots.size()) : slots.size();
        int start = pageNum.get() * perPage;

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            int itemIndex = start + i;
            GuiItem item = perPage > 0 && i < perPage && itemIndex < getPageItemsCount()
                    ? pageItem(itemIndex) : null;
            if (item != null) {
                currentPage.put(slot, item);
            } else {
                currentPage.remove(slot);
            }
            applyItem(slot, item);
        }
    }

    @Override
    public @Nullable GuiItem itemAt(int slot) {
        GuiItem paged = currentPage.get(slot);
        return paged != null ? paged : super.itemAt(slot);
    }

    @Override
    protected void onLayoutChanged() {
        cachedPageSlots = null;
    }

    /** Cached; only recomputed when the static item map changes, since page turns and auto-refresh would otherwise rescan every slot. */
    protected @NotNull List<Integer> pageSlots() {
        List<Integer> cached = cachedPageSlots;
        if (cached != null) {
            return cached;
        }
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < getSize(); slot++) {
            if (getGuiItem(slot) == null) {
                slots.add(slot);
            }
        }
        cachedPageSlots = slots;
        return slots;
    }

    private int perPage() {
        return pageSize > 0 ? pageSize : pageSlots().size();
    }

    private @Nullable GuiItem pageItem(int index) {
        IntFunction<GuiItem> supplier = pageItemSupplier;
        if (supplier == null) {
            return pageItems.get(index);
        }
        return suppliedItems.computeIfAbsent(index, supplier::apply);
    }
}
