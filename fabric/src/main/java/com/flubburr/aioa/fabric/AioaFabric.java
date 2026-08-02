package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaCommon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.network.AioaSpawnRequest;
import com.flubburr.aioa.spawn.AioaSpawnStudioHandler;

public final class AioaFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AioaCommon.init();
        ServerTickEvents.END_WORLD_TICK.register(AioaCommon::onServerLevelTick);
        PayloadTypeRegistry.playC2S().register(AioaFabricSpawnPayload.TYPE, AioaFabricSpawnPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AioaFabricSpawnPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> AioaSpawnStudioHandler.handle(context.player(), payload.request()));
        });
    }
}
