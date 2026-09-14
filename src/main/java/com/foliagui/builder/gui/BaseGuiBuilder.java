package com.foliagui.builder.gui;

import com.foliagui.gui.BaseGui;
import com.foliagui.gui.InteractionModifier;
import com.foliagui.item.GuiAction;
import com.foliagui.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public abstract class BaseGuiBuilder<G extends BaseGui, B extends BaseGuiBuilder<G, B>> {

    private Component title = Component.empty();
    private boolean clearModifiers;
    private final Set<InteractionModifier> removedModifiers = EnumSet.noneOf(InteractionModifier.class);

    private GuiAction<InventoryClickEvent> defaultClickAction;
    private GuiAction<InventoryClickEvent> defaultTopClickAction;
    private GuiAction<InventoryClickEvent> playerInventoryAction;
    private GuiAction<InventoryClickEvent> outsideClickAction;
    private GuiAction<InventoryDragEvent> dragAction;
    private GuiAction<InventoryOpenEvent> openAction;
    private GuiAction<InventoryCloseEvent> closeAction;
    private Consumer<G> postBuild;

    public @NotNull B title(@NotNull String title) {
        this.title = Text.of(title);
        return (B) this;
    }

    public @NotNull B title(@NotNull Component title) {
        this.title = title;
        return (B) this;
    }

    public @NotNull B enableAllInteractions() {
        this.clearModifiers = true;
        return (B) this;
    }

    public @NotNull B enableInteraction(@NotNull InteractionModifier modifier) {
        removedModifiers.add(modifier);
        return (B) this;
    }

    public @NotNull B onClick(@NotNull GuiAction<InventoryClickEvent> action) {
        this.defaultClickAction = action;
        return (B) this;
    }

    public @NotNull B onTopClick(@NotNull GuiAction<InventoryClickEvent> action) {
        this.defaultTopClickAction = action;
        return (B) this;
    }

    public @NotNull B onPlayerInventoryClick(@NotNull GuiAction<InventoryClickEvent> action) {
        this.playerInventoryAction = action;
        return (B) this;
    }

    public @NotNull B onOutsideClick(@NotNull GuiAction<InventoryClickEvent> action) {
        this.outsideClickAction = action;
        return (B) this;
    }

    public @NotNull B onDrag(@NotNull GuiAction<InventoryDragEvent> action) {
        this.dragAction = action;
        return (B) this;
    }

    public @NotNull B onOpen(@NotNull GuiAction<InventoryOpenEvent> action) {
        this.openAction = action;
        return (B) this;
    }

    public @NotNull B onClose(@NotNull GuiAction<InventoryCloseEvent> action) {
        this.closeAction = action;
        return (B) this;
    }

    public @NotNull B apply(@NotNull Consumer<G> consumer) {
        this.postBuild = this.postBuild == null ? consumer : this.postBuild.andThen(consumer);
        return (B) this;
    }

    protected @NotNull Component title() {
        return title;
    }

    protected @NotNull G finish(@NotNull G gui) {
        if (clearModifiers) {
            gui.clearInteractionModifiers();
        }
        for (InteractionModifier modifier : removedModifiers) {
            gui.removeInteractionModifier(modifier);
        }
        gui.setDefaultClickAction(defaultClickAction);
        gui.setDefaultTopClickAction(defaultTopClickAction);
        gui.setPlayerInventoryAction(playerInventoryAction);
        gui.setOutsideClickAction(outsideClickAction);
        gui.setDragAction(dragAction);
        gui.setOpenAction(openAction);
        gui.setCloseAction(closeAction);
        if (postBuild != null) {
            postBuild.accept(gui);
        }
        return gui;
    }

    public abstract @NotNull G create();
}
