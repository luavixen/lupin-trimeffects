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
import java.util.Map;

public final class Config {

    public Map<String, String> effects;

    public int maxEffectLevel;
    public double absorptionStunSeconds;
    public boolean resinGivesNightVision;

    public static @NotNull String DEFAULT =
        """
        {
          "effects": {
            "sentry": "resistance",
            "dune": "speed",
            "coast": "water_breathing",
            "wild": "hero_of_the_village",
            "ward": "absorption",
            "eye": "regeneration",
            "vex": "invisibility",
            "tide": "conduit_power",
            "snout": "fire_resistance",
            "rib": "haste",
            "spire": "strength",
            "wayfinder": "slow_falling",
            "shaper": "luck",
            "silence": "health_boost",
            "raiser": "saturation",
            "host": "glowing",
            "flow": "jump_boost",
            "bolt": "dolphins_grace"
          },
          "maxEffectLevel": 2,
          "absorptionStunSeconds": 12.0,
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

        try {
            return GSON.fromJson(Files.newBufferedReader(filePath), Config.class);
        } catch (NoSuchFileException cause) {
            TrimEffects2.LOGGER.warn("(TrimsEffects) Failed to read config, file not found: {}", filePath);
        } catch (IOException cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to read config, IO error", cause);
        } catch (JsonParseException cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to read config, JSON error", cause);
        } catch (Exception cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to read config", cause);
        }

        try {
            Files.writeString(tempPath, DEFAULT);
            Files.move(tempPath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to write new config, IO error", cause);
        } catch (Exception cause) {
            TrimEffects2.LOGGER.error("(TrimsEffects) Failed to write new config", cause);
        }

        return GSON.fromJson(DEFAULT, Config.class);
    }

}
