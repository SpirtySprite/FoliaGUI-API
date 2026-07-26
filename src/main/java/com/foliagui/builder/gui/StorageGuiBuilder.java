package com.foliagui.builder.gui;

import com.foliagui.gui.StorageGui;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for a {@link StorageGui}. Interactions are already unlocked by the storage GUI itself, so this
 * just configures rows, title, and the usual callbacks.
 */
public final class StorageGuiBuilder extends BaseGuiBuilder<StorageGui, StorageGuiBuilder> {

    private int rows = 6;

    /** Sets the chest row count (1-6). */
    public @NotNull StorageGuiBuilder rows(int rows) {
        this.rows = rows;
        return this;
    }

    @Override
    public @NotNull StorageGui create() {
        return finish(new StorageGui(rows, title()));
    }
}
