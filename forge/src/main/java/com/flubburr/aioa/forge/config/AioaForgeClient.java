package com.flubburr.aioa.forge.config;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.client.config.AioaConfigScreen;
import com.flubburr.aioa.client.config.AioaSpawnStudioScreen;
import com.flubburr.aioa.network.AioaClientNetworking;
import com.flubburr.aioa.forge.AioaForge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public final class AioaForgeClient {

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.aioa.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "key.categories." + AioaConstants.MOD_ID
    );
    private static final KeyMapping OPEN_SPAWN_STUDIO_KEY = new KeyMapping(
            "key.aioa.open_spawn_studio", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7,
            "key.categories." + AioaConstants.MOD_ID
    );

    private AioaForgeClient() {
    }

    public static void registerConfigScreen() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AioaForgeClient::onRegisterKeyMappings);
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(AioaForgeClient::createScreen)
        );
        AioaClientNetworking.registerSender(request -> AioaForge.NETWORK.send(request, net.minecraftforge.network.PacketDistributor.SERVER.noArg()));
        AioaClientNetworking.registerGraphSender(request -> AioaForge.NETWORK.send(request, net.minecraftforge.network.PacketDistributor.SERVER.noArg()));
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG_KEY);
        event.register(OPEN_SPAWN_STUDIO_KEY);
    }

    public static Screen createScreen(Minecraft minecraft, Screen parent) {
        return createScreen(parent);
    }

    public static Screen createScreen(Screen parent) {
        return AioaConfigScreen.create(parent);
    }

    public static KeyMapping openConfigKey() {
        return OPEN_CONFIG_KEY;
    }

    public static KeyMapping openSpawnStudioKey() {
        return OPEN_SPAWN_STUDIO_KEY;
    }
}
