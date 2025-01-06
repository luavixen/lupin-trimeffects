package dev.foxgirl.trimeffects;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimPattern;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.nio.file.Path;

public final class TrimEffects2 {

    public static final Logger LOGGER = LogManager.getLogger("trimeffects");

    public static final TrimEffects2 INSTANCE = new TrimEffects2();

    public static final int STATUS_EFFECT_DURATION_MARKER = -75915;

    public Config config;

    public void initialize(Path directory) {
        config = Config.read(directory);
    }

    public static @NotNull DynamicRegistryManager getRegistryManager(@NotNull Entity entity) {
        return entity.getWorld().getRegistryManager();
    }

    public static @NotNull Registry<StatusEffect> getStatusEffectRegistry(@NotNull DynamicRegistryManager manager) {
        return manager.get(RegistryKeys.STATUS_EFFECT);
    }
    public static Registry<StatusEffect> getStatusEffectRegistry(@NotNull Entity entity) {
        return getStatusEffectRegistry(getRegistryManager(entity));
    }

    public static <T> @NotNull RegistryKey<T> getRegistryKey(@NotNull RegistryEntry<T> entry) {
        return entry.getKey().orElseThrow();
    }

    public static @Nullable ArmorTrim getArmorTrim(@NotNull DynamicRegistryManager manager, @NotNull ItemStack stack) {
        return stack.get(DataComponentTypes.TRIM);
        /*
        var nbt = stack.getSubNbt("Trim");
        if (nbt == null || !stack.isIn(ItemTags.TRIMMABLE_ARMOR)) return null;
        return ArmorTrim.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, manager), nbt).result().orElse(null);
        */
    }

    public static @NotNull RegistryEntry<ArmorTrimMaterial> getArmorTrimMaterial(@NotNull ArmorTrim trim) {
        return ((ArmorTrimProxy) trim).trimeffects$getMaterial();
    }
    public static @NotNull RegistryEntry<ArmorTrimPattern> getArmorTrimPattern(@NotNull ArmorTrim trim) {
        return ((ArmorTrimProxy) trim).trimeffects$getPattern();
    }

    private final Map<Identifier, Identifier> patternToEffectCache = new HashMap<>();

    private final Set<String> missingPatterns = new HashSet<>();
    private final Set<String> invalidEffects = new HashSet<>();

    private void warnMissingPattern(String patternString) {
        if (missingPatterns.add(patternString)) {
            LOGGER.warn("(TrimsEffects) Config is missing an effect for trim pattern \"{}\", consider adding it!", patternString);
        }
    }
    private void warnInvalidEffect(String effectString) {
        if (invalidEffects.add(effectString)) {
            LOGGER.warn("(TrimsEffects) Config has an unknown/invalid effect \"{}\", check your spelling", effectString);
        }
    }

    private @Nullable RegistryEntry<StatusEffect> findEffectForPatternID(Registry<StatusEffect> registry, Identifier patternID) {
        var effectCachedID = patternToEffectCache.get(patternID);
        if (effectCachedID != null) {
            var effect = registry.getEntry(effectCachedID);
            if (effect.isPresent()) {
                return effect.get();
            } else {
                patternToEffectCache.remove(patternID);
            }
        }

        String patternPathString = patternID.getPath();
        String patternFullString = patternID.toString();

        for (var entry : config.effects.entrySet()) {
            String key = entry.getKey();
            if (
                patternPathString.equalsIgnoreCase(key) ||
                patternFullString.equalsIgnoreCase(key)
            ) {
                var effectID = Identifier.tryParse(entry.getValue());
                if (effectID == null) {
                    warnInvalidEffect(entry.getValue());
                    return null;
                }

                var effect = registry.getEntry(effectID);
                if (effect.isEmpty()) {
                    warnInvalidEffect(entry.getValue());
                    return null;
                }

                patternToEffectCache.put(patternID, effectID);

                return effect.get();
            }
        }

        warnMissingPattern(patternFullString);
        return null;
    }

    public record TrimDetails(
        ArmorTrim trim,
        RegistryKey<ArmorTrimMaterial> materialKey,
        RegistryKey<ArmorTrimPattern> patternKey,
        RegistryEntry<StatusEffect> effect,
        RegistryKey<StatusEffect> effectKey
    ) {
        public TrimDetails(
            ArmorTrim trim,
            RegistryKey<ArmorTrimMaterial> materialKey,
            RegistryKey<ArmorTrimPattern> patternKey,
            RegistryEntry<StatusEffect> effect
        ) {
            this(trim, materialKey, patternKey, effect, getRegistryKey(effect));
        }
    }

