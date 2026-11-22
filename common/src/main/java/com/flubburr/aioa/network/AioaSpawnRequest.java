package com.flubburr.aioa.network;

public record AioaSpawnRequest(
        String entityId,
        double x,
        double y,
        double z,
        boolean noAi,
        boolean facePlayer,
        boolean persistent
) {

    public static AioaSpawnRequest read(net.minecraft.network.FriendlyByteBuf buf) {
        return new AioaSpawnRequest(buf.readUtf(128), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void write(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeUtf(entityId(), 128);
        buf.writeDouble(x());
        buf.writeDouble(y());
        buf.writeDouble(z());
        buf.writeBoolean(noAi());
        buf.writeBoolean(facePlayer());
        buf.writeBoolean(persistent());
    }
}
