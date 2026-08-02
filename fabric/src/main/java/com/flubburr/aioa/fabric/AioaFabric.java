package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaCommon;
import com.flubburr.aioa.fabric.config.AioaMidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.network.AioaSpawnRequest;
import com.flubburr.aioa.network.AioaGraphUpdateRequest;
import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.spawn.AioaSpawnStudioHandler;

public final class AioaFabric implements ModInitializer {

    public static final ResourceLocation SPAWN_REQUEST = new ResourceLocation(AioaConstants.MOD_ID, "spawn_request");
    public static final ResourceLocation GRAPH_UPDATE = new ResourceLocation(AioaConstants.MOD_ID, "graph_update");

    @Override
    public void onInitialize() {
        AioaCommon.init();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            AioaMidnightConfig.initialize();
        }
        ServerTickEvents.END_WORLD_TICK.register(AioaCommon::onServerLevelTick);
        ServerPlayNetworking.registerGlobalReceiver(SPAWN_REQUEST, (server, player, handler, buffer, responseSender) -> {
            AioaSpawnRequest request = readSpawnRequest(buffer);
            server.execute(() -> AioaSpawnStudioHandler.handle(player, request));
        });
        ServerPlayNetworking.registerGlobalReceiver(GRAPH_UPDATE, (server, player, handler, buffer, responseSender) -> {
            AioaGraphUpdateRequest request = new AioaGraphUpdateRequest(buffer.readUtf(65_536));
            server.execute(() -> AioaGraphUpdateHandler.handle(player, request));
        });
    }

    private static AioaSpawnRequest readSpawnRequest(FriendlyByteBuf buffer) {
        return new AioaSpawnRequest(
                buffer.readUtf(128), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()
        );
    }
}
