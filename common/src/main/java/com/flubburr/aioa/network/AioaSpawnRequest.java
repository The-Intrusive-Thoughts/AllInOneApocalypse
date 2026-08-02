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
}
