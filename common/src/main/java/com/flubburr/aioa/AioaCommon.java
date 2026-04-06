package com.flubburr.aioa;

import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.platform.Services;
import com.flubburr.aioa.spawn.ApocalypseSpawnManager;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;

public final class AioaCommon {

    private static boolean initialized;
    private static final ResourceLocation MENU_MUSIC_ID = new ResourceLocation(AioaConstants.MOD_ID, "music.menu");

    private AioaCommon() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        registerSoundEvents();
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

    private static void registerSoundEvents() {
        if (!BuiltInRegistries.SOUND_EVENT.containsKey(MENU_MUSIC_ID)) {
            Registry.register(BuiltInRegistries.SOUND_EVENT, MENU_MUSIC_ID, SoundEvent.createVariableRangeEvent(MENU_MUSIC_ID));
        }
    }
}
