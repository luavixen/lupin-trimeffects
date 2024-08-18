package dev.foxgirl.trimeffects;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimPattern;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public final class TrimEffects {

    public static final Logger LOGGER = LogManager.getLogger("trimeffects");

    private static TrimEffects INSTANCE;

    public static @NotNull TrimEffects getInstance() {
        return INSTANCE;
    }

    public static @NotNull TrimEffects createInstance() {
        return new TrimEffects();
    }

    private TrimEffects() {
        INSTANCE = this;
    }

    private Config.Parsed config;

    public @NotNull Config.Parsed getConfig() {
        return Objects.requireNonNull(config, "Expression 'config'");
    }

    public void initialize(@NotNull Path configDirectory) {
        config = Config.read(configDirectory).parse();
    }

    public static @NotNull DynamicRegistryManager getRegistryManager(@NotNull Entity entity) {
        return entity.getWorld().getRegistryManager();
    }

    public static <T> @NotNull RegistryKey<T> getKey(@NotNull RegistryEntry<T> entry) {
        return entry.getKey().orElseThrow();
    }

    public static @Nullable ArmorTrim getTrim(@NotNull DynamicRegistryManager manager, @NotNull ItemStack stack) {
        return stack.get(DataComponentTypes.TRIM);
        /*
        var nbt = stack.getSubNbt("Trim");
        if (nbt == null || !stack.isIn(ItemTags.TRIMMABLE_ARMOR)) return null;
        return ArmorTrim.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, manager), nbt).result().orElse(null);
        */
    }

    private record Trim(@NotNull ArmorTrim trim) {
        private static Trim from(@NotNull DynamicRegistryManager manager, @NotNull ItemStack stack) {
            var trim = getTrim(manager, stack);
            return trim == null ? null : new Trim(trim);
        }

        private @NotNull RegistryEntry<ArmorTrimPattern> getPattern() {
            return trim.getPattern();
        }
        private @NotNull RegistryEntry<ArmorTrimMaterial> getMaterial() {
            return trim.getMaterial();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            var that = (Trim) obj;
            return Objects.equals(this.getPattern(), that.getPattern())
                && Objects.equals(this.getMaterial(), that.getMaterial());
        }
    }

    public void handleTick(LivingEntity player) {
        var manager = getRegistryManager(player);

        var armor = (List<ItemStack>) player.getArmorItems();
        var trims = new Trim[armor.size()];

        for (int i = 0, length = trims.length; i < length; i++) {
            trims[i] = Trim.from(manager, armor.get(i));
        }

        for (Trim trim : trims) {
            if (trim == null) {
                continue;
            }
            if (getConfig().isEnableCombinedEffects()) {
                if (Arrays.stream(trims).anyMatch(t -> t != null && !trim.equals(t) && trim.getPattern().equals(t.getPattern()))) {
                    continue;
                }
            } else {
                if (Arrays.stream(trims).anyMatch(t -> t != null && !trim.equals(t))) {
                    continue;
                }
            }
            int count = (int) Arrays.stream(trims).filter(t -> Objects.equals(t, trim)).count();
            if (count >= getConfig().getMinimumMatchingTrims()) {
                handleTickForTrim(player, trim);
            }
        }
    }

    private final Map<UUID, Integer> absorptionStunTicks = new HashMap<>();

    private void handleTickForTrim(LivingEntity player, Trim trim) {
        var manager = getRegistryManager(player);

        var pattern = getKey(trim.getPattern());
        var material = getKey(trim.getMaterial());

        var effect = getConfig().getEffects().get(pattern);
        var strength = getConfig().getStrengths().get(material);

        int durationMaximum = (int) ((getConfig().getSecondsMaximum() + 0.75) * 20.0);
        int durationMinimum = (int) ((getConfig().getSecondsMinimum() + 0.75) * 20.0);

        if (effect != null && strength != null && strength > 0) {
            int amplifier = strength - 1;
            var effectTypeEntryOptional = manager.get(RegistryKeys.STATUS_EFFECT).getEntry(effect);
            if (effectTypeEntryOptional.isPresent()) {
                var effectTypeEntry = effectTypeEntryOptional.get();
                var effectInstance = player.getStatusEffect(effectTypeEntry);
                if (
                    effectInstance == null ||
                    effectInstance.getAmplifier() < amplifier ||
                    effectInstance.isDurationBelow(durationMinimum)
                ) {
                    if (effectTypeEntry.matches(StatusEffects.ABSORPTION)) {
                        var stunTicks = absorptionStunTicks.get(player.getUuid());
                        if (stunTicks != null && stunTicks > 0) {
                            absorptionStunTicks.put(player.getUuid(), stunTicks - 1);
                            return;
                        }
                        if (effectInstance != null && player.getAbsorptionAmount() < player.getMaxAbsorption()) {
                            absorptionStunTicks.put(player.getUuid(), (int) (getConfig().getAbsorptionStunSeconds() * 2.0));
                            return;
                        }
                    }
                    if (effectTypeEntry.matches(StatusEffects.REGENERATION)) {
                        if (
                            effectInstance != null && !effectInstance.isDurationBelow(50) &&
                            player.getHealth() < player.getMaxHealth()
                        ) {
                            return;
                        }
                    }
                    player.addStatusEffect(new StatusEffectInstance(effectTypeEntry, durationMaximum, amplifier), player);
                }
            }
        }
    }

}
