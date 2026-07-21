package org.fortp.tag.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.fortp.tag.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class GameData extends SavedData {
    private Map<UUID, Integer> scores = new HashMap<>();
    private Map<UUID, Integer> tags = new HashMap<>();
    private Map<UUID, Integer> highScores = new HashMap<>();
    private Map<UUID, Integer> taggerScores = new HashMap<>();
    private Map<UUID, Integer> runnerScores = new HashMap<>();
    private Map<UUID, Integer> taggerTimes = new HashMap<>();
    private Map<UUID, Integer> runnerTimes = new HashMap<>();
    private Map<UUID, Double> runnerWeights = new HashMap<>();

    public GameData(Map<UUID, Integer> scores, Map<UUID, Double> runnerWeights, Map<UUID, Integer> tags, Map<UUID, Integer> highScores, Map<UUID, Integer> taggerScores, Map<UUID, Integer> runnerScores, Map<UUID, Integer> taggerTimes, Map<UUID, Integer> runnerTimes) {
        this.scores = new HashMap<>(scores);
        this.runnerWeights = new HashMap<>(runnerWeights);
        this.tags = new HashMap<>(tags);
        this.highScores = new HashMap<>(highScores);
        this.taggerScores = new HashMap<>(taggerScores);
        this.runnerScores = new HashMap<>(runnerScores);
        this.taggerTimes = new HashMap<>(taggerTimes);
        this.runnerTimes = new HashMap<>(runnerTimes);
    }

    public Map<UUID, Integer> getScores() {
        return this.scores;
    }

    public void addScore(UUID playerUUID, int score) {
        this.scores.put(playerUUID, this.scores.getOrDefault(playerUUID, 0) + score);
        setDirty();
    }

    public double getRunnerWeight(UUID playerUUID) {
        return this.runnerWeights.getOrDefault(playerUUID, 1.0);
    }

    public void setRunnerWeight(UUID playerUUID, double weight) {
        this.runnerWeights.put(playerUUID, Math.max(1.0, weight));
        setDirty();
    }

    public void addRunnerWeight(UUID playerUUID, double amount) {
        setRunnerWeight(playerUUID, getRunnerWeight(playerUUID) + amount);
    }

    public int getTags(UUID uuid) {
        return this.tags.getOrDefault(uuid, 0);
    }

    public void addTag(UUID uuid) {
        this.tags.put(uuid, this.tags.getOrDefault(uuid, 0) + 1);
        setDirty();
    }

    public int getHighScore(UUID uuid) {
        return this.highScores.getOrDefault(uuid, 0);
    }

    public void setHighScore(UUID uuid, int score) {
        this.highScores.put(uuid, score);
        setDirty();
    }

    public int getTaggerScore(UUID uuid) {
        return this.taggerScores.getOrDefault(uuid, 0);
    }

    public void addTaggerScore(UUID uuid, int score) {
        this.taggerScores.put(uuid, this.taggerScores.getOrDefault(uuid, 0) + score);
        setDirty();
    }

    public int getRunnerScore(UUID uuid) {
        return this.runnerScores.getOrDefault(uuid, 0);
    }

    public void addRunnerScore(UUID uuid, int score) {
        this.runnerScores.put(uuid, this.runnerScores.getOrDefault(uuid, 0) + score);
        setDirty();
    }

    public int getTaggerTimes(UUID uuid) {
        return this.taggerTimes.getOrDefault(uuid, 0);
    }

    public void addTaggerTime(UUID uuid) {
        this.taggerTimes.put(uuid, this.taggerTimes.getOrDefault(uuid, 0) + 1);
        setDirty();
    }

    public void removeTaggerTime(UUID uuid) {
        this.taggerTimes.put(uuid, this.taggerTimes.getOrDefault(uuid, 0) - 1);
        setDirty();
    }

    public int getRunnerTimes(UUID uuid) {
        return this.runnerTimes.getOrDefault(uuid, 0);
    }

    public void addRunnerTime(UUID uuid) {
        this.runnerTimes.put(uuid, this.runnerTimes.getOrDefault(uuid, 0) + 1);
        setDirty();
    }

    private static final Codec<GameData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                            .optionalFieldOf("scores", new HashMap<>())
                            .forGetter(data -> data.scores),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.DOUBLE)
                            .optionalFieldOf("runner_weights", new HashMap<>())
                            .forGetter(data -> data.runnerWeights),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                            .optionalFieldOf("tags", new HashMap<>())
                            .forGetter(data -> data.tags),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                            .optionalFieldOf("highscores", new HashMap<>())
                            .forGetter(data -> data.highScores),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                            .optionalFieldOf("tagger_score", new HashMap<>())
                            .forGetter(data -> data.taggerScores),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                            .optionalFieldOf("runner_score", new HashMap<>())
                            .forGetter(data -> data.runnerScores),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                            .optionalFieldOf("tagger_times", new HashMap<>())
                            .forGetter(data -> data.taggerTimes),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                            .optionalFieldOf("runner_times", new HashMap<>())
                            .forGetter(data -> data.runnerTimes)
            ).apply(instance, GameData::new)
    );

    private static final SavedDataType<GameData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Tag.MOD_ID, "gamedata"), () -> new GameData(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>()), CODEC, null
    );

    public static GameData getGameData(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
