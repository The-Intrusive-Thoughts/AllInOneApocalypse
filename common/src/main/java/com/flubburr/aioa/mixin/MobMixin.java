package com.flubburr.aioa.mixin;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.mixin.accessor.MobAccessor;
import com.flubburr.aioa.spawn.AioaZombieBehaviour;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Unique
    private AioaConfig.ZombieTargetMode aioa$lastTargetMode;

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void aioa$preventSunBurnForManagedDaySpawns(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (AioaConfigManager.getConfig().daySurfaceSpawns.preventSunlightBurn
                && (self.getTags().contains(AioaConstants.DAY_SPAWN_TAG)
                || AioaConfigManager.isBurnSafeDaySpawnEntity(self.getType()))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void aioa$applyConfiguredMobBehaviour(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        AioaConfig.DaySurfaceSpawns settings = AioaConfigManager.getConfig().daySurfaceSpawns;
        AioaConfig.ZombieTargetMode targetMode = AioaZombieBehaviour.getTargetMode(self, settings);

        if (targetMode != null && (targetMode != AioaConfig.ZombieTargetMode.VANILLA || self instanceof net.minecraft.world.entity.monster.Zombie)) {
            if (this.aioa$lastTargetMode != targetMode) {
                AioaZombieBehaviour.refreshTargetGoals(self, ((MobAccessor) self).aioa$getTargetSelector(), targetMode);
                this.aioa$lastTargetMode = targetMode;
            }

            LivingEntity target = self.getTarget();
            if (target != null && !AioaZombieBehaviour.canTarget(self, target, targetMode)) {
                self.setTarget(null);
            }
        }

        AioaZombieBehaviour.applyRefinedAi(
                self,
                AioaZombieBehaviour.usesRefinedAi(self, settings),
                settings.refinedPathfindingOpensDoors
        );
    }
}
