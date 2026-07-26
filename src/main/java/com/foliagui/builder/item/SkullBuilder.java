package com.foliagui.builder.item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builder for player-head items: skin by owning player, texture URL, or base64 texture blob. */
public final class SkullBuilder extends BaseItemBuilder<SkullBuilder> {

    private static final Pattern TEXTURE_URL = Pattern.compile("\"url\"\\s*:\\s*\"(http[^\"]+)\"");

    private SkullBuilder(@NotNull ItemStack itemStack) {
        super(itemStack);
    }

    public static @NotNull SkullBuilder create() {
        return new SkullBuilder(new ItemStack(Material.PLAYER_HEAD));
    }

    public @NotNull SkullBuilder owner(@NotNull OfflinePlayer player) {
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(player);
        }
        return this;
    }

    /** Decodes the embedded texture URL from a base64 blob (the string beginning {@code eyJ0ZXh0dXJlcyI6}). */
    public @NotNull SkullBuilder texture(@NotNull String base64) {
        String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        Matcher matcher = TEXTURE_URL.matcher(decoded);
        if (matcher.find()) {
            return textureUrl(matcher.group(1));
        }
        return this;
    }

    public @NotNull SkullBuilder textureUrl(@NotNull String url) {
        if (!(meta instanceof SkullMeta skull)) {
            return this;
        }
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), null);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            skull.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            // leaves the head blank instead of throwing during layout
            java.util.logging.Logger.getLogger(SkullBuilder.class.getName())
                    .warning("Ignoring invalid skull texture URL '" + url + "': " + e.getMessage());
        }
        return this;
    }
}
