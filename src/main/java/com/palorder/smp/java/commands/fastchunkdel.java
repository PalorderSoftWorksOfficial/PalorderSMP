package com.palorder.smp.java.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.palorder.smp.java.Implementations.spawnTNTNuke.spawnTNTNuke;

public class fastchunkdel implements RegisterCommands{

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fastchunkdel")
                .requires(PermissionUtil::isOwnerOrDev)
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                            if (player != null) {
                                spawnTNTNuke(player, 49152, "chunkdel", 1);
                                player.sendSystemMessage(Component.literal("fastchunkdeleted be ready lmao"));
                            }
                            else {
                                context.getSource().sendSuccess(() -> Component.literal("fastchunkdeleted be ready lmao."), false);
                            }
                            return 1;
                        }))
        );
    }
}
