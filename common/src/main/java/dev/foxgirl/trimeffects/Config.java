package dev.foxgirl.trimeffects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimPattern;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Config {

    public double secondsMaximum = 14.0;
    public double secondsMinimum = 12.0;

    public double absorptionStunSeconds = 10.0;

    public int minimumMatchingTrims = 4;
    public boolean enableCombinedEffects = false;

    public @NotNull Map<String, String> effects = new LinkedHashMap<>();
    public @NotNull Map<String, Integer> strengths = new LinkedHashMap<>();

    public Config() {}

    public @NotNull Parsed parse() {
        return new Parsed(this);
    }

    public static final class Parsed {

        private final double secondsMaximum;
        private final double secondsMinimum;

        private final double absorptionStunSeconds;

        private final int minimumMatchingTrims;
        private final boolean enableCombinedEffects;

        private final Map<RegistryKey<ArmorTrimPattern>, RegistryKey<StatusEffect>> effects = new LinkedHashMap<>();
        private final Map<RegistryKey<ArmorTrimMaterial>, Integer> strengths = new LinkedHashMap<>();

        private Parsed(@NotNull Config config) {
            secondsMaximum = config.secondsMaximum;
            secondsMinimum = config.secondsMinimum;
            absorptionStunSeconds = config.absorptionStunSeconds;
            minimumMatchingTrims = config.minimumMatchingTrims;
            enableCombinedEffects = config.enableCombinedEffects;
            for (var entry : config.effects.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (value == null || key == null) continue;
                effects.put(
                    RegistryKey.of(RegistryKeys.TRIM_PATTERN, new Identifier(key)),
                    RegistryKey.of(RegistryKeys.STATUS_EFFECT, new Identifier(value))
                );
            }
            for (var entry : config.strengths.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                if (value == null || key == null) continue;
                strengths.put(RegistryKey.of(RegistryKeys.TRIM_MATERIAL, new Identifier(key)), value);
            }
        }

        public double getSecondsMaximum() {
            return secondsMaximum;
        }
        public double getSecondsMinimum() {
            return secondsMinimum;
        }

        public double getAbsorptionStunSeconds() {
            return absorptionStunSeconds;
        }

        public int getMinimumMatchingTrims() {
            return minimumMatchingTrims;
        }
        public boolean isEnableCombinedEffects() {
            return enableCombinedEffects;
        }

        public @NotNull Map<RegistryKey<ArmorTrimPattern>, RegistryKey<StatusEffect>> getEffects() {
            return effects;
        }
        public @NotNull Map<RegistryKey<ArmorTrimMaterial>, Integer> getStrengths() {
            return strengths;
        }

    }

    private static final Config DEFAULT = new Config();
    static {
        DEFAULT.effects.put("spire", "strength");
        DEFAULT.effects.put("eye", "regeneration");
        DEFAULT.effects.put("snout", "fire_resistance");
        DEFAULT.effects.put("rib", "haste");
        DEFAULT.effects.put("vex", "invisibility");
        DEFAULT.effects.put("ward", "absorption");
        DEFAULT.effects.put("tide", "luck");
        DEFAULT.effects.put("wild", "hero_of_the_village");
        DEFAULT.effects.put("coast", "water_breathing");
        DEFAULT.effects.put("dune", "speed");
        DEFAULT.effects.put("sentry", "resistance");
        DEFAULT.effects.put("wayfinder", "slow_falling");
        DEFAULT.effects.put("shaper", "saturation");
        DEFAULT.effects.put("silence", "night_vision");
        DEFAULT.effects.put("raiser", "saturation");
        DEFAULT.effects.put("host", "glowing");
        DEFAULT.effects.put("flow", "jump_boost");
        DEFAULT.effects.put("bolt", "strength");
        DEFAULT.strengths.put("copper", 0);
        DEFAULT.strengths.put("iron", 0);
        DEFAULT.strengths.put("redstone", 0);
        DEFAULT.strengths.put("lapis", 0);
        DEFAULT.strengths.put("quartz", 0);
        DEFAULT.strengths.put("gold", 0);
        DEFAULT.strengths.put("emerald", 0);
        DEFAULT.strengths.put("amethyst", 0);
        DEFAULT.strengths.put("diamond", 1);
        DEFAULT.strengths.put("netherite", 2);
    }

    private static final Gson GSON =
        new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .setPrettyPrinting()
            .setLenient()
            .create();

    public static @NotNull Config read(@NotNull Path configDirectory) {
        Objects.requireNonNull(configDirectory, "Argument 'configDirectory'");

        Path filePath = configDirectory.resolve("trimeffects-config.json");
        Path tempPath = configDirectory.resolve("trimeffects-config.json.tmp");

        try {
            return GSON.fromJson(Files.newBufferedReader(filePath), Config.class);
        } catch (NoSuchFileException cause) {
            TrimEffects.LOGGER.error("Failed to read config, file not found");
        } catch (IOException cause) {
            TrimEffects.LOGGER.error("Failed to read config, IO error", cause);
        } catch (JsonParseException cause) {
            TrimEffects.LOGGER.error("Failed to read config, JSON error", cause);
        } catch (Exception cause) {
            TrimEffects.LOGGER.error("Failed to read config", cause);
        }

        try {
            Files.writeString(tempPath, GSON.toJson(DEFAULT));
            Files.move(tempPath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException cause) {
            TrimEffects.LOGGER.error("Failed to write new config, IO error", cause);
        } catch (Exception cause) {
            TrimEffects.LOGGER.error("Failed to write new config", cause);
        }

        return DEFAULT;
    }

}
