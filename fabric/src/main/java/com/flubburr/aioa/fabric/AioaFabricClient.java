package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.client.config.AioaConfigScreen;
import com.flubburr.aioa.client.config.AioaSpawnStudioScreen;
import com.flubburr.aioa.client.config.AioaBehaviorEditorScreen;
import com.flubburr.aioa.client.config.AioaScreenUtil;
import com.flubburr.aioa.client.config.AioaMobSelectionController;
import com.flubburr.aioa.network.AioaClientNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
                "key.categories." + AioaConstants.MOD_ID
        ));
        openSpawnStudioKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aioa.open_behavior_graph",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "key.categories." + AioaConstants.MOD_ID
        ));
        AioaClientNetworking.registerSender(request -> {
            var buffer = PacketByteBufs.create();
            buffer.writeUtf(request.entityId(), 128);
            buffer.writeDouble(request.x());
            buffer.writeDouble(request.y());
            buffer.writeDouble(request.z());
            buffer.writeBoolean(request.noAi());
            buffer.writeBoolean(request.facePlayer());
            buffer.writeBoolean(request.persistent());
            ClientPlayNetworking.send(AioaFabric.SPAWN_REQUEST, buffer);
        });
        AioaClientNetworking.registerGraphSender(request -> {
            var buffer = PacketByteBufs.create();
            buffer.writeUtf(request.graphJson(), 65_536);
            ClientPlayNetworking.send(AioaFabric.GRAPH_UPDATE, buffer);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AioaScreenUtil.tickMenuAudio(client);
            AioaMobSelectionController.tick(client);
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
                if (AioaMobSelectionController.isArmed()) {
                    AioaMobSelectionController.cancel(client, "AIOA mob selection cancelled.");
                    continue;
                }
                if (client.player != null && client.player.isCreative()) {
                    client.setScreen(AioaBehaviorEditorScreen.createForWorld(client.screen));
                }
            }
        });
    }
}
