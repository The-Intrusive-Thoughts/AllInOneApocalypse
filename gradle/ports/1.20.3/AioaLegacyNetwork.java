package com.flubburr.aioa.neoforge;

import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.network.AioaGraphUpdateRequest;
import com.flubburr.aioa.network.AioaSpawnRequest;
import com.flubburr.aioa.spawn.AioaSpawnStudioHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.simple.SimpleChannel;

final class AioaLegacyNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("aioa", "main"), () -> "1", "1"::equals, "1"::equals);

    static void register(IEventBus bus) {
        CHANNEL.registerMessage(0, AioaSpawnRequest.class, (r, b) -> r.write(b), AioaSpawnRequest::read, (request, ctx) -> {
            ctx.enqueueWork(() -> {
                if (ctx.getSender() != null) AioaSpawnStudioHandler.handle(ctx.getSender(), request);
            });
            ctx.setPacketHandled(true);
        });
        CHANNEL.registerMessage(1, AioaGraphUpdateRequest.class, (r, b) -> b.writeUtf(r.graphJson(), 65_536),
                b -> new AioaGraphUpdateRequest(b.readUtf(65_536)), (request, ctx) -> {
                    ctx.enqueueWork(() -> {
                        if (ctx.getSender() != null) AioaGraphUpdateHandler.handle(ctx.getSender(), request);
                    });
                    ctx.setPacketHandled(true);
                });
    }

    static void send(AioaSpawnRequest request) { CHANNEL.sendToServer(request); }
    static void send(AioaGraphUpdateRequest request) { CHANNEL.sendToServer(request); }
}
