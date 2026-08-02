package com.flubburr.aioa.network;

import java.util.Objects;
import java.util.function.Consumer;

public final class AioaClientNetworking {
    private static Consumer<AioaSpawnRequest> sender = request -> { };

    private AioaClientNetworking() {
    }

    public static void registerSender(Consumer<AioaSpawnRequest> packetSender) {
        sender = Objects.requireNonNull(packetSender);
    }

    public static void sendSpawnRequest(AioaSpawnRequest request) {
        sender.accept(request);
    }
}
