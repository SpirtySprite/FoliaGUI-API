package com.foliagui.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs GUI work on the thread owning its target, whether the server is Folia (regionised) or classic
 * single-threaded Paper/Spigot. Implementation picked once at startup by {@link SchedulerProvider}.
 */
public interface Scheduler {

    /** Runs on the thread owning {@code entity} (Folia: its region; Paper: main thread). Use for anything touching a player's open inventory. {@code retired} runs instead if the entity is removed first, may be null. */
    void runForEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired);

    /** Like {@link #runForEntity}, delayed by {@code delayTicks} (minimum 1). */
    void runForEntityLater(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks);

    /** Like {@link #runForEntity}, repeating every {@code periodTicks} (minimum 1) after {@code initialDelayTicks} (minimum 1). */
    @NotNull
    TaskHandle runForEntityTimer(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired,
                                 long initialDelayTicks, long periodTicks);

    /** Runs on the thread owning {@code location} (Folia: that region; Paper: main thread). */
    void runForLocation(@NotNull Location location, @NotNull Runnable task);

    /** Runs on the global/main tick thread (Folia: global region scheduler; Paper: main thread). */
    void runGlobal(@NotNull Runnable task);

    /** Runs off the server threads entirely. */
    void runAsync(@NotNull Runnable task);

    boolean isFolia();
}
