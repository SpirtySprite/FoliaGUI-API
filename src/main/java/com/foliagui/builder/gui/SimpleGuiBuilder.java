package com.foliagui.builder.gui;

import com.foliagui.gui.Gui;
import com.foliagui.gui.GuiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Builder for a standard {@link Gui}. Choose either a chest sized by {@link #rows(int)} (default 1) or a
 * typed inventory via {@link #type(GuiType)}.
 */
public final class SimpleGuiBuilder extends BaseGuiBuilder<Gui, SimpleGuiBuilder> {

    private static final Logger LOGGER = Logger.getLogger(SimpleGuiBuilder.class.getName());

    private int rows = 1;
    private GuiType type; // null => chest
    private boolean rowsExplicitlySet;

    /** Sets the chest row count (1-6). Ignored if a {@link #type(GuiType)} is set. */
    public @NotNull SimpleGuiBuilder rows(int rows) {
        this.rows = rows;
        this.rowsExplicitlySet = true;
        return this;
    }

    /** Makes this a typed GUI (hopper, dispenser, …) instead of a chest. */
    public @NotNull SimpleGuiBuilder type(@Nullable GuiType type) {
        this.type = type;
        return this;
    }

    @Override
    public @NotNull Gui create() {
        if (type != null && rowsExplicitlySet) {
            LOGGER.warning("SimpleGuiBuilder: rows(" + rows + ") is ignored because type(" + type
                    + ") is set; typed GUIs are sized by their type, not by row count.");
        }
        Gui gui = type == null ? new Gui(rows, title()) : new Gui(type, title());
        return finish(gui);
    }
}
