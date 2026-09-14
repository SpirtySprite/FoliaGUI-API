package com.foliagui.builder.gui;

import com.foliagui.gui.Gui;
import com.foliagui.gui.GuiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public final class SimpleGuiBuilder extends BaseGuiBuilder<Gui, SimpleGuiBuilder> {

    private static final Logger LOGGER = Logger.getLogger(SimpleGuiBuilder.class.getName());

    private int rows = 1;
    private GuiType type;
    private boolean rowsExplicitlySet;

    public @NotNull SimpleGuiBuilder rows(int rows) {
        this.rows = rows;
        this.rowsExplicitlySet = true;
        return this;
    }

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
