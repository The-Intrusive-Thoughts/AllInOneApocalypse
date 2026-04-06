package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.client.config.AioaConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class AioaFabricClient implements ClientModInitializer {

    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aioa.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.player != null && client.player.isCreative()) {
                    client.setScreen(AioaConfigScreen.create(client.screen));
                }
            }
        });
    }
}
