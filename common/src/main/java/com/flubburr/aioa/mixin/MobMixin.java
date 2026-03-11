package com.flubburr.aioa.mixin;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void aioa$preventSunBurnForManagedDaySpawns(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (AioaConfigManager.getConfig().daySurfaceSpawns.preventSunlightBurn
                && (self.getTags().contains(AioaConstants.DAY_SPAWN_TAG)
                || AioaConfigManager.isBurnSafeDaySpawnEntity(self.getType()))) {
            cir.setReturnValue(false);
        }
    }
}
