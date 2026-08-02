package com.flubburr.aioa.fabric;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.network.AioaSpawnRequest;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AioaFabricSpawnPayload(AioaSpawnRequest request) implements CustomPacketPayload {
    public static final Type<AioaFabricSpawnPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AioaConstants.MOD_ID, "spawn_request"));
    public static final StreamCodec<FriendlyByteBuf, AioaFabricSpawnPayload> CODEC = new StreamCodec<>() {
        @Override
        public AioaFabricSpawnPayload decode(FriendlyByteBuf buffer) {
            return new AioaFabricSpawnPayload(new AioaSpawnRequest(
                    buffer.readUtf(128), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()
            ));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, AioaFabricSpawnPayload payload) {
            AioaSpawnRequest request = payload.request();
            buffer.writeUtf(request.entityId(), 128);
            buffer.writeDouble(request.x());
            buffer.writeDouble(request.y());
            buffer.writeDouble(request.z());
            buffer.writeBoolean(request.noAi());
            buffer.writeBoolean(request.facePlayer());
            buffer.writeBoolean(request.persistent());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
