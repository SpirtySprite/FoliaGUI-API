package com.foliagui.builder.gui;

import com.foliagui.gui.ScrollType;
import com.foliagui.gui.ScrollingGui;
import org.jetbrains.annotations.NotNull;

public final class ScrollingGuiBuilder extends BaseGuiBuilder<ScrollingGui, ScrollingGuiBuilder> {

    private int rows = 6;
    private ScrollType scrollType = ScrollType.VERTICAL;

    public @NotNull ScrollingGuiBuilder rows(int rows) {
        this.rows = rows;
        return this;
    }

    public @NotNull ScrollingGuiBuilder scrollType(@NotNull ScrollType scrollType) {
        this.scrollType = scrollType;
        return this;
    }

    @Override
    public @NotNull ScrollingGui create() {
        return finish(new ScrollingGui(rows, title(), scrollType));
    }
}
