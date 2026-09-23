package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.builder.item.ItemBuilder;
import com.foliagui.item.GuiItem;
import com.foliagui.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public final class QuantityGui {

    private static final int[] STEPS = {-64, -10, -1, 1, 10, 64};
    private static final int[] STEP_COLUMNS = {1, 2, 3, 7, 8, 9};

    private QuantityGui() {
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "&8Choose an amount";
        private ItemStack display = new ItemStack(Material.CHEST);
        private int min = 1;
        private int max = 64;
        private int initial = 1;
        private IntFunction<List<String>> description = amount -> List.of("&7Amount: &f" + amount);
        private Consumer<Integer> onConfirm = amount -> {
        };
        private Runnable onCancel = () -> {
        };

        public @NotNull Builder title(@NotNull String title) {
            this.title = title;
            return this;
        }

        public @NotNull Builder display(@NotNull ItemStack display) {
            this.display = display.clone();
            return this;
        }

        public @NotNull Builder range(int min, int max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
            return this;
        }

        public @NotNull Builder initial(int initial) {
            this.initial = initial;
            return this;
        }

        public @NotNull Builder description(@NotNull IntFunction<List<String>> description) {
            this.description = description;
            return this;
        }

        public @NotNull Builder onConfirm(@NotNull Consumer<Integer> onConfirm) {
            this.onConfirm = onConfirm;
            return this;
        }

        public @NotNull Builder onCancel(@NotNull Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        public @NotNull Gui build() {
            Gui gui = Gui.of(3, title);
            int[] amount = {clamp(initial)};
            AtomicBoolean decided = new AtomicBoolean();
            GuiTheme theme = FoliaGUI.theme();
            gui.filler().fill(theme.filler());
            Runnable[] render = new Runnable[1];
            render[0] = () -> {
                for (int index = 0; index < STEPS.length; index++) {
                    int step = STEPS[index];
                    gui.updateItem(com.foliagui.util.Slot.of(2, STEP_COLUMNS[index]), stepButton(step, amount, render[0]));
                }
                gui.updateItem(com.foliagui.util.Slot.of(2, 5), preview(amount[0]));
            };
            for (int index = 0; index < STEPS.length; index++) {
                gui.setItem(2, STEP_COLUMNS[index], stepButton(STEPS[index], amount, render[0]));
            }
            gui.setItem(2, 5, preview(amount[0]));
            gui.setItem(3, 4, ItemBuilder.of(Material.LIME_CONCRETE).name("&aConfirm").asGuiItem(event -> {
                if (!decided.compareAndSet(false, true)) {
                    return;
                }
                theme.success(event.getWhoClicked());
                gui.close(event.getWhoClicked());
                onConfirm.accept(amount[0]);
            }));
            gui.setItem(3, 6, ItemBuilder.of(Material.RED_CONCRETE).name("&cCancel").asGuiItem(event -> {
                if (!decided.compareAndSet(false, true)) {
                    return;
                }
                theme.click(event.getWhoClicked());
                gui.close(event.getWhoClicked());
                onCancel.run();
            }));
            return gui;
        }

        public void open(@NotNull Player player) {
            build().open(player);
        }

        private GuiItem stepButton(int step, int[] amount, Runnable render) {
            int next = clamp(amount[0] + step);
            boolean possible = next != amount[0];
            Material material = step < 0
                    ? possible ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE
                    : possible ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            String label = (step < 0 ? "&c" : "&a+") + step;
            return ItemBuilder.of(material, Math.min(64, Math.abs(step))).name(label).asGuiItem(event -> {
                int target = clamp(amount[0] + step);
                if (target == amount[0]) {
                    FoliaGUI.theme().deny(event.getWhoClicked());
                    return;
                }
                amount[0] = target;
                FoliaGUI.theme().click(event.getWhoClicked());
                render.run();
            });
        }

        private GuiItem preview(int amount) {
            ItemStack stack = display.clone();
            stack.setAmount(Math.max(1, Math.min(stack.getMaxStackSize(), amount)));
            return ItemBuilder.of(stack).loreComponents(Text.parseList(description.apply(amount))).asGuiItem();
        }

        int clamp(int value) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
