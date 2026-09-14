package com.foliagui.listener;

import com.foliagui.gui.BaseGui;
import com.foliagui.gui.InteractionModifier;
import com.foliagui.item.GuiItem;
import org.bukkit.event.inventory.InventoryAction;

final class InteractionGuard {

    private InteractionGuard() {
    }

    static boolean cancelTop(BaseGui gui, InventoryAction action, boolean protectedSlot) {
        return switch (action) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE -> protectedSlot || gui.isModifierActive(InteractionModifier.PREVENT_ITEM_PLACE);
            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, COLLECT_TO_CURSOR ->
                    protectedSlot || gui.isModifierActive(InteractionModifier.PREVENT_ITEM_TAKE);
            case MOVE_TO_OTHER_INVENTORY -> protectedSlot || gui.isModifierActive(InteractionModifier.PREVENT_ITEM_TAKE);
            case SWAP_WITH_CURSOR, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD ->
                    protectedSlot || gui.isModifierActive(InteractionModifier.PREVENT_ITEM_SWAP);
            case DROP_ONE_SLOT, DROP_ALL_SLOT -> protectedSlot || gui.isModifierActive(InteractionModifier.PREVENT_ITEM_DROP);
            case NOTHING -> false;
            default -> protectedSlot || gui.isModifierActive(InteractionModifier.PREVENT_OTHER_ACTIONS);
        };
    }

    static boolean cancelBottom(BaseGui gui, InventoryAction action) {
        return switch (action) {
            case MOVE_TO_OTHER_INVENTORY -> gui.isModifierActive(InteractionModifier.PREVENT_ITEM_PLACE);
            case COLLECT_TO_CURSOR -> gui.isModifierActive(InteractionModifier.PREVENT_ITEM_TAKE);
            case UNKNOWN -> gui.isModifierActive(InteractionModifier.PREVENT_OTHER_ACTIONS);
            default -> false;
        };
    }
}
