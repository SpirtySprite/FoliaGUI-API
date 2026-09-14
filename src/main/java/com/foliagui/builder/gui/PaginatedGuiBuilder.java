package com.foliagui.builder.gui;

import com.foliagui.gui.GuiType;
import com.foliagui.gui.PaginatedGui;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PaginatedGuiBuilder extends BaseGuiBuilder<PaginatedGui, PaginatedGuiBuilder> {

    private int rows = 6;
    private GuiType type;
    private int pageSize;

    public @NotNull PaginatedGuiBuilder rows(int rows) {
        this.rows = rows;
        return this;
    }

    public @NotNull PaginatedGuiBuilder type(@Nullable GuiType type) {
        this.type = type;
        return this;
    }

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
