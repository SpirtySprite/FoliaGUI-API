package com.foliagui.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link Scheduler} implementation built on Paper's region-scheduler API. On Folia it dispatches to the
 * owning region's thread; on regular Paper the same API is a shim that runs everything on the main thread.
 * One implementation works correctly on both, so consumer code never branches on server flavour.
 */
public final class PaperFoliaScheduler implements Scheduler {

    private final Plugin plugin;
    private final boolean folia;

    public PaperFoliaScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public void runForEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired) {
        entity.getScheduler().run(plugin, t -> task.run(), retired);
    }

    @Override
    public void runForEntityLater(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, t -> task.run(), retired, Math.max(1L, delayTicks));
    }

    @Override
    public @NotNull TaskHandle runForEntityTimer(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired,
                                                 long initialDelayTicks, long periodTicks) {
        ScheduledTask scheduled = entity.getScheduler().runAtFixedRate(
                plugin, t -> task.run(), retired, Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
        return new ScheduledTaskHandle(scheduled);
    }

    @Override
    public void runForLocation(@NotNull Location location, @NotNull Runnable task) {
        Bukkit.getRegionScheduler().run(plugin, location, t -> task.run());
    }

    @Override
    public void runGlobal(@NotNull Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    @Override
    public boolean isFolia() {
        return folia;
    }

    /** Wraps a Folia/Paper {@link ScheduledTask} as the platform-neutral {@link TaskHandle}. */
    private record ScheduledTaskHandle(ScheduledTask task) implements TaskHandle {
        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }
}
