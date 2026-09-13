package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import com.foliagui.item.GuiAction;
import com.foliagui.item.GuiItem;
import com.foliagui.util.Slot;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for every GUI. open/close/update route through the Folia scheduler and work from any thread;
 * item mutators just touch the map and need update()/open() to apply. One viewer per instance.
 */
public abstract class BaseGui implements InventoryHolder {

    private Component title;
    private final int size;
    private final GuiType guiType; // null => chest sized by rows
    private final int rows;        // only meaningful for chest GUIs

    private volatile Inventory inventory;
    private final Map<Integer, GuiItem> guiItems = new ConcurrentHashMap<>();
    private final GuiItem[] renderedItems;
    private final ItemStack[] renderedStacks;
    private final Map<Integer, GuiAction<InventoryClickEvent>> slotActions = new ConcurrentHashMap<>();
    private final Set<InteractionModifier> interactionModifiers =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private GuiAction<InventoryClickEvent> defaultClickAction;
    private GuiAction<InventoryClickEvent> defaultTopClickAction;
    private GuiAction<InventoryClickEvent> playerInventoryAction;
    private GuiAction<InventoryClickEvent> outsideClickAction;
    private GuiAction<InventoryDragEvent> dragAction;
    private GuiAction<InventoryOpenEvent> openAction;
    private GuiAction<InventoryCloseEvent> closeAction;

    private volatile boolean updating;

    private volatile long updateIntervalTicks;
    private final Map<UUID, com.foliagui.scheduler.TaskHandle> updateTasks = new ConcurrentHashMap<>();

    private volatile boolean forceOpen;
    private final Set<UUID> allowedCloses = ConcurrentHashMap.newKeySet();

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(BaseGui.class.getName());

    {
        interactionModifiers.addAll(EnumSet.allOf(InteractionModifier.class));
    }

