package com.palorder.smp.java.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public interface RegisterCommands {
    void register(CommandDispatcher<CommandSourceStack> dispatcher);
}