package com.palorder.smp.java.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.palorder.smp.java.Implementations.spawnTNTNuke.spawnTNTNuke;

public class faststab implements RegisterCommands {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("faststab")
                .requires(PermissionUtil::isOwnerOrDev)
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                            if (player != null) {
                                spawnTNTNuke(player, 900, "stab", 1);
                                player.sendSystemMessage(Component.literal("Faststabbed be ready lmao"));
                            }
                            else {
                                context.getSource().sendSuccess(() -> Component.literal("Faststabbed be ready lmao."), false);
                            }
                            return 1;
                        }))
        );
    }
}
