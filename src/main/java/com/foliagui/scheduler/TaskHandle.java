package com.foliagui.scheduler;

public interface TaskHandle {

    void cancel();

    boolean isCancelled();
}
