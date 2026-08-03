package com.flubburr.aioa.behavior;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AioaBehaviorGraph {
    public String id = UUID.randomUUID().toString();
    public String name = "New behavior";
    public boolean enabled = true;
    public Scope scope = Scope.MANAGED_MOBS;
    public String selector = "";
    public List<Node> nodes = new ArrayList<>();
    public List<Edge> edges = new ArrayList<>();
    public EditorLayout editorLayout = new EditorLayout();

    public static AioaBehaviorGraph createStarter() {
        AioaBehaviorGraph graph = new AioaBehaviorGraph();
        graph.name = "Hunt nearby players";
        graph.nodes.add(new Node("base", NodeType.MOB_BASE, -140, 70)
                .parameter("entity", "auto").parameter("health", "20").parameter("damage", "3").parameter("speed", "0.23"));
        graph.nodes.add(new Node("tick", NodeType.ON_TICK, 40, 70));
        graph.nodes.add(new Node("sense", NodeType.FIND_NEAREST_PLAYER, 220, 70).parameter("range", "32"));
        graph.nodes.add(new Node("target", NodeType.SET_TARGET, 400, 70));
        graph.nodes.add(new Node("move", NodeType.MOVE_TO_TARGET, 580, 70).parameter("speed", "1.1"));
        graph.edges.add(new Edge("base", "tick", "next"));
        graph.edges.add(new Edge("tick", "sense", "next"));
        graph.edges.add(new Edge("sense", "target", "found"));
        graph.edges.add(new Edge("target", "move", "next"));
        return graph;
    }

    public AioaBehaviorGraph copy() {
        AioaBehaviorGraph copy = new AioaBehaviorGraph();
        copy.id = this.id;
        copy.name = this.name;
        copy.enabled = this.enabled;
        copy.scope = this.scope;
        copy.selector = this.selector;
        copy.nodes = this.nodes == null ? new ArrayList<>() : this.nodes.stream().map(Node::copy).toList();
        copy.nodes = new ArrayList<>(copy.nodes);
        copy.edges = this.edges == null ? new ArrayList<>() : this.edges.stream().map(Edge::copy).toList();
        copy.edges = new ArrayList<>(copy.edges);
        copy.editorLayout = this.editorLayout == null ? new EditorLayout() : this.editorLayout.copy();
        return copy.sanitize();
    }

    public AioaBehaviorGraph sanitize() {
        if (this.id == null || this.id.isBlank()) this.id = UUID.randomUUID().toString();
        if (this.name == null || this.name.isBlank()) this.name = "Untitled behavior";
        if (this.scope == null) this.scope = Scope.MANAGED_MOBS;
        if (this.selector == null) this.selector = "";
        if (this.nodes == null) this.nodes = new ArrayList<>();
        if (this.edges == null) this.edges = new ArrayList<>();
        if (this.editorLayout == null) this.editorLayout = new EditorLayout();
        this.nodes.removeIf(node -> node == null || node.id == null || node.type == null);
        this.nodes.forEach(Node::sanitize);
        this.edges.removeIf(edge -> edge == null || edge.from == null || edge.to == null);
        if (this.nodes.stream().noneMatch(node -> node.type == NodeType.MOB_BASE)) {
            Node base = new Node("base_" + UUID.randomUUID().toString().substring(0, 8), NodeType.MOB_BASE, -140, 70)
                    .parameter("entity", "auto").parameter("health", "20").parameter("damage", "3").parameter("speed", "0.23");
            this.nodes.add(0, base);
            this.nodes.stream().filter(node -> node.type == NodeType.ON_TICK).findFirst()
                    .ifPresent(entry -> this.edges.add(new Edge(base.id, entry.id, "next")));
        }
        return this;
    }

    public enum Scope {
        MANAGED_MOBS,
        ENTITY_TYPE,
        ENTITY_TAG,
        SINGLE_ENTITY,
        ALL_MOBS
    }

    public enum NodeType {
        MOB_BASE("Base", "Required root for this mob. Auto-detects the selected mob and defines base health, damage, and movement speed."),
        ON_TICK("Events", "Runs the graph once per AI tick."),
        ON_FIRST_TICK("Events", "Runs only when a newly created mob begins ticking."),
        EVERY_TICKS("Events", "Continues at a configurable tick interval."),
        EVERY_SECONDS("Events", "Continues at a configurable real-time interval measured in seconds."),
        DELAY_TICKS("Flow", "Waits for a precise number of ticks before continuing."),
        RANDOM_CHANCE("Flow", "Continues through success or fail using a percentage chance."),
        FIND_NEAREST_PLAYER("Sensing", "Finds the nearest valid player."),
        FIND_PLAYER_NAME("Sensing", "Finds the nearest online player whose name matches the selected player."),
        FIND_NEAREST_ANIMAL("Sensing", "Finds the nearest animal."),
        FIND_NEAREST_MOB("Sensing", "Finds the nearest other mob."),
        FIND_ENTITY_TYPE("Sensing", "Finds the nearest mob matching a registry id."),
        HAS_TARGET("Conditions", "Branches based on whether this mob has a live target."),
        TARGET_IN_RANGE("Conditions", "Branches when the current target is within range."),
        HEALTH_BELOW("Conditions", "Branches when health is below a percentage."),
        CAN_SEE_TARGET("Conditions", "Branches when line of sight to the target is clear."),
        IS_DAYTIME("Conditions", "Branches based on world daytime."),
        IS_ON_GROUND("Conditions", "Branches based on whether the mob is grounded."),
        WAS_HURT("Conditions", "Branches while the mob's hurt animation is active."),
        SET_TARGET("Targeting", "Uses the sensed entity as this mob's attack target."),
        CLEAR_TARGET("Targeting", "Clears the current attack target."),
        MOVE_TO_TARGET("Movement", "Pathfinds toward the current or sensed target."),
        FLEE_TARGET("Movement", "Moves away from the current or sensed target."),
        WALL_CLIMB("Movement", "Lets the mob climb while pursuing a target above it."),
        WALK_BLOCKS("Movement", "Walks a fixed number of blocks in its facing direction."),
        WANDER("Movement", "Chooses a nearby random destination."),
        ROTATE_DEGREES("Movement", "Rotates the mob by an exact number of degrees."),
        STRAFE("Movement", "Applies forward and sideways movement."),
        JUMP("Movement", "Makes the mob jump."),
        TELEPORT_RELATIVE("Movement", "Teleports by a clamped relative offset."),
        LOOK_AT_TARGET("Movement", "Turns the mob toward the target."),
        ATTACK_TARGET("Combat", "Immediately performs a normal mob attack."),
        KNOCKBACK_TARGET("Combat", "Pushes the current target away."),
        SET_AGGRESSIVE("Combat", "Changes the mob's aggressive state."),
        SHARE_TARGET("Interaction", "Shares the current target with nearby graph mobs."),
        STOP_MOVING("Movement", "Stops the mob's current navigation."),
        SET_NO_AI("State", "Enables or disables vanilla AI."),
        SET_PERSISTENT("State", "Prevents the mob from naturally despawning."),
        SET_GLOWING("State", "Toggles the glowing outline."),
        SET_SILENT("State", "Toggles mob sounds."),
        SET_INVULNERABLE("State", "Toggles damage immunity."),
        SET_CUSTOM_NAME("State", "Sets a visible custom mob name."),
        SET_MAX_HEALTH("Attributes", "Changes maximum health and safely clamps current health."),
        SET_ATTACK_DAMAGE("Attributes", "Changes base melee attack damage when the mob supports it."),
        SET_MOVEMENT_SPEED("Attributes", "Changes the mob's base movement speed."),
        EQUIP_ITEM("Equipment", "Equips a registered item into a chosen equipment slot."),
        SPAWN_MOB("World", "Safely spawns another configured mob nearby with a cooldown."),
        PLAY_SOUND("Effects", "Plays a registered sound at the mob."),
        SAY_IN_CHAT("Presentation", "Sends a formatted creator message to nearby players."),
        PARTICLE_PATTERN("Presentation", "Draws a circle, burst, or spiral particle pattern around the mob."),
        HEAL_SELF("Combat", "Heals the graph mob by a configurable amount."),
        SET_VELOCITY("Movement", "Sets an exact X/Y/Z movement vector for launches, slides, and scripted motion."),
        ADD_TAG("State", "Adds a scoreboard tag that other graphs can test."),
        REMOVE_TAG("State", "Removes a scoreboard tag from the graph mob."),
        HAS_TAG("Conditions", "Branches based on whether the graph mob has a scoreboard tag."),
        SET_VARIABLE("Values", "Stores a named numeric value for later value nodes in this execution."),
        MATH_VARIABLE("Values", "Adds, subtracts, multiplies, divides, minimums, or maximums a stored value."),
        COMPARE_VARIABLE("Conditions", "Branches by comparing a stored value with a numeric threshold."),
        SCRIPT("Scripting", "Runs a safe creator script made from documented wait, say, rotate, glow, and stop commands."),
        SET_BODY_ROTATION("Model", "Separately changes the mob body's display rotation."),
        SET_HEAD_ROTATION("Model", "Separately changes the mob head's display rotation."),
        COMMENT("Organization", "A note for creators; it does not execute.");

        public final String category;
        public final String help;

        NodeType(String category, String help) {
            this.category = category;
            this.help = help;
        }
    }

    public static final class Node {
        public String id;
        public NodeType type;
        public int x;
        public int y;
        public Map<String, String> parameters = new LinkedHashMap<>();

        public Node() {
        }

        public Node(String id, NodeType type, int x, int y) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
        }

        public Node parameter(String key, String value) {
            this.parameters.put(key, value);
            return this;
        }

        private void sanitize() {
            if (this.parameters == null) this.parameters = new LinkedHashMap<>();
            this.x = Math.max(-4096, Math.min(4096, this.x));
            this.y = Math.max(-4096, Math.min(4096, this.y));
        }

        public Node copy() {
            Node copy = new Node(this.id, this.type, this.x, this.y);
            copy.parameters = this.parameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(this.parameters);
            return copy;
        }
    }

    public static final class Edge {
        public String from;
        public String to;
        public String output = "next";

        public Edge() {
        }

        public Edge(String from, String to, String output) {
            this.from = from;
            this.to = to;
            this.output = output;
        }

        public Edge copy() {
            return new Edge(this.from, this.to, this.output);
        }
    }

    public static final class EditorLayout {
        public boolean initialized;
        public int screenWidth;
        public int screenHeight;
        public int workspaceX, workspaceY, workspaceWidth, workspaceHeight;
        public int viewportX, viewportY, viewportWidth, viewportHeight;
        public int paletteX, paletteY, paletteWidth, paletteHeight;
        public int inspectorX, inspectorY, inspectorWidth, inspectorHeight;
        public int parametersX, parametersY, parametersWidth, parametersHeight;
        public int helpX, helpY, helpWidth, helpHeight;
        public boolean paletteCollapsed, inspectorCollapsed, parametersCollapsed, showHelp = true;
        public int canvasPanX, canvasPanY;
        public double canvasZoom = 1.0D;

        public EditorLayout copy() {
            EditorLayout copy = new EditorLayout();
            copy.initialized = this.initialized;
            copy.screenWidth = this.screenWidth; copy.screenHeight = this.screenHeight;
            copy.workspaceX = this.workspaceX; copy.workspaceY = this.workspaceY;
            copy.workspaceWidth = this.workspaceWidth; copy.workspaceHeight = this.workspaceHeight;
            copy.viewportX = this.viewportX; copy.viewportY = this.viewportY;
            copy.viewportWidth = this.viewportWidth; copy.viewportHeight = this.viewportHeight;
            copy.paletteX = this.paletteX; copy.paletteY = this.paletteY;
            copy.paletteWidth = this.paletteWidth; copy.paletteHeight = this.paletteHeight;
            copy.inspectorX = this.inspectorX; copy.inspectorY = this.inspectorY;
            copy.inspectorWidth = this.inspectorWidth; copy.inspectorHeight = this.inspectorHeight;
            copy.parametersX = this.parametersX; copy.parametersY = this.parametersY;
            copy.parametersWidth = this.parametersWidth; copy.parametersHeight = this.parametersHeight;
            copy.helpX = this.helpX; copy.helpY = this.helpY;
            copy.helpWidth = this.helpWidth; copy.helpHeight = this.helpHeight;
            copy.paletteCollapsed = this.paletteCollapsed; copy.inspectorCollapsed = this.inspectorCollapsed;
            copy.parametersCollapsed = this.parametersCollapsed; copy.showHelp = this.showHelp;
            copy.canvasPanX = this.canvasPanX; copy.canvasPanY = this.canvasPanY; copy.canvasZoom = this.canvasZoom;
            return copy;
        }
    }
}
