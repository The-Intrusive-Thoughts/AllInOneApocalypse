package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.client.config.AioaConfigScreen;
import com.flubburr.aioa.client.config.AioaScreenUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class AioaFabricClient implements ClientModInitializer {

    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aioa.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "key.categories." + AioaConstants.MOD_ID
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AioaScreenUtil.tickMenuAudio(client);
            while (openConfigKey.consumeClick()) {
                if (client.player != null && client.player.isCreative()) {
                    if (client.hasSingleplayerServer() || client.getCurrentServer() == null) {
                        client.setScreen(AioaConfigScreen.create(client.screen));
                    } else {
                        client.player.displayClientMessage(Component.translatable("aioa.common.multiplayer_locked"), true);
                    }
                }
            }
        });
    }
}
