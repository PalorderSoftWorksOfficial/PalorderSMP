package com.palorder.smp.java.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static com.palorder.smp.java.authDB.UUIDs.OWNER_UUID;
import static com.palorder.smp.java.authDB.UUIDs.OWNER_UUID2;

public class PermissionUtil {

    public static boolean isOwnerOrDev(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayer();
            if (player == null) return true;

            return player.getGameProfile().getId().equals(OWNER_UUID)
                    || player.getGameProfile().getId().equals(OWNER_UUID2)
                    || "dev".equalsIgnoreCase(player.getName().getString());
        } catch (Exception e) {
            return false;
        }
    }
}