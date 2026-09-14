package com.foliagui.builder.item;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

public final class PotionBuilder extends BaseItemBuilder<PotionBuilder> {

    private PotionBuilder(@NotNull ItemStack itemStack) {
        super(itemStack);
    }

    public static @NotNull PotionBuilder of(@NotNull Material material) {
        return new PotionBuilder(new ItemStack(material));
    }

    public @NotNull PotionBuilder base(@NotNull PotionType type) {
        if (meta instanceof PotionMeta potion) {
            potion.setBasePotionType(type);
        }
        return this;
    }

    public @NotNull PotionBuilder effect(@NotNull PotionEffect effect) {
        if (meta instanceof PotionMeta potion) {
            potion.addCustomEffect(effect, true);
        }
        return this;
    }

    public @NotNull PotionBuilder effect(@NotNull PotionEffectType type, int durationTicks, int amplifier) {
        return effect(new PotionEffect(type, durationTicks, amplifier));
    }

    public @NotNull PotionBuilder color(@NotNull Color color) {
        if (meta instanceof PotionMeta potion) {
            potion.setColor(color);
        }
        return this;
    }
}
