package com.palorder.smp.java.scheduler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Scheduler {
        // ---------------- Server / Scheduler ----------------
    public static final Set<UUID> nukePendingConfirmation = new HashSet<>();
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
}
