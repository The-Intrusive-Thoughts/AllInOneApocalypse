package com.flubburr.aioa.api;

import com.flubburr.aioa.behavior.AioaBehaviorGraph;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stable common API for data-driven bosses and best-effort modded-mob graph import. */
public final class AioaBehaviorApi {
    private static final Map<ResourceLocation, AioaBehaviorGraph> RUNTIME_GRAPHS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, MobGraphContributor> CONTRIBUTORS = new ConcurrentHashMap<>();

    private AioaBehaviorApi() {}

    public static void registerGraph(ResourceLocation id, AioaBehaviorGraph graph) {
        if (id == null || graph == null) throw new IllegalArgumentException("Graph id and graph are required");
        AioaBehaviorGraph copy = graph.copy();
        copy.id = id.toString();
        RUNTIME_GRAPHS.put(id, copy);
    }

    public static void unregisterGraph(ResourceLocation id) {
        if (id != null) RUNTIME_GRAPHS.remove(id);
    }

    public static void registerMobContributor(ResourceLocation id, MobGraphContributor contributor) {
        if (id == null || contributor == null) throw new IllegalArgumentException("Contributor id and callback are required");
        CONTRIBUTORS.put(id, contributor);
    }

    public static List<AioaBehaviorGraph> registeredGraphs() {
        return RUNTIME_GRAPHS.values().stream().map(AioaBehaviorGraph::copy).toList();
    }

    /**
     * Builds an editable approximation from observable state. Java goals are intentionally not decompiled;
     * mods can make their own behavior exact by registering a contributor.
     */
    public static AioaBehaviorGraph approximateMob(Mob mob) {
        AioaBehaviorGraph graph = new AioaBehaviorGraph();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        graph.name = "Imported " + mob.getDisplayName().getString();
        graph.scope = AioaBehaviorGraph.Scope.SINGLE_ENTITY;
        graph.selector = mob.getUUID().toString();

        AioaBehaviorGraph.Node base = new AioaBehaviorGraph.Node("base", AioaBehaviorGraph.NodeType.MOB_BASE, -180, 60)
                .parameter("entity", typeId == null ? "auto" : typeId.toString())
                .parameter("health", decimal(mob.getMaxHealth()))
                .parameter("damage", decimal(attribute(mob, Attributes.ATTACK_DAMAGE, 0)))
                .parameter("speed", decimal(attribute(mob, Attributes.MOVEMENT_SPEED, 0.23)));
        AioaBehaviorGraph.Node tick = new AioaBehaviorGraph.Node("tick", AioaBehaviorGraph.NodeType.ON_TICK, 20, 60);
        graph.nodes.add(base);
        graph.nodes.add(tick);
        graph.edges.add(new AioaBehaviorGraph.Edge(base.id, tick.id, "next"));

        int y = 155;
        if (mob.isAggressive()) y = append(graph, tick, new AioaBehaviorGraph.Node("aggressive", AioaBehaviorGraph.NodeType.SET_AGGRESSIVE, 220, y).parameter("value", "true"), y);
        if (mob.isNoAi()) y = append(graph, tick, new AioaBehaviorGraph.Node("no_ai", AioaBehaviorGraph.NodeType.SET_NO_AI, 220, y).parameter("value", "true"), y);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            boolean armorSlot = slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                    || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
            AioaBehaviorGraph.NodeType nodeType = armorSlot
                    ? AioaBehaviorGraph.NodeType.EQUIP_ARMOR : AioaBehaviorGraph.NodeType.EQUIP_ITEM;
            y = append(graph, tick, new AioaBehaviorGraph.Node("equipment_" + slot.getName(), nodeType, 220, y)
                    .parameter("item", itemId.toString()).parameter("slot", slot.name()).parameter("dropChance", "0"), y);
        }
        graph.nodes.add(new AioaBehaviorGraph.Node("import_note", AioaBehaviorGraph.NodeType.COMMENT, 20, y + 25)
                .parameter("text", "Approximation from live state. Modded Java goals stay opaque; install/register that mod's AIOA contributor for exact custom nodes."));
        for (MobGraphContributor contributor : CONTRIBUTORS.values()) {
            try { contributor.contribute(mob, graph); } catch (RuntimeException ignored) { }
        }
        return graph.sanitize();
    }

    private static int append(AioaBehaviorGraph graph, AioaBehaviorGraph.Node tick, AioaBehaviorGraph.Node node, int y) {
        graph.nodes.add(node);
        graph.edges.add(new AioaBehaviorGraph.Edge(tick.id, node.id, "next"));
        return y + 90;
    }

    private static double attribute(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attribute, double fallback) {
        return mob.getAttribute(attribute) == null ? fallback : mob.getAttributeValue(attribute);
    }

    private static String decimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @FunctionalInterface
    public interface MobGraphContributor {
        void contribute(Mob mob, AioaBehaviorGraph graph);
    }
}
