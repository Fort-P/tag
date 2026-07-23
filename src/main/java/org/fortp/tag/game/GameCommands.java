package org.fortp.tag.game;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameCommands {
    public static int start(CommandContext<CommandSourceStack> context) {
        if (!GameManager.isGameActive() && !GameManager.isRoundPending()) {
            GameManager.startGame();
            context.getSource().sendSuccess(() -> Component.literal("Tag round warning started."), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("A Tag round is already pending or active!"));
            return 0;
        }
    }

    public static int stop(CommandContext<CommandSourceStack> context) {
        if (GameManager.isGameActive() || GameManager.isRoundPending()) {
            GameManager.stopGame();
            context.getSource().sendSuccess(() -> Component.literal("Game stopped!"), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("No game is active!"));
            return 0;
        }
    }

    public static int leaderboard(CommandContext<CommandSourceStack> context) {
        GameData gameData = GameData.getGameData(context.getSource().getServer());
        ServerPlayer player = context.getSource().getPlayer();
        AtomicBoolean inTop5 = new AtomicBoolean(false);
        context.getSource().sendSuccess(() -> {
            MutableComponent leaderboard = Component.literal("\n  LEADERBOARD  \n\n")
                    .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.DARK_AQUA);

            gameData.getScores().entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> {
                        UUID uuid = entry.getKey();
                        int score = entry.getValue();
                        String name;

                        if (player != null && uuid.equals(context.getSource().getPlayer().getUUID())) {
                            inTop5.set(true);
                        }

                        ServerPlayer lbPlayer = context.getSource().getServer().getPlayerList().getPlayer(uuid);
                        if (lbPlayer != null) {
                            name = lbPlayer.getPlainTextName();
                        } else {
                            Optional<GameProfile> profile = context.getSource().getServer().services().profileResolver().fetchById(uuid);
                            name = profile.map(GameProfile::name).orElse(uuid.toString());
                        }
                        MutableComponent scoreLine = Component.literal(name + " - " + score + "\n")
                                .withStyle(style -> style.withUnderlined(false));

                        leaderboard.append(scoreLine);
                    });

            if (player != null && !inTop5.get()) {
                leaderboard.append(Component.literal("···\n" + player.getPlainTextName() + " - " + gameData.getScores().get(player.getUUID()) + "\n"));
            }

            return leaderboard;
        }, false);
        return 1;
    }

    public static int join(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            GameData gameData = GameData.getGameData(context.getSource().getServer());
            if (gameData.getScores().containsKey(player.getUUID())) {
                context.getSource().sendFailure(Component.literal("You've already joined!"));
                return 0;
            }

            gameData.addScore(player.getUUID(), 0);
            context.getSource().sendSuccess(() -> Component.literal("You've joined!").withStyle(ChatFormatting.GREEN), false);
            return 1;
        } catch (NullPointerException e) {
            context.getSource().sendFailure(Component.literal("You can only run this command in game!"));
            return 0;
        }
    }

    public static int editScore(CommandContext<CommandSourceStack> context) {
        try {
            Collection<NameAndId> playerList = GameProfileArgument.getGameProfiles(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");
            GameData gameData = GameData.getGameData(context.getSource().getServer());

            for (NameAndId entry : playerList) {
                UUID uuid = entry.id();
                String name = entry.name();

                gameData.addScore(uuid, amount);

                context.getSource().sendSuccess(() -> Component.literal("Added " +  amount + " to " + name), true);
            }
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
}
