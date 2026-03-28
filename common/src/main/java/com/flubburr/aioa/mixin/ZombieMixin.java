package com.flubburr.aioa.mixin;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.mixin.accessor.MobAccessor;
import com.flubburr.aioa.spawn.AioaZombieBehaviour;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public abstract class ZombieMixin {

    @Unique
    private AioaConfig.ZombieTargetMode aioa$lastTargetMode;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void aioa$refineAiAndTargeting(CallbackInfo ci) {
        Zombie self = (Zombie) (Object) this;
        AioaConfig.DaySurfaceSpawns settings = AioaConfigManager.getConfig().daySurfaceSpawns;

        if (this.aioa$lastTargetMode != settings.zombieTargetMode) {
            AioaZombieBehaviour.refreshTargetGoals(self, ((MobAccessor) self).aioa$getTargetSelector(), settings.zombieTargetMode);
            this.aioa$lastTargetMode = settings.zombieTargetMode;
        }

        LivingEntity target = self.getTarget();
        if (target != null && !AioaZombieBehaviour.canTarget(self, target)) {
            self.setTarget(null);
        }

        AioaZombieBehaviour.applyRefinedAi(self, settings.refinedZombieAi);
    }
}
