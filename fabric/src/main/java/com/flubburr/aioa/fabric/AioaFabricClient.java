package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.client.config.AioaConfigScreen;
import com.flubburr.aioa.client.config.AioaSpawnStudioScreen;
import com.flubburr.aioa.network.AioaClientNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class AioaFabricClient implements ClientModInitializer {

    private static KeyMapping openConfigKey;
    private static KeyMapping openSpawnStudioKey;

    @Override
    public void onInitializeClient() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aioa.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                KeyMapping.Category.MISC
        ));
        openSpawnStudioKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aioa.open_spawn_studio",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "key.categories." + AioaConstants.MOD_ID
        ));
        AioaClientNetworking.registerSender(request -> {
            ClientPlayNetworking.send(new AioaFabricSpawnPayload(request));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.player != null && client.player.isCreative()) {
                    if (client.hasSingleplayerServer() || client.getCurrentServer() == null) {
                        client.setScreen(AioaConfigScreen.create(client.screen));
                    } else {
                        client.player.displayClientMessage(Component.translatable("aioa.common.multiplayer_locked"), true);
                    }
                }
            }
            while (openSpawnStudioKey.consumeClick()) {
                if (client.player != null && client.player.isCreative()) {
                    client.setScreen(AioaSpawnStudioScreen.create(client.screen));
                }
            }
        });
    }
}
