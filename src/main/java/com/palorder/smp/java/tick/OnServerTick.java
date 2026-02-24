package com.palorder.smp.java.tick;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

import static com.palorder.smp.java.tick.runLater.scheduled;

public class OnServerTick {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        long time = e.getServer().overworld().getGameTime();
        List<Runnable> list = scheduled.remove((int) time);
        if (list != null) for (Runnable r : list) r.run();
    }
}
