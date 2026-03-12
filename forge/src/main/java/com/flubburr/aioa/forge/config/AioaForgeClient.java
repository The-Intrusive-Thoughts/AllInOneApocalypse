package com.flubburr.aioa.forge.config;

import com.flubburr.aioa.AioaConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.lang.reflect.Method;

public final class AioaForgeClient {

    private static Boolean yaclAvailable;
    private static boolean missingYaclWarningLogged;

    private AioaForgeClient() {
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(AioaForgeClient::createScreen)
        );
    }

    private static Screen createScreen(Minecraft minecraft, Screen parent) {
        if (isClassPresent("dev.isxander.yacl3.api.YetAnotherConfigLib")) {
            Screen yaclScreen = createYaclScreen(parent);
            if (yaclScreen != null) {
                return yaclScreen;
            }
        } else if (!missingYaclWarningLogged) {
            AioaConstants.LOG.warn("YACL runtime not found. Falling back to the built-in Forge config screen.");
            missingYaclWarningLogged = true;
        }

        return AioaForgeFallbackConfigScreen.create(parent);
    }

    private static Screen createYaclScreen(Screen parent) {
        try {
            Class<?> screenFactoryClass = Class.forName("com.flubburr.aioa.forge.config.AioaYaclConfigScreen");
            Method createMethod = screenFactoryClass.getMethod("create", Screen.class);
            Object result = createMethod.invoke(null, parent);
            if (result instanceof Screen screen) {
                return screen;
            }
        } catch (Throwable throwable) {
            AioaConstants.LOG.warn("Failed to create AIOA YACL config screen. Using the built-in Forge config screen.", throwable);
        }
        return null;
    }

    private static boolean isClassPresent(String className) {
        if (yaclAvailable != null) {
            return yaclAvailable;
        }

        try {
            Class.forName(className);
            yaclAvailable = true;
            return true;
        } catch (ClassNotFoundException ignored) {
            yaclAvailable = false;
            return false;
        }
    }
}
