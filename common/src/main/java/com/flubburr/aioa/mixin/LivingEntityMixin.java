package com.flubburr.aioa.mixin;

import com.flubburr.aioa.behavior.AioaBehaviorRuntime;
import com.flubburr.aioa.spawn.AioaZombieBehaviour;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void aioa$allowZombieWallClimbing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Mob mob
                && AioaBehaviorRuntime.allowsWallClimbing(mob)
                && AioaZombieBehaviour.shouldClimb(mob)) {
            cir.setReturnValue(true);
        }
    }
}
