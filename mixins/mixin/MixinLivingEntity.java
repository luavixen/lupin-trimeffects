package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.TrimEffects2;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    private MixinLivingEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "baseTick()V", at = @At("TAIL"))
    private void trimeffects$afterBaseTick(CallbackInfo info) {
        if (!getWorld().isClient) {
            TrimEffects2.INSTANCE.onLivingEntityTick((LivingEntity) (Object) this);
        }
    }

}
