package com.flubburr.aioa.forge.config;

import com.flubburr.aioa.client.config.AioaConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class AioaForgeClient {

    private AioaForgeClient() {
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(AioaForgeClient::createScreen)
        );
    }

    private static Screen createScreen(Screen parent) {
        return AioaConfigScreen.create(parent);
    }
}
