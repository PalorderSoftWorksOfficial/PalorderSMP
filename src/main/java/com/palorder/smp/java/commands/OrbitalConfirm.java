package com.palorder.smp.java.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static com.palorder.smp.java.Implementations.spawnTNTNuke.spawnTNTNuke;
import static com.palorder.smp.java.scheduler.Scheduler.nukePendingConfirmation;


public class OrbitalConfirm implements RegisterCommands {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
         dispatcher.register(Commands.literal("orbitalConfirm")
                 .requires(PermissionUtil::isOwnerOrDev)
                    .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests((ctx, builder) ->
                                                net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("nuke", "stab", "chunklaser", "chunkdel","ArrowNuke","ArrowStab","void","Wolf","nuke_2"), builder))
                                        .then(Commands.argument("layers", IntegerArgumentType.integer(1, 5000))
                                                .executes(context -> {
                                                    CommandSourceStack source = context.getSource();
                                                    ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context, "target"));
                                                    if (player == null) return 0;
                                                    int tntCount = IntegerArgumentType.getInteger(context, "amount");
                                                    String type = StringArgumentType.getString(context, "type");
                                                    int layers = IntegerArgumentType.getInteger(context, "layers");
                                                    if (!type.equalsIgnoreCase("nuke") && !type.equalsIgnoreCase("stab") && !type.equalsIgnoreCase("chunklaser") && !type.equalsIgnoreCase("chunkdel")) type = "nuke";
                                                    if (nukePendingConfirmation.remove(player.getGameProfile().getId()))
                                                        spawnTNTNuke(player, tntCount, type, layers);
                                                    return 1;
                                                }))))));
    }
}