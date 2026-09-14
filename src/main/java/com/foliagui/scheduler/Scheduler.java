package com.foliagui.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Scheduler {

    void runForEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired);

    void runForEntityLater(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks);

    @NotNull
    TaskHandle runForEntityTimer(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retired,
                                 long initialDelayTicks, long periodTicks);

    void runForLocation(@NotNull Location location, @NotNull Runnable task);

    void runGlobal(@NotNull Runnable task);

    void runAsync(@NotNull Runnable task);

    boolean isFolia();
}
