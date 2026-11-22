package com.flubburr.aioa.mixin;

import com.flubburr.aioa.behavior.AioaBehaviorRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Brain.class)
public abstract class BrainMixin<E extends LivingEntity> {
    @Unique private boolean aioa$paused;

    @Shadow public abstract void stopAll(ServerLevel level, E owner);

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aioa$yieldToGraph(ServerLevel level, E owner, CallbackInfo callback) {
        if (owner instanceof AioaBehaviorRuntime.MobData mob && mob.aioa$getGraphState() != null) {
            if (!aioa$paused) stopAll(level, owner);
            aioa$paused = true;
            callback.cancel();
        } else {
            aioa$paused = false;
        }
    }
}
