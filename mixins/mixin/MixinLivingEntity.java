package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.TrimEffects2;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "baseTick()V", at = @At("TAIL"))
    private void trimeffects$afterBaseTick(CallbackInfo info) {
        TrimEffects2.INSTANCE.onLivingEntityTick((LivingEntity) (Object) this);
    }

}
