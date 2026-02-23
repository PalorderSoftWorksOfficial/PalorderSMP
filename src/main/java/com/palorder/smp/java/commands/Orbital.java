package com.palorder.smp.java.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.palorder.smp.java.authDB.UUIDs.*;
import static com.palorder.smp.java.scheduler.Scheduler.nukePendingConfirmation;
import static com.palorder.smp.java.scheduler.Scheduler.scheduler;

public class Orbital implements RegisterCommands {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("orbital")
                .requires(source -> {
                    try {
                        ServerPlayer player = source.getPlayer();
                        if (player != null) {
                            return player.getGameProfile().getId().equals(OWNER_UUID)
                                    || player.getGameProfile().getId().equals(OWNER_UUID2)
                                    || "dev".equalsIgnoreCase(player.getName().getString());
                        }
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            ServerPlayer target = source.getServer()
                                    .getPlayerList()
                                    .getPlayerByName(StringArgumentType.getString(context, "target"));

                            if (target == null) return 0;

                            UUID id = target.getGameProfile().getId();

                            if (!(id.equals(OWNER_UUID) || id.equals(DEV_UUID) || id.equals(OWNER_UUID2)))
                                return 0;

                            if (nukePendingConfirmation.contains(id)) {
                                target.sendSystemMessage(Component.literal("Pending confirmation! Use /orbitalConfirm"));
                            } else {
                                nukePendingConfirmation.add(id);
                                target.sendSystemMessage(Component.literal("Type /orbitalConfirm to spawn 2000 TNT packed in one block."));
                                scheduler.schedule(() -> nukePendingConfirmation.remove(id), 30, TimeUnit.SECONDS);
                            }
                            return 1;
                        })
                )
        );
    }
}