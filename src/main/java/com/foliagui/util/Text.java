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

/**
 * {@link #of(String)} accepts {@code &}-style and section-style colour codes; {@link #mini(String)} parses
 * MiniMessage. All helpers turn off the default italic Minecraft applies to custom item names and lore.
 */
public final class Text {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Text() {
    }

    /** Parses a MiniMessage string (e.g. {@code "<gradient:#f00:#00f>Title</gradient>"}), italic disabled. */
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

    /** Converts a legacy colour-coded ({@code &} or {@code §}) string into a component. */
    @Contract("null -> null; !null -> !null")
    public static @Nullable Component of(@Nullable String legacy) {
        if (legacy == null) {
            return null;
        }
        Component parsed = legacy.indexOf('§') >= 0 ? SECTION.deserialize(legacy) : AMPERSAND.deserialize(legacy);
        return parsed.decoration(TextDecoration.ITALIC, parsed.hasDecoration(TextDecoration.ITALIC));
    }

    /** Like {@link #of(String)} but always clears italic, for short display names. */
    @Contract("null -> null; !null -> !null")
    public static @Nullable Component label(@Nullable String legacy) {
        if (legacy == null) {
            return null;
        }
        Component parsed = legacy.indexOf('§') >= 0 ? SECTION.deserialize(legacy) : AMPERSAND.deserialize(legacy);
        return parsed.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static @NotNull String toLegacy(@NotNull Component component) {
        return SECTION.serialize(component);
    }

    /** Substitutes {@code {key}} placeholders in {@code template} before parsing via {@link #of(String)}. */
    public static @NotNull Component of(@NotNull String template, @NotNull Map<String, String> placeholders) {
        return of(substitute(template, placeholders));
    }

    /** Like {@link #of(String, Map)} but parses the substituted result as MiniMessage via {@link #mini(String)}. */
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
