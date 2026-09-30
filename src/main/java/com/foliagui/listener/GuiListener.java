package com.foliagui.listener;

import com.foliagui.FoliaGUIService;
import com.foliagui.gui.AnvilGui;
import com.foliagui.gui.BaseGui;
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

public final class GuiListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(GuiListener.class.getName());

    private final FoliaGUIService service;

    public GuiListener(FoliaGUIService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGui gui)) {
            if (!AnvilGui.handleClick(service, event)) {
                MerchantGui.handleClick(service, event);
            }
            return;
        }
        if (!gui.belongsTo(service)) {
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
            boolean guarded = InteractionGuard.cancelTop(gui, event.getAction(), protectedSlot);
            if (guarded) {
                event.setCancelled(true);
            }
            if (event.getWhoClicked() instanceof Player clicker) {
                event.setCancelled(GuiEventBridge.fireClick(clicker, gui, event, item) || guarded);
            }
            run(gui.getSlotAction(event.getSlot()), event);
            if (item != null) {
                if (item.tryClick(event.getWhoClicked().getUniqueId())) {
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
            AnvilGui.handleDrag(service, event);
            return;
        }
        if (!gui.belongsTo(service)) {
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
        if (!(event.getInventory().getHolder() instanceof BaseGui gui) || !gui.belongsTo(service)
                || gui.isUpdating()) {
            return;
        }
        if (event.getPlayer() instanceof Player player && !GuiEventBridge.fireOpen(player, gui)) {
            event.setCancelled(true);
            return;
        }
        service.guis().register(event.getPlayer(), gui);
        gui.startAutoUpdate(event.getPlayer());
        run(gui.getOpenAction(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseGui gui)) {
            if (!AnvilGui.handleClose(service, event)) {
                MerchantGui.handleClose(service, event);
            }
            return;
        }
        if (!gui.belongsTo(service) || gui.isUpdating()) {
            return;
        }
        gui.stopAutoUpdate(event.getPlayer());
        service.guis().unregister(event.getPlayer());
        boolean allowedClose = gui.consumeAllowedClose(event.getPlayer().getUniqueId());
        run(gui.getCloseAction(), event);
        if (event.getPlayer() instanceof Player player) {
            GuiEventBridge.fireClose(player, gui);
            if (gui.isForceOpen() && !allowedClose) {
                service.scheduler().runForEntity(player, () -> gui.open(player), null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onSignChange(UncheckedSignChangeEvent event) {
        SignGui.handleSignChange(service, event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.guis().unregister(event.getPlayer());
        service.navigation().clear(event.getPlayer());
        SignGui.handleQuit(service, event.getPlayer());
    }

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
