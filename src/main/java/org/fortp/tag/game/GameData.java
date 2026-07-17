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
    private Map<UUID, Double> runnerWeights = new HashMap<>();

    public GameData(Map<UUID, Integer> scores, Map<UUID, Double> runnerWeights) {
        this.scores = new HashMap<>(scores);
        this.runnerWeights = new HashMap<>(runnerWeights);
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

    private static final Codec<GameData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                    .fieldOf("scores")
                    .forGetter(data -> data.scores),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.DOUBLE)
                    .optionalFieldOf("runner_weights", new HashMap<>())
                    .forGetter(data -> data.runnerWeights)
    ).apply(instance, GameData::new)
    );

    private static final SavedDataType<GameData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Tag.MOD_ID, "gamedata"), () -> new GameData(new HashMap<>(), new HashMap<>()), CODEC, null
    );

    public static GameData getGameData(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
