package com.flubburr.aioa.fabric;

import com.flubburr.aioa.fabric.config.AioaMidnightConfig;
import net.fabricmc.api.ClientModInitializer;

public final class AioaFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AioaMidnightConfig.initialize();
    }
}
