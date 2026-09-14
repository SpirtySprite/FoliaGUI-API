package com.foliagui.builder.item;

import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

public final class FireworkBuilder extends BaseItemBuilder<FireworkBuilder> {

    private FireworkBuilder(@NotNull ItemStack itemStack) {
        super(itemStack);
    }

    public static @NotNull FireworkBuilder rocket() {
        return new FireworkBuilder(new ItemStack(Material.FIREWORK_ROCKET));
    }

    public static @NotNull FireworkBuilder star() {
        return new FireworkBuilder(new ItemStack(Material.FIREWORK_STAR));
    }

    public @NotNull FireworkBuilder power(int power) {
        if (meta instanceof FireworkMeta firework) {
            firework.setPower(Math.max(0, power));
        }
        return this;
    }

    public @NotNull FireworkBuilder effect(@NotNull FireworkEffect effect) {
        if (meta instanceof FireworkMeta firework) {
            firework.addEffect(effect);
        } else if (meta instanceof FireworkEffectMeta star) {
            star.setEffect(effect);
        }
        return this;
    }
}
