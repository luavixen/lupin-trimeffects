package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.TrimEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ArmorTrim.class)
public abstract class MixinArmorTrim {

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private static void trimeffects$afterAppendTooltip(ItemStack stack, DynamicRegistryManager manager, List<Text> tooltip, CallbackInfo info) {
        var trim = TrimEffects.getTrim(manager, stack);
        if (trim != null) {
            var config = TrimEffects.getInstance().getConfig();

            var pattern = trim.getPattern();
            var material = trim.getMaterial();

            var effect = config.getEffects().get(TrimEffects.getKey(pattern));
            var strength = config.getStrengths().get(TrimEffects.getKey(material));

            if (effect != null && strength != null && strength > 0) {
                var effectType = manager.get(RegistryKeys.STATUS_EFFECT).get(effect);
                if (effectType != null) {
                    var text = ScreenTexts.space().append(effectType.getName());
                    if (strength > 1) {
                        text.append(ScreenTexts.SPACE);
                        if (strength <= 10) {
                            text.append(Text.translatable("enchantment.level." + strength));
                        } else {
                            text.append(strength.toString());
                        }
                    }
                    tooltip.add(text.fillStyle(material.value().description().getStyle()));
                }
            }
        }
    }

}
