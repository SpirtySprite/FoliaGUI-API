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

public final class GuiItem {

    private volatile UUID uuid;
    private ItemStack itemStack;
    private GuiAction<InventoryClickEvent> action;
    private org.bukkit.Sound clickSound;
    private float clickVolume = 1.0f;
    private float clickPitch = 1.0f;
    private volatile long cooldownMillis;
    private static final UUID SHARED_CLICKER = new UUID(0L, 0L);
    private static final int COOLDOWN_PRUNE_THRESHOLD = 256;
    private final java.util.Map<UUID, Long> lastClicks = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean editable;
    private GuiAction<InventoryClickEvent> leftClickAction;
    private GuiAction<InventoryClickEvent> rightClickAction;
    private GuiAction<InventoryClickEvent> shiftClickAction;
    private GuiAction<InventoryClickEvent> numberKeyAction;
    private GuiAction<InventoryClickEvent> cooldownBlockedAction;
    private String requiredPermission;
    private Consumer<Player> permissionDeniedHandler = player -> {
    };

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
        copy.lastClicks.putAll(lastClicks);
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

    public @NotNull GuiItem onNumberKey(@NotNull GuiAction<InventoryClickEvent> action) {
        this.numberKeyAction = action;
        return this;
    }

    public @NotNull GuiItem onCooldownBlocked(@NotNull GuiAction<InventoryClickEvent> action) {
        this.cooldownBlockedAction = action;
        return this;
    }

    @ApiStatus.Internal
    public @Nullable GuiAction<InventoryClickEvent> getCooldownBlockedAction() {
        return cooldownBlockedAction;
    }

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

    public @NotNull GuiItem cooldown(long ticks) {
        this.cooldownMillis = Math.max(0, ticks) * 50L;
        return this;
    }

    public long getCooldownTicks() {
        return cooldownMillis / 50L;
    }

    @ApiStatus.Internal
    public boolean tryClick() {
        return tryClick(SHARED_CLICKER);
    }

    @ApiStatus.Internal
    public boolean tryClick(@NotNull UUID clicker) {
        long cooldown = cooldownMillis;
        if (cooldown <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        boolean[] allowed = {false};
        lastClicks.compute(clicker, (key, last) -> {
            if (last != null && now - last < cooldown) {
                return last;
            }
            allowed[0] = true;
            return now;
        });
        if (lastClicks.size() > COOLDOWN_PRUNE_THRESHOLD) {
            lastClicks.values().removeIf(last -> now - last >= cooldown);
        }
        return allowed[0];
    }

    public long remainingCooldownMillis(@NotNull UUID clicker) {
        Long last = lastClicks.get(clicker);
        if (last == null || cooldownMillis <= 0) {
            return 0L;
        }
        return Math.max(0L, cooldownMillis - (System.currentTimeMillis() - last));
    }

    public @NotNull GuiItem editable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public boolean isEditable() {
        return editable;
    }

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
