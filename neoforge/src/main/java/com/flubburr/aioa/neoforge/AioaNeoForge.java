package com.flubburr.aioa.neoforge;

import com.flubburr.aioa.AioaCommon;
import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.network.AioaGraphPayload;
import com.flubburr.aioa.network.AioaSpawnPayload;
import com.flubburr.aioa.spawn.AioaSpawnStudioHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(AioaConstants.MOD_ID)
public final class AioaNeoForge {
    public AioaNeoForge(IEventBus modBus, ModContainer container, Dist dist) {
        var sounds = DeferredRegister.create(Registries.SOUND_EVENT, AioaConstants.MOD_ID);
        sounds.register("music.menu", () -> SoundEvent.createVariableRangeEvent(AioaEntityHelper.parseResourceLocation("aioa:music.menu")));
        sounds.register(modBus);
        modBus.addListener(AioaNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener(AioaNeoForge::endLevelTick);
        AioaCommon.init();
        if (dist == Dist.CLIENT) AioaNeoForgeClient.register(modBus, container);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(AioaSpawnPayload.TYPE, AioaSpawnPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) AioaSpawnStudioHandler.handle(player, payload.request());
        });
        registrar.playToServer(AioaGraphPayload.TYPE, AioaGraphPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) AioaGraphUpdateHandler.handle(player, payload.request());
        });
    }

    private static void endLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) AioaCommon.onServerLevelTick(level);
    }
}
