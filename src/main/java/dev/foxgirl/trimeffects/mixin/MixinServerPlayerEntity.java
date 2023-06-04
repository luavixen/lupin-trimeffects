package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.Mod;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity {

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void afterTick(CallbackInfo info) {
        Mod.getInstance().handlePlayerTick((ServerPlayerEntity) (Object) this);
    }

}
