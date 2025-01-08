package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.TrimEffects2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ArmorTrim.class)
public abstract class MixinArmorTrim {

    /*
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
    */

    @Shadow @Final
    private boolean showInTooltip;

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void trimeffects$afterAppendTooltip(Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type, CallbackInfo info) {
        if (showInTooltip) {
            var self = (ArmorTrim) (Object) this;

            var player = MinecraftClient.getInstance().player;
            if (player == null) return;

            var details = TrimEffects2.INSTANCE.createTrimDetails(self, TrimEffects2.getStatusEffectRegistry(player));
            if (details == null) return;

            for (var effect : details.effects()) {
                tooltip.accept(
                    ScreenTexts.space()
                        .append(effect.value().getName())
                        .fillStyle(self.getMaterial().value().description().getStyle())
                );
            }
        }
    }

}
