package dev.foxgirl.trimeffects;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.nio.file.Path;

public final class TrimEffects2 {

    // region LOGGER and INSTANCE static fields

    public static final Logger LOGGER = LogManager.getLogger("trimeffects");
    public static final TrimEffects2 INSTANCE = new TrimEffects2();

    // endregion LOGGER and INSTANCE static fields

    public static final int STATUS_EFFECT_DURATION_MARKER = -75915;

    //region Class constructor/initialization function and config field

    public Config config;

    private TrimEffects2() {}

    public void initialize(@NotNull Path directory) {
        config = Config.read(directory);
    }

    //endregion TrimEffects2 constructor and config field

    //region Registry helper functions

    public static @NotNull DynamicRegistryManager getRegistryManager(@NotNull Entity entity) {
        return entity.getWorld().getRegistryManager();
    }

    public static @NotNull Registry<StatusEffect> getStatusEffectRegistry(@NotNull DynamicRegistryManager manager) {
        return manager.getOrThrow(RegistryKeys.STATUS_EFFECT);
    }
    public static Registry<StatusEffect> getStatusEffectRegistry(@NotNull Entity entity) {
        return getStatusEffectRegistry(getRegistryManager(entity));
    }

    public static <T> @NotNull RegistryKey<T> getRegistryKey(@NotNull RegistryEntry<T> entry) {
        return entry.getKey().orElseThrow();
    }

    @FunctionalInterface
    public interface RegistryEntryGetter<T> {
        @NotNull Optional<@NotNull RegistryEntry<T>> getEntry(@NotNull Identifier id);
    }

    public static <T> @NotNull RegistryEntryGetter<T> toRegistryEntryGetter(@NotNull Registry<T> registry) {
        return id -> {
            var entry = registry.getEntry(id);
            return entry.isPresent() ? Optional.of(entry.get()) : Optional.empty();
        };
    }
    public static <T> @NotNull RegistryEntryGetter<T> toRegistryEntryGetter(@NotNull RegistryWrapper<T> wrapper) {
        return id -> {
            RegistryKey<?> registryKey = ((RegistryWrapper.Impl<T>) wrapper).getKey();

            @SuppressWarnings("unchecked")
            RegistryKey<T> entryKey = RegistryKey.of((RegistryKey<Registry<T>>) registryKey, id);

            var entry = wrapper.getOptional(entryKey);
            return entry.isPresent() ? Optional.of(entry.get()) : Optional.empty();
        };
    }

    //endregion Registry helper functions

    //region ItemStack to ArmorTrim getter function

    public static @Nullable ArmorTrim getArmorTrimFromItemStack(@NotNull DynamicRegistryManager manager, @NotNull ItemStack stack) {

        // Implementation for 1.20.5 and up
        return stack.get(DataComponentTypes.TRIM);

        // Implementation for 1.20.4 and below
        /*
        var nbt = stack.getSubNbt("Trim");
        if (nbt == null || !stack.isIn(ItemTags.TRIMMABLE_ARMOR)) return null;
        return ArmorTrim.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, manager), nbt).result().orElse(null);
        */

    }

    //endregion ItemStack to ArmorTrim getter function

    //region Status effect duration setter function

    public interface DurationSetter {
        void trimeffects$setDuration(int duration);
    }

    public static void setStatusEffectDuration(@NotNull StatusEffectInstance instance, int duration) {
        ((DurationSetter) instance).trimeffects$setDuration(duration);
    }

    //endregion Status effect duration setter function

    //region IdentifierToEffectSetMapping implementation

    private static abstract class IdentifierToEffectSetMapping {

        private static final Identifier[] EMPTY_EFFECT_IDS = new Identifier[0];

        @SuppressWarnings("unchecked")
        private static RegistryEntry<StatusEffect>[] createStatusEffectArray(int length) {
            return (RegistryEntry<StatusEffect>[]) new RegistryEntry[length];
        }

        private final Map<Identifier, Identifier[]> keyToEffectsCache = new HashMap<>();

        private Set<RegistryEntry<StatusEffect>> lookupEffectsInCache(RegistryEntryGetter<StatusEffect> registry, Identifier key) {
            var effectIDs = keyToEffectsCache.get(key);
            if (effectIDs == null) return null;

            int length = effectIDs.length;
            if (length == 0) return ImmutableSet.of();

            var effects = createStatusEffectArray(length);

            for (int i = 0; i < length; i++) {
                var effect = registry.getEntry(effectIDs[i]);
                if (effect.isPresent()) {
                    effects[i] = effect.get();
                } else {
                    keyToEffectsCache.remove(key);
                    return null;
                }
            }

            return new ObjectArraySet<>(effects);
        }

