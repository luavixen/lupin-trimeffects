package dev.foxgirl.trimeffects;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimPattern;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableInt;
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

    public void initialize(@NotNull Path directory) {
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

    public static @NotNull RegistryKey<StatusEffect> getRegistryKey(@NotNull StatusEffect effect) {
        return getRegistryKey(effect.getRegistryEntry());
    }

    public static @Nullable ArmorTrim getArmorTrim(@NotNull DynamicRegistryManager manager, @NotNull ItemStack stack) {
        // return stack.get(DataComponentTypes.TRIM);
        var nbt = stack.getSubNbt("Trim");
        if (nbt == null || !stack.isIn(ItemTags.TRIMMABLE_ARMOR)) return null;
        return ArmorTrim.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, manager), nbt).result().orElse(null);
    }

    public interface DurationSetter {
        void trimeffects$setDuration(int duration);
    }

    public static void setDuration(@NotNull StatusEffectInstance instance, int duration) {
        ((DurationSetter) instance).trimeffects$setDuration(duration);
    }

    private final Map<Identifier, Identifier[]> patternToEffectsCache = new HashMap<>();

    private @Nullable Set<StatusEffect> lookupEffectsForPatternIDInCache(Registry<StatusEffect> registry, Identifier patternID) {
        var effectIDs = patternToEffectsCache.get(patternID);
        if (effectIDs == null) return null;

        var effects = new StatusEffect[effectIDs.length];

        for (int i = 0, length = effects.length; i < length; i++) {
            var effect = registry.get(effectIDs[i]);
            if (effect != null) {
                effects[i] = effect;
            } else {
                patternToEffectsCache.remove(patternID);
                return null;
            }
        }

        return new ObjectArraySet<>(effects);
    }

    private static final Identifier[] EMPTY_EFFECT_IDS = new Identifier[0];

    private void storeEffectsForPatternIDInCache(Identifier patternID, Set<Identifier> effectIDs) {
        patternToEffectsCache.put(patternID, effectIDs.toArray(EMPTY_EFFECT_IDS));
    }

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

    private Set<StatusEffect> findEffectsForPatternID(Registry<StatusEffect> registry, Identifier patternID) {
        var cachedEffects = lookupEffectsForPatternIDInCache(registry, patternID);
        if (cachedEffects != null) return cachedEffects;

        String patternPathString = patternID.getPath();
        String patternFullString = patternID.toString();

        for (var entry : config.effects.entrySet()) {
            String key = entry.getKey();

            if (
                patternPathString.equalsIgnoreCase(key) ||
                patternFullString.equalsIgnoreCase(key)
            ) {
                List<String> effectStrings = entry.getValue();
                if (effectStrings == null || effectStrings.isEmpty()) {
                    return ImmutableSet.of();
                }

                final int size = effectStrings.size();
                boolean invalid = false;

                var effectIDs = new ObjectArraySet<Identifier>(new Identifier[size], 0);
                var effects = new StatusEffect[size];

                for (String effectString : effectStrings) {
                    var effectID = Identifier.tryParse(effectString);
                    if (effectID == null) {
                        warnInvalidEffect(effectString);
                        invalid = true;
                        continue;
                    }

                    var effect = registry.get(effectID);
                    if (effect == null) {
                        warnInvalidEffect(effectString);
                        invalid = true;
                        continue;
                    }

                    if (effectIDs.add(effectID)) {
                        effects[effectIDs.size() - 1] = effect;
                    }
                }

                if (!invalid) {
                    storeEffectsForPatternIDInCache(patternID, effectIDs);
                }

                return new ObjectArraySet<>(effects, effectIDs.size());
            }
        }

        warnMissingPattern(patternFullString);
        return ImmutableSet.of();
    }

    public record TrimDetails(
        ArmorTrim trim,
        RegistryKey<ArmorTrimMaterial> materialKey,
        RegistryKey<ArmorTrimPattern> patternKey,
        Set<StatusEffect> effects
    ) {
        public TrimDetails(
            ArmorTrim trim,
            RegistryKey<ArmorTrimMaterial> materialKey,
            RegistryKey<ArmorTrimPattern> patternKey,
            StatusEffect effect
        ) {
            this(trim, materialKey, patternKey, ImmutableSet.of(effect));
        }
    }

    public @Nullable TrimDetails createTrimDetails(@NotNull ArmorTrim trim, @NotNull Registry<StatusEffect> effectRegistry) {
        RegistryKey<ArmorTrimMaterial> materialKey = getRegistryKey(trim.getMaterial());
        RegistryKey<ArmorTrimPattern> patternKey = getRegistryKey(trim.getPattern());

        if (config.resinGivesNightVision && materialKey.getValue().getPath().contains("resin")) {
            return new TrimDetails(trim, materialKey, patternKey, StatusEffects.NIGHT_VISION);
        }

        var effects = findEffectsForPatternID(effectRegistry, patternKey.getValue());
        if (effects.isEmpty()) return null;

        return new TrimDetails(trim, materialKey, patternKey, effects);
    }

    public void onLivingEntityTick(@NotNull LivingEntity entity) {
        if (!config.applyToMobs && !(entity instanceof PlayerEntity)) return;

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
        public final StatusEffect effect;
        public final RegistryKey<StatusEffect> effectKey;

        public int index;

        public EffectDetails(StatusEffect effect, RegistryKey<StatusEffect> effectKey) {
            this.effect = effect;
            this.effectKey = effectKey;
        }

        public void incrementIndex() {
            index++;
        }

        private int getMatchingEffectLevel() {
            var matchingEffectLevels = TrimEffects2.INSTANCE.config.matchingEffectLevels;
            if (matchingEffectLevels == null || matchingEffectLevels.isEmpty()) return 1;

            if (index < 0) return matchingEffectLevels.get(0);
            if (index >= matchingEffectLevels.size()) return matchingEffectLevels.get(matchingEffectLevels.size() - 1);

            return matchingEffectLevels.get(index);
        }

        public int getAmplifier() {
            return Math.max(getMatchingEffectLevel() - 1, 0);
        }

        public StatusEffectInstance createStatusEffectInstance() {
            return new StatusEffectInstance(effect, STATUS_EFFECT_DURATION_MARKER, getAmplifier());
        }
    }

    private List<EffectDetails> collectEffectDetails(List<TrimDetails> trims) {
        ArrayList<EffectDetails> effects = new ArrayList<>(trims.size());

        for (TrimDetails trimDetails : trims) {
            for (StatusEffect effect : trimDetails.effects()) {
                RegistryKey<StatusEffect> effectKey = getRegistryKey(effect);

                boolean exists = false;

                for (EffectDetails effectDetails : effects) {
                    if (effectDetails.effectKey.equals(effectKey)) {
                        effectDetails.incrementIndex();
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    effects.add(new EffectDetails(effect, effectKey));
                }
            }
        }

        return effects;
    }

    private final Map<UUID, AbsorptionRecord> absorptionRecords = new HashMap<>();

    private static final class AbsorptionRecord {
        private float previousAmount;
        private int stunTicks;

        private AbsorptionRecord(LivingEntity entity) {
            this.previousAmount = entity.getAbsorptionAmount();
            this.stunTicks = 0;
        }

        private void setStunTicks(int ticks) {
            stunTicks = ticks;
        }
        private void decrementStunTicks() {
            --stunTicks;
        }
    }

    private boolean isAbsorption(StatusEffect effect) {
        return StatusEffects.ABSORPTION.equals(effect);
    }

    private boolean isAbsorptionStunned(LivingEntity entity, StatusEffectInstance instance) {
        var absorptionRecord = absorptionRecords.computeIfAbsent(entity.getUuid(), uuid -> new AbsorptionRecord(entity));

        float currentAbsorptionAmount = entity.getAbsorptionAmount();
        float previousAbsorptionAmount = absorptionRecord.previousAmount;
        absorptionRecord.previousAmount = currentAbsorptionAmount;

        if (absorptionRecord.stunTicks > 0) {
            absorptionRecord.decrementStunTicks();
            return true;
        }

        if (
            instance != null && instance.isInfinite() &&
            currentAbsorptionAmount < previousAbsorptionAmount
        ) {
            int stunDurationInTicks = (int) (config.absorptionStunSeconds * 20.0);
            absorptionRecord.setStunTicks(stunDurationInTicks);
            setDuration(instance, stunDurationInTicks);
            return true;
        }

        return false;
    }

    private void updateEffectsWithTrimDetails(LivingEntity entity, List<TrimDetails> trims) {
        List<EffectDetails> effects = collectEffectDetails(trims);

        for (EffectDetails effectDetails : effects) {
            var instance = entity.getStatusEffect(effectDetails.effect);

            if (
                isAbsorption(effectDetails.effect) &&
                isAbsorptionStunned(entity, instance)
            ) continue;

            if (instance == null) {
                entity.addStatusEffect(effectDetails.createStatusEffectInstance(), entity);
            } else {
                if (instance.getDuration() == STATUS_EFFECT_DURATION_MARKER) {
                    if (instance.getAmplifier() == effectDetails.getAmplifier()) {
                        continue;
                    }
                } else {
                    if (instance.getAmplifier() >= effectDetails.getAmplifier()) {
                        continue;
                    }
                }
                entity.removeStatusEffect(effectDetails.effect);
                entity.addStatusEffect(effectDetails.createStatusEffectInstance(), entity);
            }
        }

        removeEffectsFromTrims(entity, effects);
    }

    private boolean isEffectExcluded(List<EffectDetails> excludedEffects, StatusEffect effect) {
        RegistryKey<StatusEffect> effectKey = getRegistryKey(effect);
        for (EffectDetails effectDetails : excludedEffects) {
            if (effectKey.equals(effectDetails.effectKey)) {
                return true;
            }
        }
        return false;
    }

    private void removeEffectsFromTrims(LivingEntity entity, @Nullable List<EffectDetails> excludedEffects) {
        ObjectArraySet<StatusEffect> effectsToRemove = null;

        for (StatusEffectInstance instance : entity.getStatusEffects()) {
            if (instance.getDuration() == STATUS_EFFECT_DURATION_MARKER) {
                var effect = instance.getEffectType();

                if (excludedEffects != null && isEffectExcluded(excludedEffects, effect)) {
                    continue;
                }

                if (effectsToRemove == null) {
                    effectsToRemove = new ObjectArraySet<>(2);
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
