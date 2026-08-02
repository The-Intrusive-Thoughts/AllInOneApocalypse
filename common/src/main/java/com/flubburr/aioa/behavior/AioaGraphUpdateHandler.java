package com.flubburr.aioa.behavior;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.network.AioaGraphUpdateRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class AioaGraphUpdateHandler {
    private static final int MAX_GRAPH_JSON_LENGTH = 65_536;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private AioaGraphUpdateHandler() {
    }

    public static AioaGraphUpdateRequest createRequest(AioaBehaviorGraph graph) {
        return new AioaGraphUpdateRequest(GSON.toJson(graph));
    }

    public static void handle(ServerPlayer player, AioaGraphUpdateRequest request) {
        if (!player.isCreative() || request.graphJson() == null || request.graphJson().length() > MAX_GRAPH_JSON_LENGTH) {
            return;
        }
        try {
            AioaBehaviorGraph graph = GSON.fromJson(request.graphJson(), AioaBehaviorGraph.class).sanitize();
            List<String> issues = AioaBehaviorValidator.validate(graph);
            if (!issues.isEmpty()) {
                player.displayClientMessage(Component.literal("AIOA graph rejected: " + issues.get(0)), true);
                return;
            }
            if (graph.scope != AioaBehaviorGraph.Scope.SINGLE_ENTITY
                    && !player.level().getServer().getPlayerList().isOp(player.nameAndId())) {
                player.displayClientMessage(Component.literal("Operator permission is required for non-instance behavior graphs."), true);
                return;
            }

            AioaConfig config = AioaConfigManager.getConfigCopy();
            config.behaviorGraphs.removeIf(existing -> existing.id.equals(graph.id));
            config.behaviorGraphs.add(graph);
            AioaConfigManager.save(config);
            player.displayClientMessage(Component.literal("Applied behavior graph: " + graph.name), true);
        } catch (RuntimeException exception) {
            player.displayClientMessage(Component.literal("AIOA graph data was invalid."), true);
        }
    }
}
