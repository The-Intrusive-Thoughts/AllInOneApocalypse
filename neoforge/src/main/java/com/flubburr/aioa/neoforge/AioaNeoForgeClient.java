package com.flubburr.aioa.neoforge;

import com.flubburr.aioa.client.config.AioaBehaviorEditorScreen;
import com.flubburr.aioa.client.config.AioaConfigScreen;
import com.flubburr.aioa.client.config.AioaMobSelectionController;
import com.flubburr.aioa.client.config.AioaScreenUtil;
import com.flubburr.aioa.network.AioaClientNetworking;
import com.flubburr.aioa.network.AioaGraphPayload;
import com.flubburr.aioa.network.AioaSpawnPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

final class AioaNeoForgeClient {
    private static final KeyMapping CONFIG = new KeyMapping("key.aioa.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, "key.categories.aioa");
    private static final KeyMapping EDITOR = new KeyMapping("key.aioa.open_behavior_graph", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, "key.categories.aioa");

    static void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(AioaNeoForgeClient::registerKeys);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> AioaConfigScreen.create(parent));
        AioaClientNetworking.registerSender(request -> PacketDistributor.sendToServer(new AioaSpawnPayload(request)));
        AioaClientNetworking.registerGraphSender(request -> PacketDistributor.sendToServer(new AioaGraphPayload(request)));
        NeoForge.EVENT_BUS.addListener(AioaNeoForgeClient::clientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CONFIG);
        event.register(EDITOR);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        AioaScreenUtil.tickMenuAudio(client);
        AioaMobSelectionController.tick(client);
        while (CONFIG.consumeClick()) {
            if (client.player != null && client.player.isCreative()) client.setScreen(AioaConfigScreen.create(client.screen));
        }
        while (EDITOR.consumeClick()) {
            if (AioaMobSelectionController.isArmed()) AioaMobSelectionController.cancel(client, "AIOA mob selection cancelled.");
            else if (client.player != null && client.player.isCreative()) client.setScreen(AioaBehaviorEditorScreen.createForWorld(client.screen));
        }
    }
}
