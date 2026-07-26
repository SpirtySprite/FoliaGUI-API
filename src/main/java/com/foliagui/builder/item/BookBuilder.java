package com.foliagui.builder.item;

import com.foliagui.util.Text;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

public final class BookBuilder extends BaseItemBuilder<BookBuilder> {

    private BookBuilder(@NotNull ItemStack itemStack) {
        super(itemStack);
    }

    public static @NotNull BookBuilder create() {
        return new BookBuilder(new ItemStack(Material.WRITTEN_BOOK));
    }

    public @NotNull BookBuilder bookTitle(@NotNull String miniMessage) {
        if (meta instanceof BookMeta book) {
            book.title(Text.mini(miniMessage));
        }
        return this;
    }

    public @NotNull BookBuilder author(@NotNull String miniMessage) {
        if (meta instanceof BookMeta book) {
            book.author(Text.mini(miniMessage));
        }
        return this;
    }

    public @NotNull BookBuilder page(@NotNull String... pages) {
        if (meta instanceof BookMeta book) {
            for (String page : pages) {
                book.addPages(Text.mini(page));
            }
        }
        return this;
    }
}
