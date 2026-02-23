package com.palorder.smp.java.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LinkFishingRod implements RegisterCommands {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("linkfrod")
                .requires(PermissionUtil::isOwnerOrDev)
                .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("type", StringArgumentType.string())
                                .suggests((ctx, builder) ->
                                        net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("nuke", "stab","chunklaser","chunkdel","ArrowNuke","ArrowStab","void","Wolf","nuke_2"), builder))
                                .executes(context -> {
                                    ServerPlayer p = context.getSource().getPlayer();
                                    String type = StringArgumentType.getString(context, "type");
                                    assert p != null;
                                    ItemStack i = p.getMainHandItem();
                                    if (!(i.getItem() instanceof FishingRodItem)) return 0;
                                    i.getOrCreateTag().putString("RodType", type);
                                    i.setHoverName(Component.literal(type + " shot"));
                                    if (!type.equals("void")) {
                                        i.setDamageValue(i.getMaxDamage());
                                    } else {
                                        i.setHoverName(Component.literal("Stasis rod"));
                                        i.getOrCreateTag().putString("Voidrodowner",p.getStringUUID());
                                    }
                                    return 1;
                                })
                        )));
    }
}
