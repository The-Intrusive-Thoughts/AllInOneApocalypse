package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.network.AioaGraphUpdateRequest;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AioaFabricGraphPayload(AioaGraphUpdateRequest request) implements CustomPacketPayload {
    public static final Type<AioaFabricGraphPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AioaConstants.MOD_ID, "graph_update"));
    public static final StreamCodec<FriendlyByteBuf, AioaFabricGraphPayload> CODEC = new StreamCodec<>() {
        @Override public AioaFabricGraphPayload decode(FriendlyByteBuf buffer) {
            return new AioaFabricGraphPayload(new AioaGraphUpdateRequest(buffer.readUtf(65_536)));
        }
        @Override public void encode(FriendlyByteBuf buffer, AioaFabricGraphPayload payload) {
            buffer.writeUtf(payload.request().graphJson(), 65_536);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
