package com.flubburr.aioa.network;

import java.util.Objects;
import java.util.function.Consumer;

public final class AioaClientNetworking {
    private static Consumer<AioaSpawnRequest> sender = request -> { };
    private static Consumer<AioaGraphUpdateRequest> graphSender = request -> { };

    private AioaClientNetworking() {
    }

    public static void registerSender(Consumer<AioaSpawnRequest> packetSender) {
        sender = Objects.requireNonNull(packetSender);
    }

    public static void sendSpawnRequest(AioaSpawnRequest request) {
        sender.accept(request);
    }

    public static void registerGraphSender(Consumer<AioaGraphUpdateRequest> packetSender) {
        graphSender = Objects.requireNonNull(packetSender);
    }

    public static void sendGraphUpdate(AioaGraphUpdateRequest request) {
        graphSender.accept(request);
    }
}
