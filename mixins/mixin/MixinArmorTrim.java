package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.TrimEffects2;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.DynamicRegistryManager;
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
        var trim = TrimEffects2.getArmorTrim(manager, stack);
        if (trim == null) return;

        var details = TrimEffects2.INSTANCE.createTrimDetails(trim, TrimEffects2.getStatusEffectRegistry(manager));
        if (details == null) return;

        for (var effect : details.effects()) {
            tooltip.add(
                ScreenTexts.space()
                    .append(effect.getName())
                    .fillStyle(trim.getMaterial().value().description().getStyle())
            );
        }
    }

    /*
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
                        .append(effect.getName())
                        .fillStyle(self.getMaterial().value().description().getStyle())
                );
            }
        }
    }
    */

}
