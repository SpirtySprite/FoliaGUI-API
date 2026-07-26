package com.foliagui.gui;

import com.foliagui.builder.gui.SimpleGuiBuilder;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/** Standard chest or typed GUI with a fixed layout: menus, confirmation dialogs, hand-placed shops. */
public class Gui extends BaseGui {

    /** Prefer {@link #builder()}. */
    public Gui(int rows, @NotNull Component title) {
        super(rows, title);
    }

    /** Hopper, dispenser, etc. Prefer {@link #builder()}. */
    public Gui(@NotNull GuiType type, @NotNull Component title) {
        super(type, title);
    }

    public static @NotNull SimpleGuiBuilder builder() {
        return new SimpleGuiBuilder();
    }

    /** Legacy color codes. */
    public static @NotNull Gui of(int rows, @NotNull String title) {
        return new Gui(rows, Text.of(title));
    }

    /** Legacy color codes. */
    public static @NotNull Gui of(@NotNull GuiType type, @NotNull String title) {
        return new Gui(type, Text.of(title));
    }
}
