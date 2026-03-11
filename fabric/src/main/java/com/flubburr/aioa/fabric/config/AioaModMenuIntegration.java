package com.flubburr.aioa.fabric.config;

import com.flubburr.aioa.AioaConstants;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import eu.midnightdust.lib.config.MidnightConfig;

public final class AioaModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            AioaMidnightConfig.initialize();
            AioaMidnightConfig.pullFromCommon();
            return MidnightConfig.getScreen(parent, AioaConstants.MOD_ID);
        };
    }
}
