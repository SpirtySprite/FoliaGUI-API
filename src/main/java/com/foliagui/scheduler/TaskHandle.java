package com.foliagui.scheduler;

/**
 * A cancellable handle to a scheduled repeating task, abstracting over Folia's {@code ScheduledTask}
 * and Bukkit's {@code BukkitTask}.
 */
public interface TaskHandle {

    /** Safe to call multiple times and from any thread. */
    void cancel();

    boolean isCancelled();
}
