package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaCommon;
import com.flubburr.aioa.fabric.config.AioaMidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class AioaFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AioaCommon.init();
        AioaMidnightConfig.initialize();
        ServerTickEvents.END_WORLD_TICK.register(AioaCommon::onServerLevelTick);
    }
}
