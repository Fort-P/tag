package org.fortp.tag.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fortp.tag.Tag;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final int CURRENT_CONFIG_VERSION = 1;
    private static Config config;

    private ConfigManager() {
    }

    public static void load(Path path) {
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonElement json = JsonParser.parseReader(reader);
                    JsonObject configJson = json != null && json.isJsonObject()
                            ? json.getAsJsonObject()
                            : new JsonObject();

                    config = gson.fromJson(configJson, Config.class);
                    boolean normalized = normalizeConfig(config);

                    if (normalized) {
                        save(path);
                    }
                }
            } else {
                Tag.LOGGER.info("Writing new config file");
                config = new Config();
                save(path);
            }
        } catch (IOException e) {
            Tag.LOGGER.error("An error occurred while loading config, using default config", e);
            config = new Config();
        }
    }

    public static void save(Path path) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            Tag.LOGGER.error("An error occurred while saving config", e);
        }
    }

    public static Config get() {
        if (config == null) {
            throw new IllegalStateException("Config has not been loaded yet!");
        }
        return config;
    }

    private static boolean normalizeConfig(Config config) {
        boolean changed = false;

        if (config.getConfigVersion() != CURRENT_CONFIG_VERSION) {
            config.setConfigVersion(CURRENT_CONFIG_VERSION);
            changed = true;
        }

        if (config.getWarningSeconds() <= 0) {
            config.setWarningSeconds(300);
            changed = true;
        }

        if (config.getBreakawaySeconds() <= 0) {
            config.setBreakawaySeconds(15);
            changed = true;
        }

        if (config.getCampingSeconds() <= 0) {
            config.setCampingSeconds(60);
            changed = true;
        }

        if (config.getMaxGameDurationSeconds() <= 0) {
            config.setMaxGameDurationSeconds(600);
            changed = true;
        }

        if (config.getCampingChunkRadius() < 0) {
            config.setCampingChunkRadius(1);
            changed = true;
        }

        if (config.getScoreIntervalSeconds() <= 0) {
            config.setScoreIntervalSeconds(60);
            changed = true;
        }

        if (config.getBasePointsPerInterval() < 0) {
            config.setBasePointsPerInterval(10);
            changed = true;
        }

        if (config.getTagBounty() < 0) {
            config.setTagBounty(50);
            changed = true;
        }

        if (config.getForfeitPenalty() > 0) {
            config.setForfeitPenalty(-50);
            changed = true;
        }

        if (config.getPityIncrement() < 0) {
            config.setPityIncrement(0.5);
            changed = true;
        }

        if (config.getMobilityLockoutDistanceSqr() < 0) {
            config.setMobilityLockoutDistanceSqr(16384.0);
            changed = true;
        }

        return changed;
    }
}
