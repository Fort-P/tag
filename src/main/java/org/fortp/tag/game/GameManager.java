package org.fortp.tag.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.fortp.tag.Tag;
import org.fortp.tag.TickScheduler;
import org.fortp.tag.config.ConfigManager;
import org.fortp.tag.integration.AfkPlusIntegration;
import org.fortp.tag.integration.VanishIntegration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private static GameData gameData;
    private static MinecraftServer server;

    private static boolean roundPending = false;
    private static boolean gameActive = false;
    private static int warningTicksRemaining = 0;
    private static int roundTicks = 0;
    private static int scoreTicks = 0;
    private static int breakawayTicks = 0;
    private static int runnerRoundPoints = 0;
    private static UUID runner;
    private static ChunkPos campOrigin;
    private static int campTicks = 0;
    private static boolean runnerCamping = false;

    public static void initialize(MinecraftServer server) {
        GameManager.server = server;
        gameData = GameData.getGameData(server);
        ScheduleGame();
        Tag.LOGGER.debug("GameManager initialized");
    }

    private static void ScheduleGame() {
        int delay = ThreadLocalRandom.current().nextInt(45 * 60 * 20, 60 * 60 * 20 + 1);
        TickScheduler.schedule(() -> {
            startGame();
            ScheduleGame();
        },  delay);
    }

    public static void startGame() {
        Tag.LOGGER.debug("startGame() called. roundPending={}, gameActive={}", roundPending, gameActive);
        if (server == null) return;
        if (roundPending || gameActive) return;

        roundPending = true;
        warningTicksRemaining = ConfigManager.get().getWarningSeconds() * 20;
        sendMessageToAllPlaying(
                Component.literal("A Tag round will begin in 5 minutes")
                        .withStyle(ChatFormatting.GOLD));
        Tag.LOGGER.info("Tag round warning started. Ticks remaining: {}", warningTicksRemaining);
    }

    public static void stopGame() {
        Tag.LOGGER.info("stopGame() called. Forcing round to end");
        endRound(Component.literal("Tag round stopped").withStyle(ChatFormatting.YELLOW), false, false);
    }

    public static void tick(MinecraftServer tickServer) {
        if (server == null) {
            initialize(tickServer);
        }

        if (roundPending) {
            tickPendingRound();
        }

        if (gameActive) {
            tickActiveRound();
        }
    }

    public static boolean checkTag(UUID taggerId, UUID targetId) {
        Tag.LOGGER.debug("checkTag() called by taggerId={} on targetId={}", taggerId, targetId);
        if (!gameActive || !targetId.equals(runner)) {
            Tag.LOGGER.debug("Tag invalid: gameActive={}, runnerId={}, isTargetRunner={}", gameActive, runner, targetId.equals(runner));
            return false;
        }
        if (breakawayTicks > 0) {
            Tag.LOGGER.debug("Tag blocked: Breakaway period is active ({} ticks remaining)", breakawayTicks);
            return true;
        }

        int bounty = ConfigManager.get().getTagBounty() + (int) Math.round(runnerRoundPoints / 4.0);
        if (gameData != null) {
            gameData.addScore(taggerId, bounty);
            gameData.addTaggerScore(taggerId, bounty);
            gameData.addTag(taggerId);
            gameData.setHighScore(taggerId, Math.max(gameData.getHighScore(taggerId), bounty));
        }

        String taggerName = getPlayerName(taggerId);
        String runnerName = getPlayerName(runner);
        Tag.LOGGER.info("Valid tag! Tagger: {}, Runner: {}, Bounty: {}", taggerName, runnerName, bounty);
        endRound(Component.literal(taggerName + " tagged " + runnerName + "! +" + bounty + " points")
                .withStyle(ChatFormatting.GOLD), false, true);
        return true;
    }

    public static boolean shouldBlockMobilityItem(ServerPlayer player, ItemStack stack) {
        if (!gameActive || runner == null || player == null || stack == null) return false;
        if (!stack.is(Items.ENDER_PEARL)
                && !(EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0.0F
                    && player.isInWaterOrRain()))
            return false;
        if (player.getUUID().equals(runner)) {
            Tag.LOGGER.debug("Blocked mobility item {} for runner: {}", stack.getItem(), player.getName().getString());
            return true;
        }

        ServerPlayer runnerPlayer = server.getPlayerList().getPlayer(runner);
        boolean block = runnerPlayer != null
                && player.level().dimension().equals(runnerPlayer.level().dimension())
                && player.distanceToSqr(runnerPlayer) <= ConfigManager.get().getMobilityLockoutDistanceSqr();
        if (block) {
            Tag.LOGGER.debug("Blocked mobility item {} for chaser: {} (too close to runner): {} blocks", stack.getItem(), player.getName().getString(), player.distanceToSqr(runnerPlayer));
        }
        return block;
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        Tag.LOGGER.debug("Player disconnected: {}", player.getName().getString());
        if (gameActive && player.getUUID().equals(runner)) {
            Tag.LOGGER.info("Runner disconnected! Triggering forfeit");
            forfeitRunner("disconnect");
        }
    }

    public static void onPlayerConnect(ServerPlayer player) {
        Tag.LOGGER.debug("Player connected: {}", player.getName().getString());
        if (gameActive && gameData.getScores().containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("There's an active round of tag!").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal(player.level().getPlayers(p -> p.getUUID().equals(runner)).getFirst().getPlainTextName() + " is the runner!").withStyle(ChatFormatting.GOLD));
        }
    }

    public static void onPlayerChangedLevel(ServerPlayer player, ServerLevel destination) {
        Tag.LOGGER.debug("Player {} changed level to {}", player.getName().getString(), destination.dimension());
        if (gameActive
                && player.getUUID().equals(runner)
                && !destination.dimension().equals(Level.OVERWORLD)) {
            Tag.LOGGER.info("Runner left the Overworld! Triggering forfeit");
            forfeitRunner("leaving the Overworld");
        }
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (gameActive && player.getUUID().equals(runner)) {
            Tag.LOGGER.info("Runner died! Triggering forfeit");
            forfeitRunner("death");
        }
    }

    public static boolean isGameActive() {
        return gameActive;
    }

    public static boolean isRoundPending() {
        return roundPending;
    }

    public static UUID getRunner() {
        return runner;
    }

    public static int getPlayerScore(UUID player) {
        if (gameData != null) {
            return gameData.getScores().getOrDefault(player, 0);
        }
        return 0;
    }

    private static void tickPendingRound() {
        warningTicksRemaining--;
        if (warningTicksRemaining > 0 && warningTicksRemaining % 1200 == 0) { // Every minute
            Tag.LOGGER.debug("Pending round ticks remaining: {}", warningTicksRemaining);
        }
        if (warningTicksRemaining > 0) return;

        Tag.LOGGER.info("Warning ticks depleted. Starting round now");
        roundPending = false;
        selectAndStartRound();
    }

    private static void tickActiveRound() {
        ServerPlayer runnerPlayer = server.getPlayerList().getPlayer(runner);
        if (runnerPlayer == null) {
            Tag.LOGGER.debug("tickActiveRound: runnerPlayer is null (disconnected?), skipping..");
            return;
        }

        if (breakawayTicks > 0) {
            breakawayTicks--;
            if (breakawayTicks == 0) {
                Tag.LOGGER.info("Breakaway period ended! Runner can now be tagged");
            }
        }

        runnerPlayer.removeEffect(MobEffects.INVISIBILITY);
        if (runnerPlayer.getInventory().getSlot(EquipmentSlot.HEAD.getIndex(36)).get().getItem().equals(Items.CARVED_PUMPKIN)) {
            ItemStack pumpkin = runnerPlayer.getInventory().getSlot(EquipmentSlot.HEAD.getIndex(36)).get();
            runnerPlayer.getInventory().placeItemBackInInventory(pumpkin);
        }

        updateCampingState(runnerPlayer);
        scoreTicks++;
        roundTicks++;

        if (roundTicks % 1200 == 0) { // Every minute
            Tag.LOGGER.debug("Round has been active for {} ticks. Runner: {}", roundTicks, runnerPlayer.getName().getString());
        }

        if (scoreTicks >= ConfigManager.get().getScoreIntervalSeconds() * 20) {
            scoreTicks = 0;
            awardRunnerInterval();
        }

        if (roundTicks > ConfigManager.get().getMaxGameDurationSeconds() * 20) {
            endRound(Component.literal(runnerPlayer.getName().getString() + " has gotten away!").withStyle(ChatFormatting.GREEN), false, true);
        }
    }

    private static void selectAndStartRound() {
        Tag.LOGGER.debug("selectAndStartRound() called. Gathering eligible players...");
        List<ServerPlayer> eligiblePlayers = server.getPlayerList().getPlayers().stream()
                .filter(GameManager::isEligibleRunner)
                .toList();

        if (eligiblePlayers.size() < 2) {
            sendMessageToAllPlaying(
                    Component.literal("Tag round canceled: not enough eligible players in the Overworld")
                            .withStyle(ChatFormatting.RED));
            Tag.LOGGER.warn("Round canceled: {} eligible players found", eligiblePlayers.size());
            return;
        }

        Tag.LOGGER.debug("Found {} eligible players. Rolling runner...", eligiblePlayers.size());
        ServerPlayer selected = rollWeightedRunner(eligiblePlayers);
        runner = selected.getUUID();
        gameActive = true;
        roundTicks = 0;
        scoreTicks = 0;
        breakawayTicks = ConfigManager.get().getBreakawaySeconds() * 20;
        runnerRoundPoints = 0;
        campOrigin = selected.chunkPosition();
        campTicks = 0;
        runnerCamping = false;

        if (gameData != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getUUID().equals(runner)) {
                    gameData.setRunnerWeight(player.getUUID(), 1.0);
                } else {
                    gameData.addRunnerWeight(player.getUUID(), ConfigManager.get().getPityIncrement());
                }
            }
        }

        if (selected.isPassenger() && selected.getVehicle() instanceof HappyGhast) {
            selected.stopRiding();
        }
        selected.addEffect(new MobEffectInstance(MobEffects.SPEED, ConfigManager.get().getBreakawaySeconds() * 20, 1, false, true, true));
        sendTitleToAllPlaying(Component.literal(selected.getName().getString() + " is THE RUNNER!")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        sendMessageToAllPlaying(
                Component.literal(selected.getName().getString()
                                + " is the runner. Tag them with a left-click!")
                        .withStyle(ChatFormatting.GOLD));
        Tag.LOGGER.info("{} selected as runner", selected.getName().getString());
        Tag.LOGGER.debug("Round setup complete. Breakaway ticks set to {}", breakawayTicks);
    }

    private static ServerPlayer rollWeightedRunner(List<ServerPlayer> eligiblePlayers) {
        double totalWeight = 0.0;
        for (ServerPlayer player : eligiblePlayers) {
            totalWeight += gameData != null ? gameData.getRunnerWeight(player.getUUID()) : 1.0;
            gameData.addTaggerTime(player.getUUID());
        }

        Tag.LOGGER.debug("Rolling runner. Total weight: {}", totalWeight);
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cursor = 0.0;
        for (ServerPlayer player : eligiblePlayers) {
            cursor += gameData != null ? gameData.getRunnerWeight(player.getUUID()) : 1.0;
            if (roll < cursor) {
                Tag.LOGGER.debug("Rolled runner: {} (roll: {}, cursor: {})", player.getName().getString(), roll, cursor);
                gameData.removeTaggerTime(player.getUUID());
                gameData.addRunnerTime(player.getUUID());
                return player;
            }
        }

        Tag.LOGGER.debug("Fell through to last eligible player: {}", eligiblePlayers.getLast().getName().getString());
        gameData.removeTaggerTime(eligiblePlayers.getLast().getUUID());
        gameData.addRunnerTime(eligiblePlayers.getLast().getUUID());
        return eligiblePlayers.getLast();
    }

    public static boolean isEligibleRunner(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.level().dimension().equals(Level.OVERWORLD)
                && gameData.getScores().containsKey(player.getUUID())
                && !AfkPlusIntegration.isPlayerAfk(player)
                && !VanishIntegration.IsPlayerVanished(server, player);
    }

    private static void updateCampingState(ServerPlayer runnerPlayer) {
        ChunkPos currentChunk = runnerPlayer.chunkPosition();
        if (campOrigin == null || leftCampingArea(currentChunk)) {
            if (campTicks > 0) {
                Tag.LOGGER.debug("Runner left camping area or initialized. Resetting camp origin to {} and camp ticks", currentChunk);
            }
            campOrigin = currentChunk;
            campTicks = 0;
            if (runnerCamping) {
                runnerCamping = false;
                runnerPlayer.removeEffect(MobEffects.GLOWING);
                runnerPlayer.sendSystemMessage(Component.literal("You are moving again. Points resumed")
                        .withStyle(ChatFormatting.GREEN));
                Tag.LOGGER.info("Runner {} is no longer camping", runnerPlayer.getName().getString());
            }
            return;
        }

        campTicks++;
        if (campTicks % 200 == 0 && !runnerCamping) { // Every 10 seconds
            Tag.LOGGER.debug("Runner has been in the same chunk area for {} ticks ({} needed to camp)", campTicks, ConfigManager.get().getCampingSeconds() * 20);
        }

        if (!runnerCamping && campTicks >= ConfigManager.get().getCampingSeconds() * 20) {
            runnerCamping = true;
            runnerPlayer.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0, false, true, true));
            runnerPlayer.sendSystemMessage(Component.literal("You've stopped moving! Keep running to earn points").withStyle(ChatFormatting.RED), false);
            Tag.LOGGER.info("Runner {} has triggered camping penalty!", runnerPlayer.getName().getString());
        }
    }

    private static boolean leftCampingArea(ChunkPos currentChunk) {
        return Math.abs(currentChunk.x() - campOrigin.x()) > ConfigManager.get().getCampingChunkRadius()
                || Math.abs(currentChunk.z() - campOrigin.z()) > ConfigManager.get().getCampingChunkRadius();
    }

    private static void awardRunnerInterval() {
        if (runnerCamping || gameData == null || runner == null) {
            Tag.LOGGER.debug("Skipping interval award. runnerCamping={}, gameData={}, runner={}", runnerCamping, gameData != null, runner);
            return;
        }

        int chasers = getActiveChaserCount();
        if (chasers <= 0) {
            Tag.LOGGER.debug("Skipping interval award. Active chasers: {}", chasers);
            return;
        }

        int points = (int) Math.round(ConfigManager.get().getBasePointsPerInterval() * (1.0 + Math.sqrt(chasers)));
        gameData.addScore(runner, points);
        gameData.addRunnerScore(runner, points);
        runnerRoundPoints += points;
        gameData.setHighScore(runner, Math.max(gameData.getHighScore(runner), runnerRoundPoints));

        Tag.LOGGER.debug("Awarding {} interval points to runner. Active chasers: {}. Total round points so far: {}", points, chasers, runnerRoundPoints);

        ServerPlayer runnerPlayer = server.getPlayerList().getPlayer(runner);
        if (runnerPlayer != null) {
            runnerPlayer.sendSystemMessage(Component.literal("Survival payout: +" + points + " points")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    private static int getActiveChaserCount() {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(runner)
                    && player.isAlive()
                    && !player.isSpectator()
                    && gameData.getScores().containsKey(player.getUUID())
                    && !AfkPlusIntegration.isPlayerAfk(player)
                    && !VanishIntegration.IsPlayerVanished(server, player)) {
                count++;
            }
        }
        return count;
    }

    private static void forfeitRunner(String reason) {
        Tag.LOGGER.info("forfeitRunner() called. Reason: {}", reason);
        UUID forfeitingRunner = runner;
        String runnerName = getPlayerName(forfeitingRunner);
        if (gameData != null && forfeitingRunner != null) {
            if (runnerRoundPoints > 0) {
                gameData.addScore(forfeitingRunner, -runnerRoundPoints);
                gameData.addRunnerScore(forfeitingRunner, -runnerRoundPoints);
            }
            gameData.addScore(forfeitingRunner, ConfigManager.get().getForfeitPenalty());
            gameData.addRunnerScore(forfeitingRunner, ConfigManager.get().getForfeitPenalty());
        }

        endRound(Component.literal(runnerName + " forfeited by " + reason + " and lost 50 points")
                .withStyle(ChatFormatting.RED), true, true);
    }

    private static void endRound(Component message, boolean forfeited, boolean announce) {
        Tag.LOGGER.debug("endRound() called. forfeited={}, announce={}", forfeited, announce);
        roundPending = false;
        warningTicksRemaining = 0;

        if (runner != null) {
            ServerPlayer runnerPlayer = server.getPlayerList().getPlayer(runner);
            if (runnerPlayer != null && runnerCamping) {
                runnerPlayer.removeEffect(MobEffects.GLOWING);
            }
        }

        gameActive = false;
        runner = null;
        roundTicks = 0;
        scoreTicks = 0;
        breakawayTicks = 0;
        runnerRoundPoints = 0;
        campOrigin = null;
        campTicks = 0;
        runnerCamping = false;

        if (announce) {
            sendMessageToAllPlaying(message);
        } else if (!forfeited) {
            Tag.LOGGER.info(message.getString());
        }
    }

    private static void sendTitleToAllPlaying(Component title) {
        ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(title);
        ClientboundSetTitlesAnimationPacket animPacket = new ClientboundSetTitlesAnimationPacket(10, 70, 20);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isEligibleRunner(player)) {
                player.connection.send(animPacket);
                player.connection.send(titlePacket);
            }
        }
    }

    private static void sendMessageToAllPlaying(Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isEligibleRunner(player)) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static String getPlayerName(UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        return player != null ? player.getName().getString() : id.toString();
    }
}
