package com.foliagui.builder.item;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;

public final class BannerBuilder extends BaseItemBuilder<BannerBuilder> {

    private BannerBuilder(@NotNull ItemStack itemStack) {
        super(itemStack);
    }

    public static @NotNull BannerBuilder of(@NotNull Material material) {
        return new BannerBuilder(new ItemStack(material));
    }

    public @NotNull BannerBuilder pattern(@NotNull Pattern pattern) {
        if (meta instanceof BannerMeta banner) {
            banner.addPattern(pattern);
        }
        return this;
    }

    public @NotNull BannerBuilder pattern(@NotNull DyeColor color, @NotNull PatternType type) {
        return pattern(new Pattern(color, type));
    }
}
