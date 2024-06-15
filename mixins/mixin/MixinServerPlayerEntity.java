package dev.foxgirl.trimeffects.mixin;

import dev.foxgirl.trimeffects.TrimEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends LivingEntity {

    protected MixinServerPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique
    private int trimeffects$ageUpdated;

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void trimeffects$afterTick(CallbackInfo info) {
        if (trimeffects$ageUpdated < age - 10) {
            trimeffects$ageUpdated = age;
            TrimEffects.getInstance().handleTick((ServerPlayerEntity) (Object) this);
        }
    }

}
