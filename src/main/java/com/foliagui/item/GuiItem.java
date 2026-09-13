package com.foliagui.item;

import com.foliagui.FoliaGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/** Stamps a stable {@link UUID} into its persistent data so a clicked {@link ItemStack} resolves back to it. */
public final class GuiItem {

    private volatile UUID uuid;
    private ItemStack itemStack;
    private GuiAction<InventoryClickEvent> action;
    private org.bukkit.Sound clickSound;
    private float clickVolume = 1.0f;
    private float clickPitch = 1.0f;
    private volatile long cooldownMillis;
    private final java.util.concurrent.atomic.AtomicLong lastClickMillis = new java.util.concurrent.atomic.AtomicLong();
    private volatile boolean editable;
    private GuiAction<InventoryClickEvent> leftClickAction;
    private GuiAction<InventoryClickEvent> rightClickAction;
    private GuiAction<InventoryClickEvent> shiftClickAction;
    private GuiAction<InventoryClickEvent> numberKeyAction;
    private GuiAction<InventoryClickEvent> cooldownBlockedAction;
    private String requiredPermission;
    private Consumer<Player> permissionDeniedHandler = player -> {
    };

    /** Clones {@code itemStack} so external mutation can't corrupt the GUI. */
    public GuiItem(@NotNull ItemStack itemStack, @Nullable GuiAction<InventoryClickEvent> action) {
        this.action = action;
        this.itemStack = itemStack.clone();
    }

    public GuiItem(@NotNull ItemStack itemStack) {
        this(itemStack, null);
    }

    private GuiItem(@NotNull ItemStack itemStack, @Nullable GuiAction<InventoryClickEvent> action, boolean skipClone) {
        this.action = action;
        this.itemStack = skipClone ? itemStack : itemStack.clone();
    }

    /** Skips the defensive clone; only for a stack the caller guarantees isn't referenced elsewhere. */
    public static @NotNull GuiItem trusted(@NotNull ItemStack isolatedStack, @Nullable GuiAction<InventoryClickEvent> action) {
        return new GuiItem(isolatedStack, action, true);
    }

    public GuiItem(@NotNull Material material) {
        this(new ItemStack(material), null);
    }

    private ItemStack stamp(@NotNull ItemStack stack, @NotNull UUID identity) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(FoliaGUI.itemKey(), PersistentDataType.STRING, identity.toString());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public @NotNull UUID getUuid() {
        UUID identity = uuid;
        if (identity != null) {
            return identity;
        }
        synchronized (this) {
            if (uuid == null) {
                uuid = UUID.randomUUID();
                itemStack = stamp(itemStack, uuid);
            }
            return uuid;
        }
    }

    /** Live reference; prefer {@link #setItemStack(ItemStack)} to replace it. */
    public @NotNull ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(@NotNull ItemStack itemStack) {
        ItemStack replacement = itemStack.clone();
        UUID identity = uuid;
        this.itemStack = identity == null ? replacement : stamp(replacement, identity);
    }

    public @NotNull GuiItem withItemStack(@NotNull ItemStack itemStack) {
        GuiItem copy = new GuiItem(itemStack, action);
        copy.clickSound = clickSound;
        copy.clickVolume = clickVolume;
        copy.clickPitch = clickPitch;
        copy.cooldownMillis = cooldownMillis;
        copy.lastClickMillis.set(lastClickMillis.get());
        copy.editable = editable;
        copy.leftClickAction = leftClickAction;
        copy.rightClickAction = rightClickAction;
        copy.shiftClickAction = shiftClickAction;
        copy.numberKeyAction = numberKeyAction;
        copy.cooldownBlockedAction = cooldownBlockedAction;
        copy.requiredPermission = requiredPermission;
        copy.permissionDeniedHandler = permissionDeniedHandler;
        return copy;
    }

