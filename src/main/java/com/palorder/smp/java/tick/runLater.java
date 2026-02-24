package com.palorder.smp.java.tick;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class runLater {
    public static final Map<Integer, List<Runnable>> scheduled = new HashMap<>();

    public static void runLater(ServerLevel world, int ticks, Runnable r) {
        int targetTick = (int) (world.getGameTime() + ticks);
        scheduled.computeIfAbsent(targetTick, k -> new ArrayList<>()).add(r);
    }
}
