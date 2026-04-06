package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaCommon;
import com.flubburr.aioa.fabric.config.AioaMidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class AioaFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AioaCommon.init();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            AioaMidnightConfig.initialize();
        }
        ServerTickEvents.END_WORLD_TICK.register(AioaCommon::onServerLevelTick);
    }
}
