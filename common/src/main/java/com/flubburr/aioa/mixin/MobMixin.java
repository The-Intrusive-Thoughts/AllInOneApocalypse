package com.flubburr.aioa.mixin;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.spawn.AioaAmbientAi;
import org.spongepowered.asm.mixin.Unique;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.behavior.AioaBehaviorRuntime;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin implements AioaBehaviorRuntime.MobData {
    @Unique
    private AioaBehaviorRuntime.MobState aioa$graphState;

    @Unique private AioaConfig.DaySurfaceSpawns aioa$lastSettings;
    @Unique private AioaAmbientAi aioa$ambient;

    public void aioa$releaseAmbientAi() {
        if (aioa$ambient != null) aioa$ambient.restore((Mob) (Object) this);
        aioa$ambient = null;
        aioa$lastSettings = null;
    }

    public AioaBehaviorRuntime.MobState aioa$getGraphState() { return aioa$graphState; }
    public void aioa$setGraphState(AioaBehaviorRuntime.MobState state) { aioa$graphState = state; }


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
        if (self.level().isClientSide()) return;
        AioaBehaviorRuntime.tick(self);
        if (aioa$graphState != null || self.isNoAi()) return;
        var settings = AioaConfigManager.getConfig().daySurfaceSpawns;
        if (settings != aioa$lastSettings) {
            aioa$releaseAmbientAi();
            aioa$ambient = AioaAmbientAi.configure(self, settings);
            aioa$lastSettings = settings;
        }
        if (aioa$ambient != null) aioa$ambient.tick(self, settings);
    }
}