    protected BaseGui(int rows, @NotNull Component title) {
        this.rows = Math.max(1, Math.min(6, rows));
        this.guiType = null;
        this.size = this.rows * Slot.ROW_WIDTH;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, title);
        this.renderedItems = new GuiItem[size];
        this.renderedStacks = new ItemStack[size];
    }

    protected BaseGui(@NotNull GuiType guiType, @NotNull Component title) {
        this.guiType = guiType;
        this.rows = 0;
        this.size = guiType.getSize();
        this.title = title;
        this.inventory = Bukkit.createInventory(this, guiType.getInventoryType(), title);
        this.renderedItems = new GuiItem[size];
        this.renderedStacks = new ItemStack[size];
    }

    protected void populateInventory() {
        Inventory target = getInventory();
        int slotCount = target.getSize();
        for (int slot = 0; slot < slotCount; slot++) {
            applyItem(slot, guiItems.get(slot));
        }
    }

    protected final void applyItem(int slot, @Nullable GuiItem item) {
        ItemStack stack = item == null ? null : item.getItemStack();
        if (renderedItems[slot] == item && renderedStacks[slot] == stack) {
            return;
        }
        getInventory().setItem(slot, stack == null ? null : stack.clone());
        renderedItems[slot] = item;
        renderedStacks[slot] = stack;
    }

    /** 0-indexed flat slot. */
    public @NotNull BaseGui setItem(int slot, @NotNull GuiItem guiItem) {
        Objects.requireNonNull(guiItem, "guiItem cannot be null");
        validateSlot(slot);
        guiItems.put(slot, guiItem);
        onLayoutChanged();
        return this;
    }

    /** 1-indexed (row, column). */
    public @NotNull BaseGui setItem(int row, int column, @NotNull GuiItem guiItem) {
        return setItem(Slot.of(row, column), guiItem);
    }

    public @NotNull BaseGui setItem(@NotNull List<Integer> slots, @NotNull GuiItem guiItem) {
        Objects.requireNonNull(slots, "slots cannot be null");
        Objects.requireNonNull(guiItem, "guiItem cannot be null");
        for (int slot : slots) {
            setItem(slot, guiItem);
        }
        return this;
    }

    /** Fills the first available empty slots, skipping occupied ones. */
    public @NotNull BaseGui addItem(@NotNull GuiItem... items) {
        Objects.requireNonNull(items, "items cannot be null");
        int slot = 0;
        for (GuiItem item : items) {
            Objects.requireNonNull(item, "items cannot contain null");
            while (slot < size && guiItems.containsKey(slot)) {
                slot++;
            }
            if (slot >= size) {
                break;
            }
            guiItems.put(slot++, item);
        }
        onLayoutChanged();
        return this;
    }

    public @NotNull BaseGui removeItem(int slot) {
        guiItems.remove(slot);
        onLayoutChanged();
        return this;
    }

    public @NotNull BaseGui removeItem(int row, int column) {
        return removeItem(Slot.of(row, column));
    }

    /** Removes wherever it appears in this GUI. */
    public @NotNull BaseGui removeItem(@NotNull GuiItem guiItem) {
        Objects.requireNonNull(guiItem, "guiItem cannot be null");
        guiItems.values().removeIf(existing -> existing.equals(guiItem));
        onLayoutChanged();
        return this;
    }

    protected void onLayoutChanged() {
    }

    public @Nullable GuiItem getGuiItem(int slot) {
        return guiItems.get(slot);
    }

    /**
     * Resolves the item actually displayed in a slot right now, including dynamic content like a
     * {@link PaginatedGui} page. The click listener uses this so page/scroll items dispatch actions.
     */
    public @Nullable GuiItem itemAt(int slot) {
        return guiItems.get(slot);
    }

    /** Keeps the slot's action, pushes the change to viewers immediately. No-op if the slot is empty. */
    public @NotNull BaseGui updateItem(int slot, @NotNull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        GuiItem existing = guiItems.get(slot);
        if (existing == null) {
            return this;
        }
        GuiItem replacement = existing.withItemStack(itemStack);
        guiItems.put(slot, replacement);
        applyToInventory(() -> applyItem(slot, replacement));
        return this;
    }

    public @NotNull BaseGui updateItem(int slot, @NotNull GuiItem guiItem) {
        Objects.requireNonNull(guiItem, "guiItem cannot be null");
        validateSlot(slot);
        guiItems.put(slot, guiItem);
        applyToInventory(() -> applyItem(slot, guiItem));
        return this;
    }

    /**
     * Opens on the correct region thread; safe from any thread. Logs a warning (best-effort) if another
     * player already has this exact instance open, see the one-viewer-per-instance rule above.
     */
    public void open(@NotNull HumanEntity player) {
        Objects.requireNonNull(player, "player cannot be null");
        if (player.isSleeping()) {
            return;
        }
        for (HumanEntity viewer : inventory.getViewers()) {
            if (!viewer.getUniqueId().equals(player.getUniqueId())) {
                LOGGER.warning(getClass().getSimpleName() + " is being opened for " + player.getName()
                        + " while " + viewer.getName() + " already has this exact instance open. "
                        + "A BaseGui supports one viewer at a time; create a separate instance per player.");
                break;
            }
        }
        FoliaGUI.scheduler().runForEntity(player, () -> {
            populateInventory();
            player.openInventory(inventory);
        }, null);
    }

    public void close(@NotNull HumanEntity player) {
        Objects.requireNonNull(player, "player cannot be null");
        allowedCloses.add(player.getUniqueId());
        FoliaGUI.scheduler().runForEntity(player, player::closeInventory, null);
    }

    public void update() {
        applyToInventory(this::populateInventory);
    }

    /** Rebuilds contents every {@code ticks} ticks while a player has this open. {@code 0} disables. */
    public @NotNull BaseGui setUpdateInterval(long ticks) {
        this.updateIntervalTicks = Math.max(0, ticks);
        return this;
    }

    public long getUpdateInterval() {
        return updateIntervalTicks;
    }

    @ApiStatus.Internal
    public void startAutoUpdate(@NotNull HumanEntity player) {
        if (updateIntervalTicks <= 0) {
            return;
        }
        stopAutoUpdate(player);
        com.foliagui.scheduler.TaskHandle handle = FoliaGUI.scheduler().runForEntityTimer(
                player, this::populateInventory, () -> stopAutoUpdate(player),
                updateIntervalTicks, updateIntervalTicks);
        updateTasks.put(player.getUniqueId(), handle);
    }

    @ApiStatus.Internal
    public void stopAutoUpdate(@NotNull HumanEntity player) {
        com.foliagui.scheduler.TaskHandle handle = updateTasks.remove(player.getUniqueId());
        if (handle != null) {
            handle.cancel();
        }
    }

    /**
     * If set, a player-initiated close (Escape, inventory swap, ...) reopens the GUI immediately. Closes via
     * {@link #close(HumanEntity)} are unaffected. Useful for dialogs that must be resolved, not dismissed.
     */
    public @NotNull BaseGui setForceOpen(boolean forceOpen) {
        this.forceOpen = forceOpen;
        return this;
    }

    public boolean isForceOpen() {
        return forceOpen;
    }

    @ApiStatus.Internal
    public boolean consumeAllowedClose(@NotNull UUID viewerId) {
        return allowedCloses.remove(viewerId);
    }

    public void updateTitle(@NotNull String title) {
        Objects.requireNonNull(title, "title cannot be null");
        updateTitle(Text.of(title));
    }

    public void updateTitle(@NotNull Component title) {
        Objects.requireNonNull(title, "title cannot be null");
        FoliaGUI.scheduler().runGlobal(() -> {
            this.title = title;
            List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
            Inventory replacement = guiType == null
                    ? Bukkit.createInventory(this, size, title)
                    : Bukkit.createInventory(this, guiType.getInventoryType(), title);
            this.inventory = replacement;
            Arrays.fill(renderedItems, null);
            Arrays.fill(renderedStacks, null);
            populateInventory();
            // bracket with updating flag so the synchronous close/open events skip user callbacks
            for (HumanEntity viewer : viewers) {
                FoliaGUI.scheduler().runForEntity(viewer, () -> {
                    updating = true;
                    try {
                        viewer.openInventory(replacement);
                    } finally {
                        updating = false;
                    }
                }, null);
            }
        });
    }

    /**
     * Runs a mutation on the thread owning the current viewer's region (or immediately if no viewers).
     * Skips the scheduler round-trip if the calling thread already owns that region.
     */
    private void applyToInventory(@NotNull Runnable mutation) {
        List<HumanEntity> viewers = inventory.getViewers();
        if (viewers.isEmpty()) {
            mutation.run();
            return;
        }
        HumanEntity viewer = viewers.get(0);
        if (Bukkit.getServer().isOwnedByCurrentRegion(viewer)) {
            mutation.run();
            return;
        }
        // single-viewer GUI, all valid viewers share a region, dispatch on the first
        FoliaGUI.scheduler().runForEntity(viewer, mutation, null);
    }

    public @NotNull BaseGui addInteractionModifier(@NotNull InteractionModifier modifier) {
        interactionModifiers.add(modifier);
        return this;
    }

    public @NotNull BaseGui removeInteractionModifier(@NotNull InteractionModifier modifier) {
        interactionModifiers.remove(modifier);
        return this;
    }

    public @NotNull BaseGui clearInteractionModifiers() {
        interactionModifiers.clear();
        return this;
    }

    public boolean isModifierActive(@NotNull InteractionModifier modifier) {
        return interactionModifiers.contains(modifier);
    }

    /** Fires after slot-specific actions. */
    public @NotNull BaseGui setDefaultClickAction(@Nullable GuiAction<InventoryClickEvent> action) {
        this.defaultClickAction = action;
        return this;
    }

    public @NotNull BaseGui setDefaultTopClickAction(@Nullable GuiAction<InventoryClickEvent> action) {
        this.defaultTopClickAction = action;
        return this;
    }

    public @NotNull BaseGui setPlayerInventoryAction(@Nullable GuiAction<InventoryClickEvent> action) {
        this.playerInventoryAction = action;
        return this;
    }

    public @NotNull BaseGui setOutsideClickAction(@Nullable GuiAction<InventoryClickEvent> action) {
        this.outsideClickAction = action;
        return this;
    }

    public @NotNull BaseGui setDragAction(@Nullable GuiAction<InventoryDragEvent> action) {
        this.dragAction = action;
        return this;
    }

    public @NotNull BaseGui setOpenAction(@Nullable GuiAction<InventoryOpenEvent> action) {
        this.openAction = action;
        return this;
    }

    public @NotNull BaseGui setCloseAction(@Nullable GuiAction<InventoryCloseEvent> action) {
        this.closeAction = action;
        return this;
    }

    /** Fires before the default click action. */
    public @NotNull BaseGui setSlotAction(int slot, @Nullable GuiAction<InventoryClickEvent> action) {
        validateSlot(slot);
        if (action == null) {
            slotActions.remove(slot);
        } else {
            slotActions.put(slot, action);
        }
        return this;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public @NotNull GuiFiller filler() {
        return new GuiFiller(this);
    }

    public @NotNull List<Player> getViewerPlayers() {
        List<Player> players = new ArrayList<>();
        for (HumanEntity viewer : inventory.getViewers()) {
            if (viewer instanceof Player player) {
                players.add(player);
            }
        }
        return players;
    }

    public boolean isOpenFor(@NotNull HumanEntity player) {
        return GuiManager.getOpenGui(player) == this;
    }

    public @NotNull Component title() {
        return title;
    }

    public int getSize() {
        return size;
    }

    /** 0 for typed GUIs. */
    public int getRows() {
        return rows;
    }

    /** {@code null} for chest GUIs. */
    public @Nullable GuiType getGuiType() {
        return guiType;
    }

    public @NotNull Map<Integer, GuiItem> getGuiItems() {
        return guiItems;
    }

    public @Nullable GuiAction<InventoryClickEvent> getSlotAction(int slot) {
        return slotActions.get(slot);
    }

    public @Nullable GuiAction<InventoryClickEvent> getDefaultClickAction() {
        return defaultClickAction;
    }

    public @Nullable GuiAction<InventoryClickEvent> getDefaultTopClickAction() {
        return defaultTopClickAction;
    }

    public @Nullable GuiAction<InventoryClickEvent> getPlayerInventoryAction() {
        return playerInventoryAction;
    }

    public @Nullable GuiAction<InventoryClickEvent> getOutsideClickAction() {
        return outsideClickAction;
    }

    public @Nullable GuiAction<InventoryDragEvent> getDragAction() {
        return dragAction;
    }

    public @Nullable GuiAction<InventoryOpenEvent> getOpenAction() {
        return openAction;
    }

    public @Nullable GuiAction<InventoryCloseEvent> getCloseAction() {
        return closeAction;
    }

    public boolean isUpdating() {
        return updating;
    }

    protected void validateSlot(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("slot " + slot + " is out of bounds for size " + size);
        }
    }

    protected static @NotNull Player asPlayer(@NotNull HumanEntity entity) {
        return (Player) entity;
    }
}
