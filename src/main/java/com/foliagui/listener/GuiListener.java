package com.foliagui.listener;

import com.foliagui.FoliaGUI;
import com.foliagui.gui.AnvilGui;
import com.foliagui.gui.BaseGui;
import com.foliagui.gui.GuiManager;
import com.foliagui.gui.GuiNavigator;
import com.foliagui.gui.InteractionModifier;
import com.foliagui.gui.MerchantGui;
import com.foliagui.gui.SignGui;
import com.foliagui.item.GuiAction;
import com.foliagui.item.GuiItem;
import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The single Bukkit listener backing every GUI. Recognises our inventories by their holder, delegates
 * cancel/allow decisions to {@link InteractionGuard}, bridges to the public events via {@link GuiEventBridge},
 * and dispatches per-GUI callbacks. Inventory events already fire on the region thread owning the acting
 * player, so callbacks run on the correct Folia thread with no extra scheduling here.
 */
public final class GuiListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(GuiListener.class.getName());

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGui gui)) {
            if (!AnvilGui.handleClick(event)) {
                MerchantGui.handleClick(event);
            }
            return;
        }

        Inventory clicked = event.getClickedInventory();

        if (clicked == null) {
            run(gui.getOutsideClickAction(), event);
            run(gui.getDefaultClickAction(), event);
            return;
        }

        if (clicked.equals(gui.getInventory())) {
            GuiItem item = gui.itemAt(event.getSlot());
            boolean protectedSlot = item != null && !item.isEditable();
            if (InteractionGuard.cancelTop(gui, event.getAction(), protectedSlot)) {
                event.setCancelled(true);
            }
            if (event.getWhoClicked() instanceof Player clicker) {
                event.setCancelled(GuiEventBridge.fireClick(clicker, gui, event, item));
            }
            run(gui.getSlotAction(event.getSlot()), event);
            if (item != null) {
                if (item.tryClick()) {
                    run(item.getAction(), event);
                    if (item.getClickSound() != null && event.getWhoClicked() instanceof Player player) {
                        player.playSound(player.getLocation(), item.getClickSound(),
                                item.getClickVolume(), item.getClickPitch());
                    }
                } else {
                    run(item.getCooldownBlockedAction(), event);
                }
            }
            run(gui.getDefaultTopClickAction(), event);
            run(gui.getDefaultClickAction(), event);
        } else {
            if (InteractionGuard.cancelBottom(gui, event.getAction())) {
                event.setCancelled(true);
            }
            run(gui.getPlayerInventoryAction(), event);
            run(gui.getDefaultClickAction(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGui gui)) {
            AnvilGui.handleDrag(event);
            return;
        }
        int topSize = gui.getInventory().getSize();
        boolean touchesGui = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        boolean touchesProtected = event.getRawSlots().stream()
                .anyMatch(slot -> slot < topSize && gui.itemAt(slot) != null && !gui.itemAt(slot).isEditable());
        if (touchesGui && (touchesProtected || gui.isModifierActive(InteractionModifier.PREVENT_ITEM_DRAG))) {
            event.setCancelled(true);
        }
        run(gui.getDragAction(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGui gui) || gui.isUpdating()) {
            return;
        }
        if (event.getPlayer() instanceof Player player && !GuiEventBridge.fireOpen(player, gui)) {
            event.setCancelled(true);
            return;
        }
        GuiManager.register(event.getPlayer(), gui);
        gui.startAutoUpdate(event.getPlayer());
        run(gui.getOpenAction(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGui gui)) {
            if (!AnvilGui.handleClose(event)) {
                MerchantGui.handleClose(event);
            }
            return;
        }
        if (gui.isUpdating()) {
            return;
        }
        gui.stopAutoUpdate(event.getPlayer());
        GuiManager.unregister(event.getPlayer());
        boolean allowedClose = gui.consumeAllowedClose(event.getPlayer().getUniqueId());
        run(gui.getCloseAction(), event);
        if (event.getPlayer() instanceof Player player) {
            GuiEventBridge.fireClose(player, gui);
            if (gui.isForceOpen() && !allowedClose) {
                FoliaGUI.scheduler().runForEntity(player, () -> gui.open(player), null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onSignChange(UncheckedSignChangeEvent event) {
        SignGui.handleSignChange(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GuiManager.unregister(event.getPlayer());
        GuiNavigator.clear(event.getPlayer());
        SignGui.handleQuit(event.getPlayer());
    }

    /** Logs instead of propagating, so one broken handler doesn't stop the rest of the callback chain. */
    private static <T extends org.bukkit.event.Event> void run(GuiAction<T> action, T event) {
        if (action == null) {
            return;
        }
        try {
            action.execute(event);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "A GuiAction threw an exception handling " + event.getClass().getSimpleName(), e);
        }
    }
}
