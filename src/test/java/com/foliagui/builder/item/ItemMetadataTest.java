package com.foliagui.builder.item;

import com.foliagui.FoliaGUI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemMetadataTest {

    @BeforeAll
    static void setUp() {
        MockBukkit.mock();
        FoliaGUI.init(MockBukkit.createMockPlugin("FoliaGUITest"));
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void bulkEnchantsAndQueries() {
        ItemStack sword = ItemBuilder.of(Material.DIAMOND_SWORD)
                .enchants(Map.of(Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3))
                .build();

        ItemBuilder reader = ItemBuilder.of(sword);
        assertTrue(reader.hasEnchant(Enchantment.SHARPNESS));
        assertEquals(5, reader.getEnchants().get(Enchantment.SHARPNESS));
        assertEquals(3, reader.getEnchants().get(Enchantment.UNBREAKING));
    }

    @Test
    void clearEnchantsRemovesEverything() {
        ItemBuilder builder = ItemBuilder.of(Material.DIAMOND_SWORD).enchant(Enchantment.SHARPNESS, 5);
        assertTrue(builder.hasEnchant(Enchantment.SHARPNESS));
        builder.clearEnchants();
        assertFalse(builder.hasEnchant(Enchantment.SHARPNESS));
    }

    @Test
    void persistentDataRoundTrips() {
        NamespacedKey key = new NamespacedKey(FoliaGUI.plugin(), "test-key");
        ItemBuilder builder = ItemBuilder.of(Material.STONE)
                .setData(key, PersistentDataType.STRING, "hello");

        assertEquals("hello", builder.getData(key, PersistentDataType.STRING));

        builder.removeData(key);
        assertNull(builder.getData(key, PersistentDataType.STRING));
    }

    @Test
    void persistentDataEscapeHatchSeesTheSameContainer() {
        NamespacedKey key = new NamespacedKey(FoliaGUI.plugin(), "escape-hatch");
        ItemBuilder builder = ItemBuilder.of(Material.STONE);
        builder.persistentData(container -> container.set(key, PersistentDataType.INTEGER, 42));

        assertEquals(42, builder.getData(key, PersistentDataType.INTEGER));
    }

    @Test
    void rarityAndEnchantableApply() {
        ItemStack stack = ItemBuilder.of(Material.STONE)
                .rarity(ItemRarity.EPIC)
                .enchantable(10)
                .build();

        assertEquals(ItemRarity.EPIC, stack.getItemMeta().getRarity());
        assertEquals(10, stack.getItemMeta().getEnchantable());
    }

    @Test
    void gliderAndFireResistantFlagsApply() {
        ItemBuilder builder = ItemBuilder.of(Material.STONE)
                .glider(true)
                .fireResistant(true);

        assertTrue(builder.meta.isGlider());
        assertTrue(builder.meta.isFireResistant());
    }

    @Test
    void foodComponentConfiguresNutrition() {
        ItemBuilder builder = ItemBuilder.of(Material.COOKED_BEEF)
                .food(food -> {
                    food.setNutrition(20);
                    food.setSaturation(5.0f);
                    food.setCanAlwaysEat(true);
                });

        var food = builder.meta.getFood();
        assertEquals(20, food.getNutrition());
        assertEquals(5.0f, food.getSaturation());
        assertTrue(food.canAlwaysEat());
    }

    @Test
    void attributeModifiersAddAndClear() {
        AttributeModifier modifier = new AttributeModifier(
                new NamespacedKey(FoliaGUI.plugin(), "bonus-damage"), 2.0, AttributeModifier.Operation.ADD_NUMBER);

        ItemBuilder builder = ItemBuilder.of(Material.DIAMOND_SWORD)
                .attribute(Attribute.ATTACK_DAMAGE, modifier);
        ItemStack withAttribute = builder.build();
        assertTrue(withAttribute.getItemMeta().hasAttributeModifiers());

        builder.clearAttributes();
        assertFalse(builder.build().getItemMeta().hasAttributeModifiers());
    }

    @Test
    void canDestroyAndCanPlaceOnRestrictItem() {
        ItemStack stack = ItemBuilder.of(Material.DIAMOND_PICKAXE)
                .canDestroy(Material.STONE, Material.DIRT)
                .canPlaceOn(Material.GRASS_BLOCK)
                .build();

        assertEquals(Set.of(Material.STONE, Material.DIRT), stack.getItemMeta().getCanDestroy());
        assertEquals(Set.of(Material.GRASS_BLOCK), stack.getItemMeta().getCanPlaceOn());
    }

    @Test
    void repairCostAppliesToRepairableItems() {
        ItemStack stack = ItemBuilder.of(Material.DIAMOND_SWORD).repairCost(5).build();
        assertEquals(5, ((Repairable) stack.getItemMeta()).getRepairCost());
    }
}
