package com.foliagui.builder.gui;

import com.foliagui.gui.GuiType;
import com.foliagui.gui.PaginatedGui;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builder for a {@link PaginatedGui}. Configure the chest rows (or a {@link GuiType}) and an optional fixed
 * page size; a page size of {@code 0} lets pages use every empty slot.
 */
public final class PaginatedGuiBuilder extends BaseGuiBuilder<PaginatedGui, PaginatedGuiBuilder> {

    private int rows = 6;
    private GuiType type;
    private int pageSize;

    /** Sets the chest row count (1-6). */
    public @NotNull PaginatedGuiBuilder rows(int rows) {
        this.rows = rows;
        return this;
    }

    /** Makes this a typed GUI instead of a chest. */
    public @NotNull PaginatedGuiBuilder type(@Nullable GuiType type) {
        this.type = type;
        return this;
    }

    /** Fixes how many items appear per page. {@code 0} (default) means "use every empty slot". */
    public @NotNull PaginatedGuiBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    @Override
    public @NotNull PaginatedGui create() {
        PaginatedGui gui = type == null
                ? new PaginatedGui(rows, title(), pageSize)
                : new PaginatedGui(type, title(), pageSize);
        return finish(gui);
    }
}
