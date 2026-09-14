package com.foliagui.builder.gui;

import com.foliagui.gui.StorageGui;
import org.jetbrains.annotations.NotNull;

public final class StorageGuiBuilder extends BaseGuiBuilder<StorageGui, StorageGuiBuilder> {

    private int rows = 6;

    public @NotNull StorageGuiBuilder rows(int rows) {
        this.rows = rows;
        return this;
    }

    @Override
    public @NotNull StorageGui create() {
        return finish(new StorageGui(rows, title()));
    }
}
