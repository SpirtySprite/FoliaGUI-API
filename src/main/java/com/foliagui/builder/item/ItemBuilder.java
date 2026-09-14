package com.foliagui.builder.item;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

public final class ItemBuilder extends BaseItemBuilder<ItemBuilder> {

    private ItemBuilder(@NotNull ItemStack itemStack) {
        super(itemStack);
    }

    public static @NotNull ItemBuilder of(@NotNull Material material) {
        requireNotAir(material);
        return new ItemBuilder(new ItemStack(material));
    }

    public static @NotNull ItemBuilder of(@NotNull Material material, int amount) {
        requireNotAir(material);
        return new ItemBuilder(new ItemStack(material, Math.max(1, amount)));
    }

    public static @NotNull ItemBuilder of(@NotNull ItemStack itemStack) {
        requireNotAir(itemStack.getType());
        return new ItemBuilder(itemStack.clone());
    }

    private static void requireNotAir(@NotNull Material material) {
        if (material.isAir()) {
            throw new IllegalArgumentException(
                    "Cannot build an item from " + material + ": air has no ItemMeta, so every " +
                    "name/lore/enchant/etc. call would silently do nothing. Pick a real material instead.");
        }
    }

    public static @NotNull SkullBuilder skull() {
        return SkullBuilder.create();
    }

    public static @NotNull PotionBuilder potion(@NotNull Material material) {
        return PotionBuilder.of(material);
    }

    public static @NotNull BannerBuilder banner(@NotNull Material material) {
        return BannerBuilder.of(material);
    }

    public static @NotNull FireworkBuilder firework() {
        return FireworkBuilder.rocket();
    }

    public static @NotNull FireworkBuilder fireworkStar() {
        return FireworkBuilder.star();
    }

    public static @NotNull BookBuilder book() {
        return BookBuilder.create();
    }

    public @NotNull ItemBuilder leatherColor(@NotNull Color color) {
        if (meta instanceof LeatherArmorMeta leather) {
            leather.setColor(color);
        }
        return this;
    }

    public @NotNull ItemBuilder potionColor(@NotNull Color color) {
        if (meta instanceof PotionMeta potion) {
            potion.setColor(color);
        }
        return this;
    }

    public @NotNull ItemBuilder trim(@NotNull TrimMaterial material, @NotNull TrimPattern pattern) {
        if (meta instanceof ArmorMeta armor) {
            armor.setTrim(new ArmorTrim(material, pattern));
        }
        return this;
    }
}