        private void storeEffectsInCache(Identifier key, @Nullable Set<Identifier> effectIDs) {
            keyToEffectsCache.put(key, effectIDs != null ? effectIDs.toArray(EMPTY_EFFECT_IDS) : EMPTY_EFFECT_IDS);
        }

        private final Set<String> missingKeys = new HashSet<>();
        private final Set<String> invalidEffects = new HashSet<>();

        private void warnMissingKey(String keyString) {
            if (missingKeys.add(keyString)) {
                printWarningForMissingKey(keyString);
            }
        }
        private void warnInvalidEffect(String effectString) {
            if (invalidEffects.add(effectString)) {
                printWarningForInvalidEffect(effectString);
            }
        }

        protected abstract void printWarningForMissingKey(String keyString);
        protected abstract void printWarningForInvalidEffect(String effectString);

        protected abstract Map<String, List<String>> getUnderlyingConfigValue();

        public final @NotNull Set<@NotNull RegistryEntry<StatusEffect>> collectEffectsForKey(
            @NotNull RegistryEntryGetter<StatusEffect> registry,
            @NotNull Identifier key
        ) {
            var cachedEffects = lookupEffectsInCache(registry, key);
            if (cachedEffects != null) return cachedEffects;

            String keyPathString = key.getPath();
            String keyFullString = key.toString();

            for (var entry : getUnderlyingConfigValue().entrySet()) {
                String entryKeyString = entry.getKey();

                if (
                    keyPathString.equalsIgnoreCase(entryKeyString) ||
                    keyFullString.equalsIgnoreCase(entryKeyString)
                ) {
                    List<String> effectStrings = entry.getValue();
                    if (effectStrings == null) {
                        return ImmutableSet.of();
                    }
                    if (effectStrings.isEmpty()) {
                        storeEffectsInCache(key, null);
                        return ImmutableSet.of();
                    }

                    final int size = effectStrings.size();
                    boolean invalid = false;

                    var effectIDs = new ObjectArraySet<Identifier>(new Identifier[size], 0);
                    var effects = createStatusEffectArray(size);

                    for (String effectString : effectStrings) {
                        var effectID = Identifier.tryParse(effectString);
                        if (effectID == null) {
                            warnInvalidEffect(effectString);
                            invalid = true;
                            continue;
                        }

                        var effect = registry.getEntry(effectID);
                        if (effect.isEmpty()) {
                            warnInvalidEffect(effectString);
                            invalid = true;
                            continue;
                        }

                        if (effectIDs.add(effectID)) {
                            effects[effectIDs.size() - 1] = effect.get();
                        }
                    }

                    if (!invalid) {
                        storeEffectsInCache(key, effectIDs);
                    }

                    return new ObjectArraySet<>(effects, effectIDs.size());
                }
            }

            warnMissingKey(keyFullString);
            return ImmutableSet.of();
        }

    }

    //endregion IdentifierToEffectSetMapping implementation

    //region configEffectsMapping and configMaterialEffectOverridesMapping IdentifierToEffectSetMapping instances

    private final @NotNull IdentifierToEffectSetMapping configEffectsMapping = new IdentifierToEffectSetMapping() {
        protected void printWarningForMissingKey(String keyString) {
            LOGGER.warn("(TrimsEffects) Config is missing an effect for trim pattern \"{}\", consider adding it!", keyString);
        }
        protected void printWarningForInvalidEffect(String effectString) {
            LOGGER.warn("(TrimsEffects) Config has an unknown/invalid effect \"{}\", check your spelling", effectString);
        }

        protected Map<String, List<String>> getUnderlyingConfigValue() {
            return config.effects;
        }
    };
    private final @NotNull IdentifierToEffectSetMapping configMaterialEffectOverridesMapping = new IdentifierToEffectSetMapping() {
        protected void printWarningForMissingKey(String keyString) {
        }
        protected void printWarningForInvalidEffect(String effectString) {
            LOGGER.warn("(TrimsEffects) Config has an unknown/invalid effect \"{}\" in materialEffectOverrides, check your spelling", effectString);
        }

        protected Map<String, List<String>> getUnderlyingConfigValue() {
            return config.materialEffectOverrides;
        }
    };

