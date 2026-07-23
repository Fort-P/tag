package org.fortp.tag;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.permissions.PermissionLevel;
import org.fortp.tag.game.GameCommands;

public class TagCommands {
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(
                Commands.literal("taggame")
                        .then(Commands.literal("start").requires(Permissions.require("tag.start", PermissionLevel.MODERATORS))
                                .executes(GameCommands::start))
                        .then(Commands.literal("stop").requires(Permissions.require("tag.stop", PermissionLevel.MODERATORS))
                                .executes(GameCommands::stop))
                        .then(Commands.literal("leaderboard")
                                .executes(GameCommands::leaderboard))
                        .then(Commands.literal("join")
                                .executes(GameCommands::join))
                        .then(Commands.literal("editScore").requires(Permissions.require("tag.editscore", PermissionLevel.MODERATORS))
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(GameCommands::editScore)))));
    }
}
