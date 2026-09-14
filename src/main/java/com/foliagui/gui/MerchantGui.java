package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MerchantGui {

    private static final int RESULT_SLOT = 2;

    private static final SessionRegistry<MerchantGui> SESSIONS = new SessionRegistry<>();

    private final Component title;
    private final List<MerchantRecipe> recipes;
    private final BiConsumer<Player, MerchantRecipe> onTrade;
    private final Consumer<Player> onClose;

    private MerchantGui(Builder builder) {
        this.title = builder.title;
        this.recipes = builder.recipes;
        this.onTrade = builder.onTrade;
        this.onClose = builder.onClose;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static boolean hasSession(@NotNull HumanEntity player) {
        return SESSIONS.has(player);
    }

    public void open(@NotNull Player player) {
        FoliaGUI.scheduler().runForEntity(player, () -> {
            Merchant merchant = Bukkit.createMerchant(title);
            merchant.setRecipes(recipes);
            player.openMerchant(merchant, true);
            SESSIONS.put(player, this);
        }, null);
    }

    @ApiStatus.Internal
    public static boolean handleClick(@NotNull InventoryClickEvent event) {
        MerchantGui gui = SESSIONS.get(event.getWhoClicked());
        if (gui == null || !(event.getInventory() instanceof MerchantInventory merchantInventory)) {
            return false;
        }
        if (gui.onTrade != null && event.getSlot() == RESULT_SLOT
                && merchantInventory.equals(event.getClickedInventory())) {
            MerchantRecipe recipe = merchantInventory.getSelectedRecipe();
            if (recipe != null) {
                Player player = (Player) event.getWhoClicked();
                FoliaGUI.scheduler().runForEntity(player, () -> gui.onTrade.accept(player, recipe), null);
            }
        }
        return true;
    }

    public static void clearSessions() {
        SESSIONS.clear();
    }

    @ApiStatus.Internal
    public static boolean handleClose(@NotNull InventoryCloseEvent event) {
        MerchantGui gui = SESSIONS.remove(event.getPlayer());
        if (gui == null) {
            return false;
        }
        if (gui.onClose != null) {
            gui.onClose.accept((Player) event.getPlayer());
        }
        return true;
    }

    public static final class Builder {
        private Component title = Component.empty();
        private final List<MerchantRecipe> recipes = new ArrayList<>();
        private BiConsumer<Player, MerchantRecipe> onTrade;
        private Consumer<Player> onClose;

        public @NotNull Builder title(@NotNull String title) {
            this.title = Text.of(title);
            return this;
        }

        public @NotNull Builder title(@NotNull Component title) {
            this.title = title;
            return this;
        }

        public @NotNull Builder addRecipe(@NotNull MerchantRecipe recipe) {
            recipes.add(recipe);
            return this;
        }

        public @NotNull Builder addRecipe(@NotNull ItemStack result, @NotNull List<ItemStack> ingredients) {
            MerchantRecipe recipe = new MerchantRecipe(result, Integer.MAX_VALUE);
            recipe.setIngredients(ingredients);
            recipes.add(recipe);
            return this;
        }

        public @NotNull Builder onTrade(@NotNull BiConsumer<Player, MerchantRecipe> onTrade) {
            this.onTrade = onTrade;
            return this;
        }

        public @NotNull Builder onClose(@NotNull Consumer<Player> onClose) {
            this.onClose = onClose;
            return this;
        }

        public @NotNull MerchantGui build() {
            if (recipes.isEmpty()) {
                throw new IllegalStateException("MerchantGui needs at least one recipe; call addRecipe(...) first");
            }
            return new MerchantGui(this);
        }

        public void open(@NotNull Player player) {
            build().open(player);
        }
    }
}