    /** If any per-click-type handler or {@link #requirePermission} is set, returns a dispatcher combining them. */
    public @Nullable GuiAction<InventoryClickEvent> getAction() {
        if (requiredPermission == null && leftClickAction == null && rightClickAction == null
                && shiftClickAction == null && numberKeyAction == null) {
            return action;
        }
        return event -> {
            if (requiredPermission != null && event.getWhoClicked() instanceof Player player
                    && !player.hasPermission(requiredPermission)) {
                permissionDeniedHandler.accept(player);
                return;
            }
            ClickType click = event.getClick();
            if (click.isShiftClick() && shiftClickAction != null) {
                shiftClickAction.execute(event);
            } else if (click == ClickType.RIGHT && rightClickAction != null) {
                rightClickAction.execute(event);
            } else if (click == ClickType.LEFT && leftClickAction != null) {
                leftClickAction.execute(event);
            } else if (click == ClickType.NUMBER_KEY && numberKeyAction != null) {
                numberKeyAction.execute(event);
            }
            if (action != null) {
                action.execute(event);
            }
        };
    }

    public void setAction(@Nullable GuiAction<InventoryClickEvent> action) {
        this.action = action;
    }

    public @NotNull GuiItem onLeftClick(@NotNull GuiAction<InventoryClickEvent> action) {
        this.leftClickAction = action;
        return this;
    }

    public @NotNull GuiItem onRightClick(@NotNull GuiAction<InventoryClickEvent> action) {
        this.rightClickAction = action;
        return this;
    }

    public @NotNull GuiItem onShiftClick(@NotNull GuiAction<InventoryClickEvent> action) {
        this.shiftClickAction = action;
        return this;
    }

    /** Fires when the player presses a hotbar number key while hovering this item, swapping it into that slot. */
    public @NotNull GuiItem onNumberKey(@NotNull GuiAction<InventoryClickEvent> action) {
        this.numberKeyAction = action;
        return this;
    }

    /** Fires instead of the normal action(s) when a click lands inside {@link #cooldown(long)}'s window. */
    public @NotNull GuiItem onCooldownBlocked(@NotNull GuiAction<InventoryClickEvent> action) {
        this.cooldownBlockedAction = action;
        return this;
    }

    @ApiStatus.Internal
    public @Nullable GuiAction<InventoryClickEvent> getCooldownBlockedAction() {
        return cooldownBlockedAction;
    }

    /** Clicks from a player lacking {@code permission} run {@code onDenied} instead of this item's action(s). */
    public @NotNull GuiItem requirePermission(@NotNull String permission, @NotNull Consumer<Player> onDenied) {
        this.requiredPermission = permission;
        this.permissionDeniedHandler = onDenied;
        return this;
    }

    public @NotNull GuiItem clickSound(@NotNull org.bukkit.Sound sound) {
        return clickSound(sound, 1.0f, 1.0f);
    }

    public @NotNull GuiItem clickSound(@NotNull org.bukkit.Sound sound, float volume, float pitch) {
        this.clickSound = sound;
        this.clickVolume = volume;
        this.clickPitch = pitch;
        return this;
    }

    public @Nullable org.bukkit.Sound getClickSound() {
        return clickSound;
    }

    public float getClickVolume() {
        return clickVolume;
    }

    public float getClickPitch() {
        return clickPitch;
    }

    /** Minimum delay between successful clicks. Clicks inside the window are silently dropped, sound included. {@code 0} disables it. */
    public @NotNull GuiItem cooldown(long ticks) {
        this.cooldownMillis = Math.max(0, ticks) * 50L;
        return this;
    }

    /** In ticks, 0 if disabled. */
    public long getCooldownTicks() {
        return cooldownMillis / 50L;
    }

    @ApiStatus.Internal
    public boolean tryClick() {
        if (cooldownMillis <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        long last = lastClickMillis.get();
        if (now - last < cooldownMillis) {
            return false;
        }
        lastClickMillis.set(now);
        return true;
    }

    /** Opts out of the automatic take/swap/drop/drag protection every GuiItem otherwise gets. Defaults to {@code false}. */
    public @NotNull GuiItem editable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public boolean isEditable() {
        return editable;
    }

    /** Null if {@code stack} isn't a stamped GUI item. */
    public static @Nullable UUID uuidOf(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(FoliaGUI.itemKey(), PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        Component name = itemStack.getItemMeta() != null ? itemStack.getItemMeta().displayName() : null;
        return "GuiItem{uuid=" + getUuid() + ", type=" + itemStack.getType() + ", name=" + name + '}';
    }
}
