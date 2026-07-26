package com.foliagui.builder.item;

import com.foliagui.item.GuiAction;
import com.foliagui.item.GuiItem;
import com.foliagui.util.Text;
import com.google.common.collect.HashMultimap;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public abstract class BaseItemBuilder<B extends BaseItemBuilder<B>> {

    protected final ItemStack itemStack;
    protected final ItemMeta meta;

    protected BaseItemBuilder(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack;
        this.meta = itemStack.getItemMeta();
    }

    public @NotNull B name(@NotNull String name) {
        if (meta != null) {
            meta.displayName(Text.label(name));
        }
        return (B) this;
    }

    public @NotNull B name(@NotNull Component name) {
        if (meta != null) {
            meta.displayName(name);
        }
        return (B) this;
    }

    public @NotNull B nameMini(@NotNull String miniMessage) {
        if (meta != null) {
            meta.displayName(Text.mini(miniMessage));
        }
        return (B) this;
    }

    public @NotNull B loreMini(@NotNull String... lines) {
        if (meta != null) {
            meta.lore(Text.miniList(Arrays.asList(lines)));
        }
        return (B) this;
    }

    public @NotNull B lore(@NotNull String... lines) {
        return lore(Arrays.asList(lines));
    }

    public @NotNull B lore(@NotNull List<String> lines) {
        if (meta != null) {
            meta.lore(lines.stream().map(Text::label).collect(Collectors.toList()));
        }
        return (B) this;
    }

    public @NotNull B loreComponents(@NotNull List<Component> lines) {
        if (meta != null) {
            meta.lore(new ArrayList<>(lines));
        }
        return (B) this;
    }

    public @NotNull B addLore(@NotNull String... lines) {
        if (meta != null) {
            List<Component> current = meta.lore();
            List<Component> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
            for (String line : lines) {
                updated.add(Text.label(line));
            }
            meta.lore(updated);
        }
        return (B) this;
    }

    /** Clamps to a minimum of 1. */
    public @NotNull B amount(int amount) {
        itemStack.setAmount(Math.max(1, amount));
        return (B) this;
    }

    /** Ignores the enchantment's normal level cap. */
    public @NotNull B enchant(@NotNull Enchantment enchantment, int level) {
        return enchant(enchantment, level, true);
    }

    public @NotNull B enchant(@NotNull Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        if (meta != null) {
            meta.addEnchant(enchantment, level, ignoreLevelRestriction);
        }
        return (B) this;
    }

    /** Ignores level restrictions. */
    public @NotNull B enchants(@NotNull Map<Enchantment, Integer> levels) {
        if (meta != null) {
            levels.forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
        }
        return (B) this;
    }

    public @NotNull B removeEnchant(@NotNull Enchantment enchantment) {
        if (meta != null) {
            meta.removeEnchant(enchantment);
        }
        return (B) this;
    }

    public @NotNull B clearEnchants() {
        if (meta != null) {
            meta.removeEnchantments();
        }
        return (B) this;
    }

    public boolean hasEnchant(@NotNull Enchantment enchantment) {
        return meta != null && meta.hasEnchant(enchantment);
    }

    /** Copy; empty if none or meta is unavailable. */
    public @NotNull Map<Enchantment, Integer> getEnchants() {
        return meta != null ? new HashMap<>(meta.getEnchants()) : Map.of();
    }

    public @NotNull B flags(@NotNull ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return (B) this;
    }

    /** Also hides the durability tooltip when set unbreakable. */
    public @NotNull B unbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            if (unbreakable) {
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            } else {
                meta.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }
        }
        return (B) this;
    }

    public @NotNull B customModelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return (B) this;
    }

    /** Configurer receives the component already populated with whatever is currently set. */
    public @NotNull B customModelData(@NotNull Consumer<CustomModelDataComponent> configurer) {
        if (meta != null) {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            configurer.accept(component);
            meta.setCustomModelDataComponent(component);
        }
        return (B) this;
    }

    /** Distinct from the lore-visible display name; not renameable via an anvil. */
    public @NotNull B itemName(@NotNull String legacy) {
        if (meta != null) {
            meta.itemName(Text.label(legacy));
        }
        return (B) this;
    }

    public @NotNull B itemName(@NotNull Component name) {
        if (meta != null) {
            meta.itemName(name);
        }
        return (B) this;
    }

    public @NotNull B itemModel(@NotNull NamespacedKey key) {
        if (meta != null) {
            meta.setItemModel(key);
        }
        return (B) this;
    }

    public @NotNull B tooltipStyle(@NotNull NamespacedKey key) {
        if (meta != null) {
            meta.setTooltipStyle(key);
        }
        return (B) this;
    }

    public @NotNull B rarity(@NotNull ItemRarity rarity) {
        if (meta != null) {
            meta.setRarity(rarity);
        }
        return (B) this;
    }

    /** Overrides anvil/enchanting-table cost, not actual enchant eligibility. */
    public @NotNull B enchantable(int value) {
        if (meta != null) {
            meta.setEnchantable(value);
        }
        return (B) this;
    }

    public @NotNull B glider(boolean glider) {
        if (meta != null) {
            meta.setGlider(glider);
        }
        return (B) this;
    }

    public @NotNull B fireResistant(boolean fireResistant) {
        if (meta != null) {
            meta.setFireResistant(fireResistant);
        }
        return (B) this;
    }

    public @NotNull B damageResistant(@NotNull Tag<DamageType> damageTypes) {
        if (meta != null) {
            meta.setDamageResistant(damageTypes);
        }
        return (B) this;
    }

    /** Item left in the slot once this stack is fully consumed, e.g. an empty bottle. */
    public @NotNull B useRemainder(@NotNull ItemStack remainder) {
        if (meta != null) {
            meta.setUseRemainder(remainder);
        }
        return (B) this;
    }

    public @NotNull B useCooldown(@NotNull Consumer<UseCooldownComponent> configurer) {
        if (meta != null) {
            UseCooldownComponent component = meta.getUseCooldown();
            configurer.accept(component);
            meta.setUseCooldown(component);
        }
        return (B) this;
    }

    public @NotNull B food(@NotNull Consumer<FoodComponent> configurer) {
        if (meta != null) {
            FoodComponent component = meta.getFood();
            configurer.accept(component);
            meta.setFood(component);
        }
        return (B) this;
    }

    public @NotNull B tool(@NotNull Consumer<ToolComponent> configurer) {
        if (meta != null) {
            ToolComponent component = meta.getTool();
            configurer.accept(component);
            meta.setTool(component);
        }
        return (B) this;
    }

    /** Works even on items that aren't normally equippable. */
    public @NotNull B equippable(@NotNull Consumer<EquippableComponent> configurer) {
        if (meta != null) {
            EquippableComponent component = meta.getEquippable();
            configurer.accept(component);
            meta.setEquippable(component);
        }
        return (B) this;
    }

    public @NotNull B jukeboxPlayable(@NotNull Consumer<JukeboxPlayableComponent> configurer) {
        if (meta != null) {
            JukeboxPlayableComponent component = meta.getJukeboxPlayable();
            configurer.accept(component);
            meta.setJukeboxPlayable(component);
        }
        return (B) this;
    }

    public @NotNull B attribute(@NotNull Attribute attribute, @NotNull AttributeModifier modifier) {
        if (meta != null) {
            meta.addAttributeModifier(attribute, modifier);
        }
        return (B) this;
    }

    public @NotNull B removeAttribute(@NotNull Attribute attribute) {
        if (meta != null) {
            meta.removeAttributeModifier(attribute);
        }
        return (B) this;
    }

    public @NotNull B removeAttribute(@NotNull Attribute attribute, @NotNull AttributeModifier modifier) {
        if (meta != null) {
            meta.removeAttributeModifier(attribute, modifier);
        }
        return (B) this;
    }

    public @NotNull B clearAttributes() {
        if (meta != null) {
            meta.setAttributeModifiers(HashMultimap.create());
        }
        return (B) this;
    }

    public @NotNull B canDestroy(@NotNull Material... materials) {
        if (meta != null) {
            meta.setCanDestroy(Set.of(materials));
        }
        return (B) this;
    }

    public @NotNull B canPlaceOn(@NotNull Material... materials) {
        if (meta != null) {
            meta.setCanPlaceOn(Set.of(materials));
        }
        return (B) this;
    }

    /** No-op if this item doesn't track a repair cost. */
    public @NotNull B repairCost(int levels) {
        if (meta instanceof Repairable repairable) {
            repairable.setRepairCost(levels);
        }
        return (B) this;
    }

    /** Glint without a real enchantment; no fake enchant appears in the tooltip. */
    public @NotNull B glow(boolean glow) {
        if (meta != null) {
            meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null);
        }
        return (B) this;
    }

    public @NotNull B hideTooltip(boolean hidden) {
        if (meta != null) {
            meta.setHideTooltip(hidden);
        }
        return (B) this;
    }

    /** Valid range is 1-99. Pass {@code null} to restore the material default. */
    public @NotNull B maxStackSize(@Nullable Integer max) {
        if (meta != null) {
            meta.setMaxStackSize(max);
        }
        return (B) this;
    }

    /** No-op if this item isn't damageable. */
    public @NotNull B damage(int damage) {
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(Math.max(0, damage));
        }
        return (B) this;
    }

    /** No-op if not damageable. Pass {@code null} to restore the material default. */
    public @NotNull B maxDamage(@Nullable Integer maxDamage) {
        if (meta instanceof Damageable damageable) {
            damageable.setMaxDamage(maxDamage);
        }
        return (B) this;
    }

    public @NotNull B hideExtras() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return (B) this;
    }

    public <T, Z> @NotNull B setData(@NotNull NamespacedKey key,
                                     @NotNull PersistentDataType<T, Z> type, @NotNull Z value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, type, value);
        }
        return (B) this;
    }

    public @NotNull B removeData(@NotNull NamespacedKey key) {
        if (meta != null) {
            meta.getPersistentDataContainer().remove(key);
        }
        return (B) this;
    }

    public <T, Z> @Nullable Z getData(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
        return meta != null ? meta.getPersistentDataContainer().get(key, type) : null;
    }

    public @NotNull B persistentData(@NotNull Consumer<PersistentDataContainer> consumer) {
        if (meta != null) {
            consumer.accept(meta.getPersistentDataContainer());
        }
        return (B) this;
    }

    public @NotNull B editMeta(@NotNull Consumer<ItemMeta> consumer) {
        if (meta != null) {
            consumer.accept(meta);
        }
        return (B) this;
    }

    /** Returns a fresh copy each call; the builder stays safe to keep configuring afterwards. */
    public @NotNull ItemStack build() {
        if (meta != null) {
            itemStack.setItemMeta(meta);
        }
        return itemStack.clone();
    }

    public @NotNull GuiItem asGuiItem() {
        return GuiItem.trusted(build(), null);
    }

    public @NotNull GuiItem asGuiItem(@NotNull GuiAction<InventoryClickEvent> action) {
        return GuiItem.trusted(build(), action);
    }
}