    //endregion configEffectsMapping and configMaterialEffectOverridesMapping IdentifierToEffectSetMapping instances

    //region collectEffectsForPatternAndMaterial function

    private @NotNull Set<@NotNull RegistryEntry<StatusEffect>> collectEffectsForPatternAndMaterial(
        @NotNull RegistryEntryGetter<StatusEffect> registry,
        @NotNull RegistryKey<ArmorTrimMaterial> materialKey,
        @NotNull RegistryKey<ArmorTrimPattern> patternKey
    ) {
        var effectsOverridden = configMaterialEffectOverridesMapping.collectEffectsForKey(registry, materialKey.getValue());
        if (!effectsOverridden.isEmpty()) return effectsOverridden;
        return configEffectsMapping.collectEffectsForKey(registry, patternKey.getValue());
    }

    //endregion collectEffectsForPatternAndMaterial function

    //region TrimDetails record and createTrimDetails function

    public record TrimDetails(
        @NotNull ArmorTrim trim,
        @NotNull RegistryKey<ArmorTrimMaterial> materialKey,
        @NotNull RegistryKey<ArmorTrimPattern> patternKey,
        @NotNull Set<RegistryEntry<StatusEffect>> effects
    ) {}

    public @Nullable TrimDetails createTrimDetails(@NotNull RegistryEntryGetter<StatusEffect> registry, @NotNull ArmorTrim trim) {
        RegistryKey<ArmorTrimMaterial> materialKey = getRegistryKey(trim.material());
        RegistryKey<ArmorTrimPattern> patternKey = getRegistryKey(trim.pattern());

        var effects = collectEffectsForPatternAndMaterial(registry, materialKey, patternKey);
        if (effects.isEmpty()) return null;

        return new TrimDetails(trim, materialKey, patternKey, effects);
    }

    //endregion TrimDetails record and createTrimDetails function

    private boolean shouldApplyToEntity(@NotNull LivingEntity entity) {
        if (config.applyToMobs) {
            return true;
        } else {
            return entity instanceof PlayerEntity;
        }
    }

