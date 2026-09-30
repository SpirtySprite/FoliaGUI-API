package com.foliagui.gui;

import com.foliagui.FoliaGUIService;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Tracks which {@link BaseGui} each player currently has open, for one {@code FoliaGUIService}. */
public final class GuiRegistry {

    private final Map<UUID, BaseGui> open = new ConcurrentHashMap<>();
    private final FoliaGUIService service;

    @ApiStatus.Internal
    public GuiRegistry(@NotNull FoliaGUIService service) {
        this.service = service;
    }

    @ApiStatus.Internal
    public void register(@NotNull HumanEntity player, @NotNull BaseGui gui) {
        open.put(player.getUniqueId(), gui);
    }

    @ApiStatus.Internal
    public void unregister(@NotNull HumanEntity player) {
        open.remove(player.getUniqueId());
    }

    public @Nullable BaseGui getOpenGui(@NotNull HumanEntity player) {
        return open.get(player.getUniqueId());
    }

    public boolean hasGuiOpen(@NotNull HumanEntity player) {
        return open.containsKey(player.getUniqueId());
    }

    public boolean hasAnyScreenOpen(@NotNull HumanEntity player) {
        return hasGuiOpen(player) || service.sessions().hasAny(player);
    }

    public int openCount() {
        return open.size();
    }

    public void refresh(@NotNull BaseGui gui) {
        gui.update();
    }

    public @NotNull Collection<BaseGui> openGuis() {
        return open.values();
    }

    public @NotNull List<Player> viewersOf(@NotNull BaseGui gui) {
        List<Player> viewers = new ArrayList<>();
        for (Map.Entry<UUID, BaseGui> entry : open.entrySet()) {
            if (entry.getValue() == gui) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    viewers.add(player);
                }
            }
        }
        return viewers;
    }

    public @NotNull List<BaseGui> openGuisOfType(@NotNull Class<? extends BaseGui> type) {
        List<BaseGui> matches = new ArrayList<>();
        for (BaseGui gui : open.values()) {
            if (type.isInstance(gui)) {
                matches.add(gui);
            }
        }
        return matches;
    }

    public void closeAll() {
        boolean enabled = service.plugin().isEnabled();
        for (UUID uuid : open.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (enabled) {
                service.scheduler().runForEntity(player, player::closeInventory, null);
                continue;
            }
            try {
                player.closeInventory();
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void closeAll(@NotNull Predicate<BaseGui> filter) {
        for (Map.Entry<UUID, BaseGui> entry : open.entrySet()) {
            if (!filter.test(entry.getValue())) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                service.scheduler().runForEntity(player, player::closeInventory, null);
            }
        }
    }

    @ApiStatus.Internal
    public void clearAll() {
        open.clear();
    }
}
