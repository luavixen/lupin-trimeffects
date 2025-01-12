package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.TrimEffects2;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectInstance.class)
public abstract class MixinStatusEffectInstance implements TrimEffects2.DurationSetter {

    @Shadow
    private int duration;

    @Override
    public void trimeffects$setDuration(int duration) {
        this.duration = duration;
    }

    @Inject(method = "isInfinite()Z", at = @At("HEAD"), cancellable = true)
    private void trimeffects$beforeIsInfinite(CallbackInfoReturnable<Boolean> info) {
        if (duration == TrimEffects2.STATUS_EFFECT_DURATION_MARKER) {
            info.setReturnValue(true);
        }
    }

}