    public @Nullable TrimDetails createTrimDetails(@NotNull ArmorTrim trim, @NotNull Registry<StatusEffect> effectRegistry) {
        RegistryKey<ArmorTrimMaterial> materialKey = getRegistryKey(getArmorTrimMaterial(trim));
        RegistryKey<ArmorTrimPattern> patternKey = getRegistryKey(getArmorTrimPattern(trim));

        if (config.resinGivesNightVision && materialKey.getValue().getPath().contains("resin")) {
            return new TrimDetails(trim, materialKey, patternKey, StatusEffects.NIGHT_VISION);
        }

        var effect = findEffectForPatternID(effectRegistry, patternKey.getValue());
        if (effect == null) return null;

        return new TrimDetails(trim, materialKey, patternKey, effect);
    }

    public void onLivingEntityTick(@NotNull LivingEntity entity) {
        DynamicRegistryManager manager = getRegistryManager(entity);

        Registry<StatusEffect> registry = null;
        ArrayList<TrimDetails> trims = null;

        for (ItemStack stack : entity.getArmorItems()) {
            var trim = getArmorTrim(manager, stack);
            if (trim != null) {
                if (registry == null) {
                    registry = getStatusEffectRegistry(manager);
                }

                var details = createTrimDetails(trim, registry);
                if (details == null) continue;

                if (trims == null) {
                    trims = new ArrayList<>(4);
                }

                trims.add(details);
            }
        }

        if (trims != null && !trims.isEmpty()) {
            updateEffectsWithTrimDetails(entity, trims);
        } else {
            removeEffectsFromTrims(entity, null);
        }
    }

    private static final class EffectDetails {
        public final RegistryEntry<StatusEffect> effect;
        public final RegistryKey<StatusEffect> effectKey;

        public int amplifier;

        public EffectDetails(RegistryEntry<StatusEffect> effect) {
            this.effect = effect;
            this.effectKey = getRegistryKey(effect);
        }

        public void increaseAmplifier(int maximumLevel) {
            amplifier = Math.min(amplifier + 1, maximumLevel - 1);
        }

        public StatusEffectInstance createStatusEffectInstance() {
            return new StatusEffectInstance(effect, STATUS_EFFECT_DURATION_MARKER, amplifier);
        }
    }

    private List<EffectDetails> collectEffectDetails(List<TrimDetails> trims) {
        ArrayList<EffectDetails> effects = new ArrayList<>(trims.size());

        for (TrimDetails trimDetails : trims) {
            boolean exists = false;

            for (EffectDetails effectDetails : effects) {
                if (effectDetails.effectKey.equals(trimDetails.effectKey())) {
                    effectDetails.increaseAmplifier(config.maxEffectLevel);
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                effects.add(new EffectDetails(trimDetails.effect()));
            }
        }

        return effects;
    }

    private void updateEffectsWithTrimDetails(LivingEntity entity, List<TrimDetails> trims) {
        List<EffectDetails> effects = collectEffectDetails(trims);

        for (EffectDetails effectDetails : effects) {
            var instance = entity.getStatusEffect(effectDetails.effect);
            if (instance == null) {
                entity.addStatusEffect(effectDetails.createStatusEffectInstance(), entity);
            } else {
                if (instance.getDuration() == STATUS_EFFECT_DURATION_MARKER) {
                    if (instance.getAmplifier() == effectDetails.amplifier) {
                        continue;
                    }
                } else {
                    if (instance.getAmplifier() >= effectDetails.amplifier) {
                        continue;
                    }
                }
                entity.removeStatusEffect(effectDetails.effect);
                entity.addStatusEffect(effectDetails.createStatusEffectInstance(), entity);
            }
        }

        removeEffectsFromTrims(entity, effects);
    }

    private boolean isEffectExcluded(List<EffectDetails> excludedEffects, RegistryEntry<StatusEffect> effect) {
        RegistryKey<StatusEffect> effectKey = getRegistryKey(effect);
        for (EffectDetails effectDetails : excludedEffects) {
            if (effectKey.equals(effectDetails.effectKey)) {
                return true;
            }
        }
        return false;
    }

    private void removeEffectsFromTrims(LivingEntity entity, @Nullable List<EffectDetails> excludedEffects) {
        ObjectArraySet<RegistryEntry<StatusEffect>> effectsToRemove = null;

        for (StatusEffectInstance instance : entity.getStatusEffects()) {
            if (instance.getDuration() == STATUS_EFFECT_DURATION_MARKER) {
                var effect = instance.getEffectType();

                if (excludedEffects != null && isEffectExcluded(excludedEffects, effect)) {
                    continue;
                }

                if (effectsToRemove == null) {
                    effectsToRemove = new ObjectArraySet<>();
                }

                effectsToRemove.add(effect);
            }
        }

        if (effectsToRemove != null) {
            for (var effect : effectsToRemove) {
                entity.removeStatusEffect(effect);
            }
        }
    }

}