    public void onLivingEntityTick(@NotNull LivingEntity entity) {
        if (!shouldApplyToEntity(entity)) {
            removeEffectsFromTrims(entity, null);
            return;
        }

        DynamicRegistryManager manager = getRegistryManager(entity);

        Registry<StatusEffect> registry = null;
        ArrayList<TrimDetails> trims = null;

        for (ItemStack stack : entity.getArmorItems()) {
            var trim = getArmorTrimFromItemStack(manager, stack);
            if (trim != null) {
                if (registry == null) {
                    registry = getStatusEffectRegistry(manager);
                }

                var details = createTrimDetails(toRegistryEntryGetter(registry), trim);
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

    private int getConfigMatchingEffectLevel(int index) {
        var matchingEffectLevels = config.matchingEffectLevels;
        if (matchingEffectLevels.isEmpty()) return -1;

        if (index < 0) return matchingEffectLevels.getFirst();
        if (index >= matchingEffectLevels.size()) return matchingEffectLevels.getLast();

        return matchingEffectLevels.get(index);
    }

    private int getConfigMaterialEffectLevel(@NotNull Identifier key) {
        var materialEffectLevels = config.materialEffectLevels;
        if (materialEffectLevels.isEmpty()) return -1;

        String keyPathString = key.getPath();
        String keyFullString = key.toString();

        for (var entry : materialEffectLevels.entrySet()) {
            String entryKeyString = entry.getKey();

            if (
                keyPathString.equalsIgnoreCase(entryKeyString) ||
                keyFullString.equalsIgnoreCase(entryKeyString)
            ) {
                return entry.getValue();
            }
        }

        return -1;
    }

    private static final class EffectDetails {
        public final RegistryEntry<StatusEffect> effect;
        public final RegistryKey<StatusEffect> effectKey;

        private RegistryKey<ArmorTrimMaterial> materialKey;
        private int materialLevel = -1;

        public int count;
        public int amplifier = -2;

        public EffectDetails(
            RegistryEntry<StatusEffect> effect,
            RegistryKey<StatusEffect> effectKey,
            RegistryKey<ArmorTrimMaterial> materialKey
        ) {
            this.effect = effect;
            this.effectKey = effectKey;
            trySetMaterial(materialKey);
        }
        public EffectDetails(
            RegistryEntry<StatusEffect> effect,
            RegistryKey<ArmorTrimMaterial> materialKey
        ) {
            this(effect, getRegistryKey(effect), materialKey);
        }

        private static int getConfigMaterialEffectLevel(RegistryKey<ArmorTrimMaterial> materialKey) {
            return TrimEffects2.INSTANCE.getConfigMaterialEffectLevel(materialKey.getValue());
        }

        private int getConfigMaterialEffectLevel() {
            return materialKey != null ? getConfigMaterialEffectLevel(materialKey) : -1;
        }
        private int getConfigMatchingEffectLevel() {
            return TrimEffects2.INSTANCE.getConfigMatchingEffectLevel(count);
        }
        private int getConfigMaterialMinimumMatching() {
            return TrimEffects2.INSTANCE.config.materialEffectLevelsMinimumMatching;
        }

        public void trySetMaterial(RegistryKey<ArmorTrimMaterial> materialKey) {
            int oldLevel = getConfigMaterialEffectLevel();
            int newLevel = getConfigMaterialEffectLevel(materialKey);
            if (newLevel > oldLevel) {
                this.materialKey = materialKey;
                this.materialLevel = getConfigMaterialEffectLevel();
                amplifier = -2;
            }
        }

        public void incrementCount() {
            count++;
            amplifier = -2;
        }

        public int getAmplifier() {
            if (amplifier >= -1) return amplifier;

            if (materialLevel >= 0) {
                if (count + 1 >= getConfigMaterialMinimumMatching()) {
                    amplifier = Math.max(materialLevel - 1, -1);
                } else {
                    amplifier = -1;
                }
                return amplifier;
            }

            int level = getConfigMatchingEffectLevel();
            if (level >= 0) {
                amplifier = Math.max(level - 1, -1);
                return amplifier;
            }

            amplifier = -1;
            return amplifier;
        }

        public boolean shouldBeIgnored() {
            return getAmplifier() < 0;
        }

        public boolean shouldBeOmmittedFromTooltip() {
            return materialLevel == 0;
        }

        public StatusEffectInstance createStatusEffectInstance() {
            return new StatusEffectInstance(effect, STATUS_EFFECT_DURATION_MARKER, Math.max(getAmplifier(), 0));
        }
    }

    private List<EffectDetails> collectEffectDetails(List<TrimDetails> trims) {
        ArrayList<EffectDetails> effects = new ArrayList<>(trims.size());

        for (TrimDetails trimDetails : trims) {
            for (RegistryEntry<StatusEffect> effect : trimDetails.effects()) {
                RegistryKey<StatusEffect> effectKey = getRegistryKey(effect);

                boolean exists = false;

                for (EffectDetails effectDetails : effects) {
                    if (effectDetails.effectKey.equals(effectKey)) {
                        effectDetails.trySetMaterial(trimDetails.materialKey());
                        effectDetails.incrementCount();
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    effects.add(new EffectDetails(
                        effect, effectKey,
                        trimDetails.materialKey()
                    ));
                }
            }
        }

        effects.removeIf(EffectDetails::shouldBeIgnored);

        return effects;
    }

    public static boolean shouldOmitFromTooltip(TrimDetails trimDetails) {
        return trimDetails.effects().stream().allMatch(details -> new EffectDetails(details, trimDetails.materialKey()).shouldBeOmmittedFromTooltip());
    }

    private final Map<UUID, MutableInt> absorptionStunTicks = new HashMap<>();

    private boolean isAbsorption(RegistryEntry<StatusEffect> effect) {
        return StatusEffects.ABSORPTION.equals(effect);
    }

    private boolean isAbsorptionStunned(LivingEntity entity, StatusEffectInstance instance) {
        var stunTicks = absorptionStunTicks.computeIfAbsent(entity.getUuid(), (uuid) -> new MutableInt(0));
        if (stunTicks.intValue() > 0) {
            stunTicks.decrement();
            return true;
        }
        if (
            instance != null && instance.isInfinite() &&
            entity.getAbsorptionAmount() < entity.getMaxAbsorption()
        ) {
            int stunDurationInTicks = (int) (config.absorptionStunSeconds * 20.0);
            stunTicks.setValue(stunDurationInTicks);
            setStatusEffectDuration(instance, stunDurationInTicks);
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
