package com.foliagui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextTest {

    @Test
    void ofParsesAmpersandCodes() {
        Component component = Text.of("&aHello");
        assertEquals("§aHello", Text.toLegacy(component));
    }

    @Test
    void ofPreservesExplicitItalic() {
        // Legacy colour codes reset prior formatting, so italic must come after the colour code to stick.
        Component italic = Text.of("&a&oHello");
        assertEquals(TextDecoration.State.TRUE, italic.decoration(TextDecoration.ITALIC));
    }

    @Test
    void labelDisablesTheImplicitItalicDefault() {
        // Minecraft renders custom item names/lore in italic by default; label() turns that default off.
        Component label = Text.label("&aHello");
        assertEquals(TextDecoration.State.FALSE, label.decoration(TextDecoration.ITALIC));
    }

    @Test
    void labelDoesNotOverrideAnExplicitItalicRequest() {
        Component label = Text.label("&a&oHello");
        assertEquals(TextDecoration.State.TRUE, label.decoration(TextDecoration.ITALIC));
    }

    @Test
    void miniParsesMiniMessageTags() {
        Component component = Text.mini("<red>Hello");
        assertEquals("§cHello", Text.toLegacy(component));
    }

    @Test
    void nullInputsReturnNull() {
        assertNull(Text.of(null));
        assertNull(Text.mini(null));
        assertNull(Text.label(null));
    }
}
