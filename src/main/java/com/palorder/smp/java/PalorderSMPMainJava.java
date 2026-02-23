package com.palorder.smp.java;

import com.mojang.brigadier.Command;
import com.palorder.smp.java.commands.CommandManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.palorder.smp.java.tickRelated.runLater.scheduled;

@Mod("palordersmp_tweaked")
@Mod.EventBusSubscriber(modid = "palordersmp_tweaked", value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PalorderSMPMainJava {
    public static final Logger logger =
            LoggerFactory.getLogger(PalorderSMPMainJava.class);

    private static final Logger log =
            LoggerFactory.getLogger(PalorderSMPMainJava.class);
    public PalorderSMPMainJava() {
        MinecraftForge.EVENT_BUS.register(this);
        if (ModList.get().isLoaded("computercraft")) {
            logger.info("ComputerCraft is installed, Registering addon stuff. (non-existent lmao)");
        } else {
            logger.warn("ComputerCraft is NOT present!");
        }
    }
        @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        long time = e.getServer().overworld().getGameTime();
        List<Runnable> list = scheduled.remove((int) time);
        if (list != null) for (Runnable r : list) r.run();
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        CommandManager.registerAll(event.getServer().getCommands().getDispatcher());
        MinecraftServer server = event.getServer();
    }
}
