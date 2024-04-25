package dev.foxgirl.trimeffects;

import net.fabricmc.api.ModInitializer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class TrimEffects implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("trimeffects");

    private static TrimEffects INSTANCE;

    public static TrimEffects getInstance() {
        return INSTANCE;
    }

    public TrimEffects() {
        INSTANCE = this;
    }

    private Config.Parsed config;

    public @NotNull Config.Parsed getConfig() {
        return Objects.requireNonNull(config, "Expression 'config'");
    }

    @Override
    public void onInitialize() {
        config = Config.read().parse();
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

    private static final Map<UUID, Integer> absorptionStunTicks = new HashMap<>();

    public void handleTick(LivingEntity player) {
        var manager = getRegistryManager(player);

        var armor = (List<ItemStack>) player.getArmorItems();

        var trim = getTrim(manager, armor.get(0));
        if (trim == null) return;

        var pattern = getKey(trim.getPattern());
        var material = getKey(trim.getMaterial());

        for (int i = 1, size = armor.size(); i < size; i++) {
            var trimCurrent = getTrim(manager, armor.get(i));
            if (trimCurrent == null) return;
            if (pattern != getKey(trimCurrent.getPattern()) || material != getKey(trimCurrent.getMaterial())) return;
        }

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
                    player.addStatusEffect(new StatusEffectInstance(effectTypeEntry, durationMaximum, amplifier), player);
                }
            }
        }
    }

}
