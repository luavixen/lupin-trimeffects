package dev.foxgirl.trimeffects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public final class Config {

    public Map<String, List<String>> effects;

    public boolean applyToMobs;

    public List<Integer> matchingEffectLevels;

    public double absorptionStunSeconds;
    public boolean resinGivesNightVision;

    public static @NotNull String DEFAULT =
        """
        // TrimsEffects v2 config JSON file
        {

          // Mapping of armor trim patterns to their associated potion effects
          "effects": {
            // Simple example:
            // "rib": ["strength"],
            // Multiple effects example:
            // "dune": ["speed", "jump_boost"],
            // Fully-qualified identifiers example:
            // "minecraft:eye": ["minecraft:invisibility"]",
            // Custom modded patterns and effects example:
            // "fancymod:fancytrim": ["fancymod:fancyeffect"]",

            // Default values:
            "sentry": ["resistance"],
            "dune": ["speed"],
            "coast": ["water_breathing"],
            "wild": ["hero_of_the_village"],
            "ward": ["absorption"],
            "eye": ["regeneration"],
            "vex": ["invisibility"],
            "tide": ["conduit_power"],
            "snout": ["fire_resistance"],
            "rib": ["haste"],
            "spire": ["strength"],
            "wayfinder": ["slow_falling"],
            "shaper": ["luck"],
            "silence": ["health_boost"],
            "raiser": ["saturation"],
            "host": ["glowing"],
            "flow": ["jump_boost"],
            "bolt": ["dolphins_grace"]
          },

          // Whether mobs wearing trimmed armor should get the same
          // status effects as players, default true
          "applyToMobs": true,

          // Association between number of matching status effects per
          // armor piece / armor trim to what level the status effect
          // should have, default [1, 1, 1, 2]
          "matchingEffectLevels": [1, 1, 1, 2],

          // How long a player needs to wait for absorption to refresh,
          // in seconds, default 12.0
          "absorptionStunSeconds": 12.0,

          // Whether resin gives night vision, default true
          "resinGivesNightVision": true

        }
        """;

    private static final Gson GSON =
        new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .setPrettyPrinting()
            .setLenient()
            .create();

    public static @NotNull Config read(@NotNull Path directory) {
        Path filePath = directory.resolve("trimseffects-config2.json");
        Path tempPath = directory.resolve("trimseffects-config2.json.tmp");

        try (var reader = Files.newBufferedReader(filePath)) {
            return GSON.fromJson(reader, Config.class);
        } catch (NoSuchFileException cause) {
            TrimEffects2.LOGGER.warn("(TrimsEffects) Config file not found: {}", filePath);
            writeDefaultConfig(filePath, tempPath);
        } catch (JsonParseException cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Config file is invalid! {}", cause.getMessage());
        } catch (IOException cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to read config file, IO error", cause);
        } catch (Exception cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to read config file", cause);
        }

        TrimEffects2.LOGGER.warn("(TrimsEffects) Using default config values");

        return GSON.fromJson(DEFAULT, Config.class);
    }

    private static void writeDefaultConfig(@NotNull Path filePath, @NotNull Path tempPath) {
        try {
            Files.writeString(tempPath, DEFAULT);
            Files.move(tempPath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to write default config file, IO error", cause);
        } catch (Exception cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to write default config file", cause);
        }
    }

}
