package com.flubburr.aioa;

import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.platform.Services;
import com.flubburr.aioa.spawn.ApocalypseSpawnManager;
import net.minecraft.server.level.ServerLevel;

public final class AioaCommon {

    private static boolean initialized;
    private AioaCommon() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        AioaConfigManager.bootstrap();
        AioaConstants.LOG.info(
                "Initialized {} on {} ({})",
                AioaConstants.MOD_NAME,
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.getEnvironmentName()
        );
    }

    public static void onServerLevelTick(ServerLevel level) {
        AioaConfigManager.refreshIfChanged();
        AioaConfigManager.ensureMobCatalogWritten(level);
        ApocalypseSpawnManager.tick(level);
    }

}
