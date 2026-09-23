package com.foliagui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Text {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Text() {
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable Component mini(@Nullable String miniMessage) {
        if (miniMessage == null) {
            return null;
        }
        return MINI.deserialize(miniMessage).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static @NotNull List<Component> miniList(@NotNull List<String> lines) {
        return lines.stream().map(Text::mini).collect(Collectors.toList());
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable Component of(@Nullable String legacy) {
        if (legacy == null) {
            return null;
        }
        Component parsed = legacy.indexOf('§') >= 0 ? SECTION.deserialize(legacy) : AMPERSAND.deserialize(legacy);
        return parsed.decoration(TextDecoration.ITALIC, parsed.hasDecoration(TextDecoration.ITALIC));
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable Component label(@Nullable String legacy) {
        if (legacy == null) {
            return null;
        }
        Component parsed = legacy.indexOf('§') >= 0 ? SECTION.deserialize(legacy) : AMPERSAND.deserialize(legacy);
        return parsed.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable Component parse(@Nullable String anyFormat) {
        if (anyFormat == null) {
            return null;
        }
        return MINI.deserialize(Legacy.toMini(anyFormat))
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static @NotNull List<Component> parseList(@NotNull List<String> lines) {
        return lines.stream().map(Text::parse).collect(Collectors.toList());
    }

    public static @NotNull String plain(@NotNull Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static @NotNull String escape(@NotNull String value) {
        return MINI.escapeTags(value);
    }

    public static @NotNull String toLegacy(@NotNull Component component) {
        return SECTION.serialize(component);
    }

    public static @NotNull Component of(@NotNull String template, @NotNull Map<String, String> placeholders) {
        return of(substitute(template, placeholders));
    }

    public static @NotNull Component mini(@NotNull String template, @NotNull Map<String, String> placeholders) {
        return mini(substitute(template, placeholders));
    }

    private static @NotNull String substitute(@NotNull String template, @NotNull Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
