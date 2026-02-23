package com.palorder.smp.java.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class CommandManager {

    private static final RegisterCommands[] COMMANDS = {
            new Orbital(),
            new OrbitalConfirm(),
            new fastorbital(),
            new faststab(),
            new fastchunklaser(),
            new fastchunkdel(),
            new LinkFishingRod()
    };

    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (RegisterCommands command : COMMANDS) {
            command.register(dispatcher);
        }
    }
}