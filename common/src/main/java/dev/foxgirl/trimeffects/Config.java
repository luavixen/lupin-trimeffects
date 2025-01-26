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

    public List<Integer> matchingEffectLevels;

    public Map<String, Integer> materialEffectLevels;
    public int materialEffectLevelsMinimumMatching;

    public Map<String, List<String>> materialEffectOverrides;

    public boolean applyToMobs;

    public double absorptionStunSeconds;

    public static @NotNull String DEFAULT =
        """
        // TrimsEffects v2.1.X configuration JSON file
        // Make sure that your config file version matches the mod version!

        // CurseForge: https://www.curseforge.com/minecraft/mc-mods/trimseffects
        // Modrinth: https://modrinth.com/mod/trimseffects

        // === JSON EDITING TIPS ===
        // - Always use quotes around both keys and values: "key": "value"
        // - Don't put commas after the last item in a list or object:
        //   CORRECT:             WRONG:
        //   {                    {
        //     "a": "1",            "a": "1",
        //     "b": "2"             "b": "2",  <- Extra comma!
        //   }                    }
        // - Use square brackets [] for lists and curly braces {} for objects
        // - Keep all the original punctuation (colons, commas, brackets) when editing
        // =======================

        {

          // Maps armor trim patterns to their associated potion effects
          // Note: The "minecraft:" prefix is omitted from all pattern and effect names
          // For patterns/effects from other mods, include the full name (e.g., "othermod:custom_pattern")
          "effects": {
            // Format: "pattern": ["effect1", "effect2", ...]
            // You can specify multiple effects per pattern!

            // Example with multiple effects:
            // "wild": ["hero_of_the_village", "strength", "resistance"],

            // Example with a modded armor trim pattern and potion effect:
            // "othermod:fancy_pattern": ["othermod:fancy_effect"],

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

          // Controls potion effect level based on number of matching armor pieces
          // Format: [1 piece, 2 pieces, 3 pieces, 4 pieces]
          // Default: No effect for 1-2 pieces, level 1 for 3 pieces, level 2 for 4 pieces
          "matchingEffectLevels": [0, 0, 1, 2],

          // Legacy v1 feature: Override potion effect levels based on trim material
          // Note: The "minecraft:" prefix is omitted from material names
          // When enabled, gives effects even with just one armor piece!
          "materialEffectLevels": {
            // Format: "material": level
            // Uncomment to enable:
            /*
            "copper": 0,
            "iron": 0,
            "redstone": 0,
            "lapis": 0,
            "quartz": 0,
            "gold": 0,
            "emerald": 0,
            "amethyst": 0,
            "resin": 1,
            "diamond": 1,
            "netherite": 2
            */
          },

          // Legacy v1 feature: Minimum number of matching armor pieces required before an
          // effect is applied when using "materialEffectLevels"
          "materialEffectLevelsMinimumMatching": 4,

          // Override potion effects based on trim material instead of pattern
          // Note: The "minecraft:" prefix is omitted from material and effect names
          "materialEffectOverrides": {
            // Format: "material": ["effect1", "effect2", ...]

            // Example with multiple effects:
            // "gold": ["haste", "luck"],

            // Default values:
            "resin": ["night_vision"]
          },

          // If true, mobs wearing trimmed armor will get the effects
          // Disable this if your modpack already has powerful mobs
          "applyToMobs": true,

          // Prevents absorption effect from making players invincible
          // Waits this many seconds after damage before refreshing absorption hearts
          "absorptionStunSeconds": 12.0

        }
        """;

    public @NotNull Config verify() {
        if (effects == null)
            throw new IllegalStateException("Field 'effects' is missing");
        for (var entry : effects.entrySet()) {
            if (entry.getKey() == null)
                throw new IllegalStateException("Key in 'effects' map is missing/null");
            if (entry.getValue() == null)
                throw new IllegalStateException("Value in 'effects' map is missing/null");
            for (String effect : entry.getValue()) {
                if (effect == null)
                    throw new IllegalStateException("Value inside of 'effects' map is missing/null");
            }
        }

        if (matchingEffectLevels == null)
            throw new IllegalStateException("Field 'matchingEffectLevels' is missing");
        for (Integer level : matchingEffectLevels) {
            if (level == null)
                throw new IllegalStateException("Value in 'matchingEffectLevels' array is missing/null");
        }

        if (materialEffectLevels == null)
            throw new IllegalStateException("Field 'materialEffectLevels' is missing");
        for (var entry : materialEffectLevels.entrySet()) {
            if (entry.getKey() == null)
                throw new IllegalStateException("Key in 'materialEffectLevels' map is missing/null");
            if (entry.getValue() == null)
                throw new IllegalStateException("Value in 'materialEffectLevels' map is missing/null");
        }

        if (materialEffectOverrides == null)
            throw new IllegalStateException("Field 'materialEffectOverrides' is missing");
        for (var entry : materialEffectOverrides.entrySet()) {
            if (entry.getKey() == null)
                throw new IllegalStateException("Key in 'materialEffectOverrides' map is missing/null");
            if (entry.getValue() == null)
                throw new IllegalStateException("Value in 'materialEffectOverrides' map is missing/null");
            for (String effect : entry.getValue()) {
                if (effect == null)
                    throw new IllegalStateException("Value inside of 'materialEffectOverrides' map is missing/null");
            }
        }

        return this;
    }

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
            return GSON.fromJson(reader, Config.class).verify();
        } catch (NoSuchFileException cause) {
            TrimEffects2.LOGGER.warn("(TrimsEffects) Config file not found: {}", filePath);
            writeDefaultConfig(filePath, tempPath);
        } catch (IllegalStateException | JsonParseException cause) {
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
