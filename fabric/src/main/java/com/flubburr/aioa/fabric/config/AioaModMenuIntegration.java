package com.flubburr.aioa.fabric.config;

import com.flubburr.aioa.client.config.AioaConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class AioaModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AioaConfigScreen::create;
    }
}
