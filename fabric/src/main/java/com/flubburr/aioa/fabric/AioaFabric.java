package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaCommon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import com.flubburr.aioa.compat.AioaRegistryCompat;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.network.AioaSpawnRequest;
import com.flubburr.aioa.network.AioaGraphUpdateRequest;
import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.spawn.AioaSpawnStudioHandler;

public final class AioaFabric implements ModInitializer {

    public static final Identifier MENU_MUSIC = Identifier.fromNamespaceAndPath(AioaConstants.MOD_ID, "music.menu");
    @Override
    public void onInitialize() {
        var soundEvents = AioaRegistryCompat.soundEvents();
        if (soundEvents != null && !soundEvents.containsKey(MENU_MUSIC)) {
            Registry.register(soundEvents, MENU_MUSIC, SoundEvent.createVariableRangeEvent(MENU_MUSIC));
        }
        AioaCommon.init();
        ServerTickEvents.END_LEVEL_TICK.register(AioaCommon::onServerLevelTick);
        PayloadTypeRegistry.serverboundPlay().register(AioaFabricSpawnPayload.TYPE, AioaFabricSpawnPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AioaFabricGraphPayload.TYPE, AioaFabricGraphPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AioaFabricSpawnPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> AioaSpawnStudioHandler.handle(context.player(), payload.request()));
        });
        ServerPlayNetworking.registerGlobalReceiver(AioaFabricGraphPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> AioaGraphUpdateHandler.handle(context.player(), payload.request()));
        });
    }
}
