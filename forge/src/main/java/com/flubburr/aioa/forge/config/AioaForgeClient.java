package com.flubburr.aioa.forge.config;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.client.config.AioaConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public final class AioaForgeClient {

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.aioa.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            KeyMapping.Category.MISC
    );

    private AioaForgeClient() {
    }

    public static void registerConfigScreen() {
        RegisterKeyMappingsEvent.getBus(FMLJavaModLoadingContext.get().getModBusGroup()).addListener(AioaForgeClient::onRegisterKeyMappings);
        MinecraftForge.registerConfigScreen(AioaForgeClient::createScreen);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG_KEY);
    }

    public static Screen createScreen(Screen parent) {
        return AioaConfigScreen.create(parent);
    }

    public static KeyMapping openConfigKey() {
        return OPEN_CONFIG_KEY;
    }
}
