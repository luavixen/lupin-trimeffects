package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.TrimEffects2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.RegistryKeys;
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
        var trim = TrimEffects2.getArmorTrim(manager, stack);
        if (trim == null) return;

        var details = TrimEffects2.INSTANCE.createTrimDetails(
            TrimEffects2.toRegistryEntryGetter(TrimEffects2.getStatusEffectRegistry(manager)),
            trim
        );
        if (details == null) return;

        for (var effect : details.effects()) {
            tooltip.add(
                ScreenTexts.space()
                    .append(effect.value().getName())
                    .fillStyle(trim.getMaterial().value().description().getStyle())
            );
        }
    }
    */

    @Shadow @Final
    private boolean showInTooltip;

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void trimeffects$afterAppendTooltip(Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type, CallbackInfo info) {
        if (showInTooltip) {
            var self = (ArmorTrim) (Object) this;

            var registryLookup = context.getRegistryLookup();
            if (registryLookup == null) return;
            var registryWrapper = registryLookup.getWrapperOrThrow(RegistryKeys.STATUS_EFFECT);
            var registry = TrimEffects2.toRegistryEntryGetter(registryWrapper);

            var details = TrimEffects2.INSTANCE.createTrimDetails(registry, self);
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
