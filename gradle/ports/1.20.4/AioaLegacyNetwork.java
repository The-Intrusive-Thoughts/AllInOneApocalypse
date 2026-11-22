package com.flubburr.aioa.neoforge;

import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.network.AioaGraphUpdateRequest;
import com.flubburr.aioa.network.AioaSpawnRequest;
import com.flubburr.aioa.spawn.AioaSpawnStudioHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;

final class AioaLegacyNetwork {
    private record Spawn(AioaSpawnRequest request) implements CustomPacketPayload {
        static final ResourceLocation ID = new ResourceLocation("aioa", "spawn_request");
        public ResourceLocation id() { return ID; }
        public void write(FriendlyByteBuf buf) { request.write(buf); }
    }

    private record Graph(AioaGraphUpdateRequest request) implements CustomPacketPayload {
        static final ResourceLocation ID = new ResourceLocation("aioa", "graph_update");
        public ResourceLocation id() { return ID; }
        public void write(FriendlyByteBuf buf) { buf.writeUtf(request.graphJson(), 65_536); }
    }

    static void register(IEventBus bus) { bus.addListener(AioaLegacyNetwork::registerPayloads); }

    private static void registerPayloads(RegisterPayloadHandlerEvent event) {
        var registrar = event.registrar("aioa").versioned("1");
        registrar.play(Spawn.ID, buf -> new Spawn(AioaSpawnRequest.read(buf)), direction -> direction.server((packet, ctx) ->
                ctx.workHandler().execute(() -> {
                    if (ctx.player().orElse(null) instanceof ServerPlayer player) AioaSpawnStudioHandler.handle(player, packet.request());
                })));
        registrar.play(Graph.ID, buf -> new Graph(new AioaGraphUpdateRequest(buf.readUtf(65_536))), direction -> direction.server((packet, ctx) ->
                ctx.workHandler().execute(() -> {
                    if (ctx.player().orElse(null) instanceof ServerPlayer player) AioaGraphUpdateHandler.handle(player, packet.request());
                })));
    }

    static void send(AioaSpawnRequest request) { PacketDistributor.SERVER.noArg().send(new Spawn(request)); }
    static void send(AioaGraphUpdateRequest request) { PacketDistributor.SERVER.noArg().send(new Graph(request)); }
}
