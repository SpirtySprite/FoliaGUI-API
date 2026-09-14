package com.foliagui.gui;

import com.foliagui.builder.gui.ScrollingGuiBuilder;
import com.foliagui.item.GuiItem;
import com.foliagui.util.Slot;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ScrollingGui extends BaseGui {

    private final ScrollType scrollType;
    private final List<GuiItem> content = new CopyOnWriteArrayList<>();
    private final Map<Integer, GuiItem> currentView = new ConcurrentHashMap<>();
    private volatile int[] cachedRegionRows;
    private volatile int[] cachedRegionColumns;
    private final AtomicInteger offset = new AtomicInteger();

    public ScrollingGui(int rows, @NotNull Component title, @NotNull ScrollType scrollType) {
        super(rows, title);
        this.scrollType = scrollType;
    }

    public static @NotNull ScrollingGuiBuilder builder() {
        return new ScrollingGuiBuilder();
    }

    public @NotNull ScrollingGui addContent(@NotNull GuiItem item) {
        content.add(item);
        return this;
    }

    public @NotNull ScrollingGui addContent(@NotNull GuiItem... items) {
        for (GuiItem item : items) {
            content.add(item);
        }
        return this;
    }

    public @NotNull ScrollingGui addContent(@NotNull Collection<GuiItem> items) {
        content.addAll(items);
        return this;
    }

    public @NotNull ScrollingGui clearContent() {
        content.clear();
        return this;
    }

    public @NotNull List<GuiItem> getContent() {
        return content;
    }

    public @NotNull ScrollType getScrollType() {
        return scrollType;
    }

    public boolean canScrollNext() {
        return offset.get() < maxOffset();
    }

    public boolean canScrollPrevious() {
        return offset.get() > 0;
    }

    public boolean scrollNext() {
        int max = maxOffset();
        int previousValue = offset.getAndUpdate(current -> current < max ? current + 1 : current);
        boolean scrolled = previousValue < max;
        if (scrolled) {
            update();
        }
        return scrolled;
    }

    public boolean scrollPrevious() {
        int previousValue = offset.getAndUpdate(current -> current > 0 ? current - 1 : current);
        boolean scrolled = previousValue > 0;
        if (scrolled) {
            update();
        }
        return scrolled;
    }

    public void scrollToEnd() {
        offset.set(maxOffset());
        update();
    }

    @Override
    protected void populateInventory() {
        super.populateInventory();

        int[] rows = regionRows();
        int[] cols = regionColumns();
        int width = cols.length;
        int height = rows.length;
        if (width == 0 || height == 0 || content.isEmpty()) {
            currentView.clear();
            return;
        }

        int currentOffset = offset.get();
        if (scrollType == ScrollType.VERTICAL) {
            for (int r = 0; r < height; r++) {
                int contentRow = currentOffset + r;
                for (int c = 0; c < width; c++) {
                    int index = contentRow * width + c;
                    place(index, rows[r], cols[c]);
                }
            }
        } else {
            for (int c = 0; c < width; c++) {
                int contentCol = currentOffset + c;
                for (int r = 0; r < height; r++) {
                    int index = contentCol * height + r;
                    place(index, rows[r], cols[c]);
                }
            }
        }
    }

    private void place(int index, int row, int column) {
        int slot = Slot.of(row, column);
        if (getGuiItem(slot) != null) {
            return;
        }
        GuiItem item = (index >= 0 && index < content.size()) ? content.get(index) : null;
        if (item != null) {
            currentView.put(slot, item);
        } else {
            currentView.remove(slot);
        }
        ItemStack desired = item != null ? item.getItemStack().clone() : null;
        if (!Objects.equals(getInventory().getItem(slot), desired)) {
            getInventory().setItem(slot, desired);
        }
    }

    @Override
    public @Nullable GuiItem itemAt(int slot) {
        GuiItem view = currentView.get(slot);
        return view != null ? view : super.itemAt(slot);
    }

    @Override
    protected void onLayoutChanged() {
        cachedRegionRows = null;
        cachedRegionColumns = null;
    }

    private int[] regionRows() {
        int[] cached = cachedRegionRows;
        if (cached != null) {
            return cached;
        }
        TreeSet<Integer> rows = new TreeSet<>();
        for (int slot = 0; slot < getSize(); slot++) {
            if (getGuiItem(slot) == null) {
                rows.add(Slot.rowOf(slot));
            }
        }
        int[] computed = rows.stream().mapToInt(Integer::intValue).toArray();
        cachedRegionRows = computed;
        return computed;
    }

    private int[] regionColumns() {
        int[] cached = cachedRegionColumns;
        if (cached != null) {
            return cached;
        }
        TreeSet<Integer> cols = new TreeSet<>();
        for (int slot = 0; slot < getSize(); slot++) {
            if (getGuiItem(slot) == null) {
                cols.add(Slot.columnOf(slot));
            }
        }
        int[] computed = cols.stream().mapToInt(Integer::intValue).toArray();
        cachedRegionColumns = computed;
        return computed;
    }

    private int maxOffset() {
        int width = regionColumns().length;
        int height = regionRows().length;
        if (width == 0 || height == 0) {
            return 0;
        }
        if (scrollType == ScrollType.VERTICAL) {
            int totalRows = (int) Math.ceil(content.size() / (double) width);
            return Math.max(0, totalRows - height);
        }
        int totalCols = (int) Math.ceil(content.size() / (double) height);
        return Math.max(0, totalCols - width);
    }
}
