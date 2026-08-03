package com.flubburr.aioa.behavior;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.network.AioaGraphUpdateRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class AioaGraphUpdateHandler {
    private static final int MAX_GRAPH_JSON_LENGTH = 262_144;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private AioaGraphUpdateHandler() {
    }

    public static AioaGraphUpdateRequest createRequest(AioaBehaviorGraph graph) {
        return new AioaGraphUpdateRequest(GSON.toJson(graph));
    }

    public static AioaGraphUpdateRequest createWorkspaceRequest(List<AioaBehaviorGraph> graphs, boolean silent) {
        return new AioaGraphUpdateRequest(GSON.toJson(new WorkspaceUpdate(
                "aioa-behavior-workspace", 2, silent, graphs.stream().map(AioaBehaviorGraph::copy).toList())));
    }

    public static void handle(ServerPlayer player, AioaGraphUpdateRequest request) {
        if (!player.isCreative() || request.graphJson() == null || request.graphJson().length() > MAX_GRAPH_JSON_LENGTH) {
            return;
        }
        try {
            JsonObject object = JsonParser.parseString(request.graphJson()).getAsJsonObject();
            if ("aioa-behavior-workspace".equals(string(object, "format"))) {
                handleWorkspace(player, GSON.fromJson(object, WorkspaceUpdate.class));
                return;
            }
            AioaBehaviorGraph graph = GSON.fromJson(object, AioaBehaviorGraph.class).sanitize();
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

    private static void handleWorkspace(ServerPlayer player, WorkspaceUpdate update) {
        if (update == null || update.version < 1 || update.version > 2 || update.graphs == null || update.graphs.size() > 128) {
            player.displayClientMessage(Component.literal("AIOA workspace data was invalid."), true);
            return;
        }
        List<AioaBehaviorGraph> graphs = update.graphs.stream().map(AioaBehaviorGraph::sanitize).toList();
        for (AioaBehaviorGraph graph : graphs) {
            List<String> issues = AioaBehaviorValidator.validate(graph);
            if (!issues.isEmpty()) {
                if (!update.silent) player.displayClientMessage(Component.literal("AIOA graph rejected: " + issues.get(0)), true);
                return;
            }
        }

        AioaConfig config = AioaConfigManager.getConfigCopy();
        if (player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            config.behaviorGraphs = graphs.stream().map(AioaBehaviorGraph::copy)
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        } else {
            for (AioaBehaviorGraph graph : graphs) {
                if (graph.scope != AioaBehaviorGraph.Scope.SINGLE_ENTITY) continue;
                config.behaviorGraphs.removeIf(existing -> existing.id.equals(graph.id));
                config.behaviorGraphs.add(graph.copy());
            }
        }
        AioaConfigManager.save(config);
        if (!update.silent) player.displayClientMessage(Component.literal("Applied " + graphs.size() + " behavior graph(s)."), true);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private static final class WorkspaceUpdate {
        String format;
        int version;
        boolean silent;
        List<AioaBehaviorGraph> graphs;

        WorkspaceUpdate(String format, int version, boolean silent, List<AioaBehaviorGraph> graphs) {
            this.format = format;
            this.version = version;
            this.silent = silent;
            this.graphs = graphs;
        }
    }
}
