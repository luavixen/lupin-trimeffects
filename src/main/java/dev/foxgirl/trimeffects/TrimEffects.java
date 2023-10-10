package dev.foxgirl.trimeffects;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@Mod("trimeffects")
public final class TrimEffects {

    public static final Logger LOGGER = LogManager.getLogger("trimeffects");

    private static TrimEffects INSTANCE;

    public static TrimEffects getInstance() {
        return INSTANCE;
    }

    public TrimEffects() {
        INSTANCE = this;

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private Config.Parsed config;

    public @NotNull Config.Parsed getConfig() {
        return Objects.requireNonNull(config, "Expression 'config'");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        config = Config.read().parse();
    }

    public static <T> @NotNull RegistryKey<T> getKey(@NotNull RegistryEntry<T> entry) {
        return entry.getKey().orElseThrow();
    }

    public static @Nullable ArmorTrim getTrim(@NotNull DynamicRegistryManager manager, @NotNull ItemStack stack) {
        var nbt = stack.getSubNbt("Trim");
        if (nbt == null || !stack.isIn(ItemTags.TRIMMABLE_ARMOR)) return null;
        return ArmorTrim.CODEC.parse(RegistryOps.of(NbtOps.INSTANCE, manager), nbt).result().orElse(null);
    }

    public void handlePlayerTick(ServerPlayerEntity player) {
        var manager = player.getWorld().getRegistryManager();

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
            var effectType = manager.get(RegistryKeys.STATUS_EFFECT).get(effect);
            if (effectType != null) {
                var effectInstance = player.getStatusEffect(effectType);
                if (effectInstance == null || effectInstance.isDurationBelow(durationMinimum)) {
                    player.addStatusEffect(new StatusEffectInstance(effectType, durationMaximum, strength - 1), player);
                }
            }
        }
    }

}
