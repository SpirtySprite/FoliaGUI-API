package com.foliagui.builder.gui;

import com.foliagui.gui.ScrollType;
import com.foliagui.gui.ScrollingGui;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for a {@link ScrollingGui}. Configure the chest rows and the scroll direction.
 */
public final class ScrollingGuiBuilder extends BaseGuiBuilder<ScrollingGui, ScrollingGuiBuilder> {

    private int rows = 6;
    private ScrollType scrollType = ScrollType.VERTICAL;

    /** Sets the chest row count (1-6). */
    public @NotNull ScrollingGuiBuilder rows(int rows) {
        this.rows = rows;
        return this;
    }

    /** Sets the scroll direction (default {@link ScrollType#VERTICAL}). */
    public @NotNull ScrollingGuiBuilder scrollType(@NotNull ScrollType scrollType) {
        this.scrollType = scrollType;
        return this;
    }

    @Override
    public @NotNull ScrollingGui create() {
        return finish(new ScrollingGui(rows, title(), scrollType));
    }
}
