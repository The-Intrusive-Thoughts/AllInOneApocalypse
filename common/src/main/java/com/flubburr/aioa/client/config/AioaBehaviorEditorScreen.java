package com.flubburr.aioa.client.config;

import com.flubburr.aioa.behavior.AioaBehaviorGraph;
import com.flubburr.aioa.behavior.AioaBehaviorValidator;
import com.flubburr.aioa.behavior.AioaGraphLibrary;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.network.AioaClientNetworking;
import com.flubburr.aioa.compat.AioaEntityHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AioaBehaviorEditorScreen extends AioaAnimatedScreen {
    private static final int WINDOW_WIDTH = 920;
    private static final int WINDOW_HEIGHT = 520;
    private static final int PALETTE_WIDTH = 170;
    private static final int INSPECTOR_WIDTH = 190;
    private static final int NODE_WIDTH = 142;
    private static final int NODE_HEIGHT = 46;

    private final Screen parent;
    private final AioaConfig editableConfig;
    private final List<AioaBehaviorGraph> graphs = new ArrayList<>();
    private final Deque<AioaBehaviorGraph> undo = new ArrayDeque<>();
    private final Deque<AioaBehaviorGraph> redo = new ArrayDeque<>();
    private AioaBehaviorGraph graph;
    private int graphIndex;
    private int viewportX;
    private int viewportY;
    private int viewportWidth = 178;
    private int viewportHeight = 118;
    private AioaBehaviorGraph.Node selected;
    private AioaBehaviorGraph.Node linkStart;
    private String linkOutput = "next";
    private int windowX;
    private int windowY;
    private int canvasPanX;
    private int canvasPanY;
    private double canvasZoom = 1.0D;
    private int palettePage;
    private long lastNodeClickAt;
    private String lastNodeClickId = "";
    private int parameterIndex;
    private boolean showContextMenu;
    private int contextMenuX;
    private int contextMenuY;
    private boolean draggingWindow;
    private boolean draggingNode;
    private boolean draggingCanvas;
    private boolean draggingLink;
    private boolean draggingViewport;
    private boolean resizingViewport;
    private double linkMouseX;
    private double linkMouseY;
    private boolean draggingHelp;
    private boolean resizingHelp;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean showHelp = true;
    private int helpX;
    private int helpY;
    private int helpWidth = 470;
    private int helpHeight = 112;
    private int helpPage;
    private String status = "Ready";
    private EditBox graphName;
    private EditBox selector;
    private EditBox parameterKey;
    private EditBox parameterValue;
    private Button scopeButton;

    private AioaBehaviorEditorScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("AIOA Behavior Graph Studio"));
        this.parent = parent;
        this.editableConfig = editableConfig;
        if (editableConfig.behaviorGraphs.isEmpty()) {
            this.graphs.add(AioaBehaviorGraph.createStarter());
        } else {
            editableConfig.behaviorGraphs.forEach(graph -> this.graphs.add(graph.copy()));
        }
        this.graph = this.graphs.get(0);
    }

    public static Screen create(Screen parent, AioaConfig editableConfig) {
        return new AioaBehaviorEditorScreen(parent, editableConfig);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.windowX = this.windowX == 0 ? Math.max(8, (this.width - Math.min(WINDOW_WIDTH, this.width - 16)) / 2) : this.windowX;
        this.windowY = this.windowY == 0 ? Math.max(8, (this.height - Math.min(WINDOW_HEIGHT, this.height - 16)) / 2) : this.windowY;
        this.helpX = this.helpX == 0 ? this.windowX + PALETTE_WIDTH + 18 : this.helpX;
        this.helpY = this.helpY == 0 ? this.windowY + windowHeight() - 118 : this.helpY;
        this.viewportX = this.viewportX == 0 ? this.canvasRight() - this.viewportWidth - 8 : this.viewportX;
        this.viewportY = this.viewportY == 0 ? this.canvasTop() + 8 : this.viewportY;
        rebuildEditorWidgets();
    }

    private int windowWidth() { return Math.min(WINDOW_WIDTH, this.width - 16); }
    private int windowHeight() { return Math.min(WINDOW_HEIGHT, this.height - 16); }
    private int inspectorLeft() { return this.windowX + windowWidth() - INSPECTOR_WIDTH; }
    private int canvasLeft() { return this.windowX + PALETTE_WIDTH; }
    private int canvasRight() { return inspectorLeft(); }
    private int canvasTop() { return this.windowY + 58; }
    private int canvasBottom() { return this.windowY + windowHeight() - 28; }

    private void rebuildEditorWidgets() {
        this.clearWidgets();
        int toolbarY = this.windowY + 28;
        int x = this.windowX + 8;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 66, "New", button -> newGraph()));
        x += 70;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 66, "Undo", button -> undo()));
        x += 70;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 66, "Redo", button -> redo()));
        x += 70;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 82, "Validate", button -> validateGraph()));
        x += 86;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 72, this.linkStart == null ? "Link" : "Linking", button -> {
            this.linkStart = this.selected;
            if (this.linkStart != null) this.linkOutput = defaultOutput(this.linkStart.type);
            this.status = this.linkStart == null ? "Select a source node first." : "Click a destination node.";
            rebuildEditorWidgets();
        }));
        x += 76;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 76, "Port: " + this.linkOutput, button -> {
            this.linkOutput = nextOutput(this.selected == null ? null : this.selected.type, this.linkOutput);
            rebuildEditorWidgets();
        }));
        x += 80;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 66, "Delete", button -> deleteSelected()));
        x += 70;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 58, Component.translatable("aioa.editor.help").getString(), button -> this.showHelp = !this.showHelp));
        x += 62;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 58, Component.translatable("aioa.editor.docs").getString(), button ->
                this.transitionTo(new AioaDocsScreen(this))));

        int right = this.windowX + windowWidth() - 8;
        this.addRenderableWidget(AioaScreenUtil.button(right - 152, toolbarY, 72, "Apply", button -> applyAndClose()));
        this.addRenderableWidget(AioaScreenUtil.button(right - 76, toolbarY, 72, "Close", button -> this.onClose()));

        int inspectorX = inspectorLeft() + 10;
        int inspectorWidth = INSPECTOR_WIDTH - 20;
        this.graphName = new EditBox(this.font, inspectorX, this.windowY + 78, inspectorWidth, 22, Component.literal("Graph name"));
        this.graphName.setValue(this.graph.name);
        this.addRenderableWidget(this.graphName);
        this.scopeButton = this.addRenderableWidget(AioaScreenUtil.button(inspectorX, this.windowY + 106, inspectorWidth,
                "Scope: " + friendly(this.graph.scope), button -> {
                    snapshot();
                    this.graph.scope = next(this.graph.scope, AioaBehaviorGraph.Scope.values());
                    button.setMessage(Component.literal("Scope: " + friendly(this.graph.scope)));
                }));
        this.selector = new EditBox(this.font, inspectorX, this.windowY + 136, inspectorWidth, 22, Component.literal("Scope selector"));
        this.selector.setValue(this.graph.selector);
        this.selector.setHint(Component.literal("entity id, tag, or UUID"));
        this.addRenderableWidget(this.selector);

        this.parameterKey = new EditBox(this.font, inspectorX, this.windowY + 218, inspectorWidth, 22, Component.literal("Parameter name"));
        this.parameterKey.setHint(Component.literal("range / speed / ticks..."));
        this.addRenderableWidget(this.parameterKey);
        this.parameterValue = new EditBox(this.font, inspectorX, this.windowY + 246, inspectorWidth, 22, Component.literal("Parameter value"));
        this.parameterValue.setHint(Component.literal("value"));
        this.addRenderableWidget(this.parameterValue);
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX, this.windowY + 274, inspectorWidth, "Set parameter", button -> setParameter()));
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX, this.windowY + 306, inspectorWidth, "Spawn a mob...", button ->
                this.transitionTo(AioaSpawnStudioScreen.create(this))));
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX, this.windowY + 338, inspectorWidth, "Browse mob types...", button ->
                this.transitionTo(new AioaEntityPickerScreen(this, "Select Behavior Mob", AioaScreenUtil.allEntityIds(),
                        "Choose an entity type for this graph or a node parameter.", "Use Selected Mob", id -> {
                    snapshot();
                    if (this.selected != null && (this.selected.type == AioaBehaviorGraph.NodeType.FIND_ENTITY_TYPE
                            || this.selected.type == AioaBehaviorGraph.NodeType.SPAWN_MOB)) {
                        this.selected.parameters.put("entity", id.toString());
                        loadSelectedParameter(false);
                        this.status = "Node mob set to " + AioaScreenUtil.entityDisplayName(id) + ".";
                    } else {
                        this.graph.scope = AioaBehaviorGraph.Scope.ENTITY_TYPE;
                        this.graph.selector = id.toString();
                        this.selector.setValue(this.graph.selector);
                        this.scopeButton.setMessage(Component.literal("Scope: " + friendly(this.graph.scope)));
                        this.status = "Graph mob set to " + AioaScreenUtil.entityDisplayName(id) + ".";
                    }
                }))));
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX, this.windowY + 370, inspectorWidth, "Pick in world (right-click)", button ->
                AioaMobSelectionController.arm(this)));
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX, this.windowY + 402, (inspectorWidth - 6) / 2, "< Graph", button -> switchGraph(-1)));
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX + (inspectorWidth + 6) / 2, this.windowY + 402, (inspectorWidth - 6) / 2, "Graph >", button -> switchGraph(1)));
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX, this.windowY + 434, (inspectorWidth - 6) / 2, "Export", button -> exportGraph()));
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX + (inspectorWidth + 6) / 2, this.windowY + 434, (inspectorWidth - 6) / 2, "Load Newest", button -> loadNewestGraph()));

        int paletteX = this.windowX + 10;
        int paletteY = this.windowY + 82;
        AioaBehaviorGraph.NodeType[] types = AioaBehaviorGraph.NodeType.values();
        int pageSize = Math.max(4, (windowHeight() - 150) / 30);
        int start = this.palettePage * pageSize;
        for (int i = start; i < Math.min(types.length, start + pageSize); i++) {
            AioaBehaviorGraph.NodeType type = types[i];
            this.addRenderableWidget(AioaScreenUtil.button(paletteX, paletteY, PALETTE_WIDTH - 20, type.name().replace('_', ' '), button -> addNode(type)));
            paletteY += 30;
        }
        this.addRenderableWidget(AioaScreenUtil.button(paletteX, this.windowY + windowHeight() - 56, 70, "< Prev", button -> {
            this.palettePage = Math.max(0, this.palettePage - 1); rebuildEditorWidgets();
        }));
        this.addRenderableWidget(AioaScreenUtil.button(paletteX + 76, this.windowY + windowHeight() - 56, 70, "Next >", button -> {
            if ((this.palettePage + 1) * pageSize < types.length) this.palettePage++;
            rebuildEditorWidgets();
        }));
    }

    private void newGraph() {
        syncFields();
        this.graph = AioaBehaviorGraph.createStarter();
        this.graph.name = "Behavior " + (this.graphs.size() + 1);
        this.graphs.add(this.graph);
        this.graphIndex = this.graphs.size() - 1;
        this.selected = null;
        this.undo.clear();
        this.redo.clear();
        this.status = "Created graph " + (this.graphIndex + 1) + " of " + this.graphs.size() + ".";
        rebuildEditorWidgets();
    }

    private void switchGraph(int direction) {
        if (this.graphs.size() < 2) {
            this.status = "Only one graph exists. Use New to add another.";
            return;
        }
        syncFields();
        this.graphIndex = Math.floorMod(this.graphIndex + direction, this.graphs.size());
        this.graph = this.graphs.get(this.graphIndex);
        this.selected = null;
        this.linkStart = null;
        this.undo.clear();
        this.redo.clear();
        this.status = "Graph " + (this.graphIndex + 1) + " of " + this.graphs.size() + ": " + this.graph.name;
        rebuildEditorWidgets();
    }

    private void selectGraph(int index) {
        if (index < 0 || index >= this.graphs.size() || index == this.graphIndex) return;
        syncFields();
        this.graphIndex = index;
        this.graph = this.graphs.get(index);
        this.selected = null;
        this.linkStart = null;
        this.undo.clear();
        this.redo.clear();
        this.status = "Opened graph tab: " + this.graph.name;
        rebuildEditorWidgets();
    }

    private void exportGraph() {
        syncFields();
        List<String> issues = AioaBehaviorValidator.validate(this.graph);
        if (!issues.isEmpty()) {
            this.status = "Export blocked: " + issues.get(0);
            return;
        }
        try {
            this.status = "Saved " + AioaGraphLibrary.exportGraph(this.graph, this.editableConfig).getFileName() + ".";
        } catch (java.io.IOException exception) {
            this.status = "Could not export graph: " + exception.getMessage();
        }
    }

    private void loadNewestGraph() {
        try {
            var loaded = AioaGraphLibrary.loadNewest(this.editableConfig);
            if (loaded.isEmpty()) {
                this.status = "No .aioagraph files found in the configured graph library.";
                return;
            }
            this.graph = loaded.get();
            this.graphs.add(this.graph);
            this.graphIndex = this.graphs.size() - 1;
            this.selected = null;
            this.status = "Loaded graph as a new tab.";
            rebuildEditorWidgets();
        } catch (java.io.IOException exception) {
            this.status = "Could not load graph: " + exception.getMessage();
        }
    }

    private void addNode(AioaBehaviorGraph.NodeType type) {
        if (type == AioaBehaviorGraph.NodeType.MOB_BASE && this.graph.nodes.stream().anyMatch(node -> node.type == type)) {
            this.status = "This graph already has its required Base Mob node.";
            return;
        }
        snapshot();
        AioaBehaviorGraph.Node node = new AioaBehaviorGraph.Node("node_" + UUID.randomUUID().toString().substring(0, 8), type,
                50 - this.canvasPanX + (this.graph.nodes.size() % 3) * 165,
                70 - this.canvasPanY + (this.graph.nodes.size() / 3) * 68);
        this.graph.nodes.add(node);
        applyDefaultParameters(node);
        this.selected = node;
        this.status = "Added " + friendly(type) + ".";
        loadSelectedParameter(false);
    }

    private void deleteSelected() {
        if (this.selected == null) return;
        if (this.selected.type == AioaBehaviorGraph.NodeType.MOB_BASE) {
            this.status = "Base Mob is required and cannot be deleted.";
            return;
        }
        snapshot();
        String id = this.selected.id;
        this.graph.nodes.removeIf(node -> node.id.equals(id));
        this.graph.edges.removeIf(edge -> edge.from.equals(id) || edge.to.equals(id));
        this.selected = null;
        this.linkStart = null;
        this.status = "Node deleted.";
    }

    private void duplicateSelected() {
        if (this.selected == null) return;
        if (this.selected.type == AioaBehaviorGraph.NodeType.MOB_BASE) {
            this.status = "Base Mob is unique and cannot be duplicated.";
            return;
        }
        snapshot();
        AioaBehaviorGraph.Node copy = this.selected.copy();
        copy.id = "node_" + UUID.randomUUID().toString().substring(0, 8);
        copy.x += 28;
        copy.y += 28;
        this.graph.nodes.add(copy);
        this.selected = copy;
        this.status = "Node duplicated. Drag it into position.";
    }

    private void fitGraph() {
        if (this.graph.nodes.isEmpty()) {
            this.canvasZoom = 1.0D;
            this.canvasPanX = 0;
            this.canvasPanY = 0;
            return;
        }
        int minX = this.graph.nodes.stream().mapToInt(node -> node.x).min().orElse(0);
        int minY = this.graph.nodes.stream().mapToInt(node -> node.y).min().orElse(0);
        int maxX = this.graph.nodes.stream().mapToInt(node -> node.x + NODE_WIDTH).max().orElse(NODE_WIDTH);
        int maxY = this.graph.nodes.stream().mapToInt(node -> node.y + NODE_HEIGHT).max().orElse(NODE_HEIGHT);
        double fitX = (canvasRight() - canvasLeft() - 36.0D) / Math.max(1, maxX - minX);
        double fitY = (canvasBottom() - canvasTop() - 36.0D) / Math.max(1, maxY - minY);
        this.canvasZoom = clampZoom(Math.min(fitX, fitY));
        this.canvasPanX = 18 - (int) Math.round(minX * this.canvasZoom);
        this.canvasPanY = 18 - (int) Math.round(minY * this.canvasZoom);
        this.status = "Graph fitted at " + zoomPercent() + "% (fixed Scale 2 UI).";
    }

    private void setParameter() {
        if (this.selected == null || this.parameterKey.getValue().isBlank()) {
            this.status = "Select a node and enter a parameter name.";
            return;
        }
        snapshot();
        this.selected.parameters.put(this.parameterKey.getValue().trim(), this.parameterValue.getValue().trim());
        this.status = "Parameter updated.";
    }

    private void loadSelectedParameter(boolean advance) {
        if (this.selected == null || this.selected.parameters.isEmpty()) return;
        List<String> keys = new ArrayList<>(this.selected.parameters.keySet());
        this.parameterIndex = advance ? (this.parameterIndex + 1) % keys.size() : 0;
        String key = keys.get(this.parameterIndex);
        this.parameterKey.setValue(key);
        this.parameterValue.setValue(this.selected.parameters.getOrDefault(key, ""));
        this.parameterValue.setFocused(true);
        this.status = "Editing " + friendly(this.selected.type) + " / " + key + ". Press Enter or Set parameter to apply.";
    }

    private void connect(AioaBehaviorGraph.Node from, AioaBehaviorGraph.Node to) {
        if (from == null || to == null || from == to) {
            this.status = "A node cannot connect into itself.";
            return;
        }
        boolean duplicate = this.graph.edges.stream().anyMatch(edge -> edge.from.equals(from.id)
                && edge.to.equals(to.id) && edge.output.equals(this.linkOutput));
        if (duplicate) {
            this.status = "That connection already exists.";
            return;
        }
        snapshot();
        this.graph.edges.add(new AioaBehaviorGraph.Edge(from.id, to.id, this.linkOutput));
        this.status = "Connected " + friendly(from.type) + " to " + friendly(to.type) + ".";
    }

    private void snapshot() {
        this.undo.push(this.graph.copy());
        while (this.undo.size() > 40) this.undo.removeLast();
        this.redo.clear();
    }

    private void undo() {
        if (this.undo.isEmpty()) return;
        this.redo.push(this.graph.copy());
        this.graph = this.undo.pop();
        this.graphs.set(this.graphIndex, this.graph);
        this.selected = null;
        this.status = "Undo";
        rebuildEditorWidgets();
    }

    private void redo() {
        if (this.redo.isEmpty()) return;
        this.undo.push(this.graph.copy());
        this.graph = this.redo.pop();
        this.graphs.set(this.graphIndex, this.graph);
        this.selected = null;
        this.status = "Redo";
        rebuildEditorWidgets();
    }

    private void validateGraph() {
        syncFields();
        List<String> issues = AioaBehaviorValidator.validate(this.graph);
        this.status = issues.isEmpty() ? "Graph is valid and ready to run." : issues.get(0) + (issues.size() > 1 ? " (+" + (issues.size() - 1) + ")" : "");
    }

    void bindWorldSelectedMob(Mob mob) {
        snapshot();
        this.graph.scope = AioaBehaviorGraph.Scope.SINGLE_ENTITY;
        this.graph.selector = mob.getUUID().toString();
        this.status = "Bound graph to " + mob.getDisplayName().getString() + ".";
        rebuildEditorWidgets();
    }

    private void syncFields() {
        this.graph.name = this.graphName.getValue().trim();
        this.graph.selector = this.selector.getValue().trim();
        this.graph.sanitize();
    }

    private void applyAndClose() {
        syncFields();
        List<String> issues = AioaBehaviorValidator.validate(this.graph);
        if (!issues.isEmpty()) {
            this.status = "Cannot apply: " + issues.get(0);
            return;
        }
        this.editableConfig.behaviorGraphs = this.graphs.stream().map(AioaBehaviorGraph::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            for (AioaBehaviorGraph savedGraph : this.graphs) {
                AioaClientNetworking.sendGraphUpdate(AioaGraphUpdateHandler.createRequest(savedGraph));
            }
        }
        this.transitionTo(this.parent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (this.showContextMenu && contextMenuClicked(mouseX, mouseY, button)) return true;
        this.showContextMenu = false;
        int visibleHelpWidth = Math.min(this.helpWidth, this.width - this.helpX);
        int visibleHelpHeight = Math.min(this.helpHeight, this.height - this.helpY);
        if (button == 0 && mouseY >= this.windowY + 2 && mouseY <= this.windowY + 22) {
            int tabStart = this.windowX + 330;
            int visibleTabs = Math.min(5, this.graphs.size());
            for (int i = 0; i < visibleTabs; i++) {
                if (mouseX >= tabStart + i * 92 && mouseX < tabStart + i * 92 + 88) {
                    selectGraph(i);
                    return true;
                }
            }
        }
        if (button == 0 && mouseX >= this.viewportX + this.viewportWidth - 12 && mouseX <= this.viewportX + this.viewportWidth
                && mouseY >= this.viewportY + this.viewportHeight - 12 && mouseY <= this.viewportY + this.viewportHeight) {
            this.resizingViewport = true;
            return true;
        }
        if (button == 0 && mouseX >= this.viewportX && mouseX <= this.viewportX + this.viewportWidth
                && mouseY >= this.viewportY && mouseY <= this.viewportY + 18) {
            this.draggingViewport = true;
            this.dragOffsetX = (int) mouseX - this.viewportX;
            this.dragOffsetY = (int) mouseY - this.viewportY;
            return true;
        }
        if (this.showHelp && mouseX >= this.helpX + visibleHelpWidth - 12 && mouseX <= this.helpX + visibleHelpWidth
                && mouseY >= this.helpY + visibleHelpHeight - 12 && mouseY <= this.helpY + visibleHelpHeight) {
            this.resizingHelp = true;
            return true;
        }
        if (this.showHelp && mouseX >= this.helpX && mouseX <= this.helpX + visibleHelpWidth && mouseY >= this.helpY && mouseY <= this.helpY + 18) {
            if (mouseX >= this.helpX + visibleHelpWidth - 24) {
                this.showHelp = false;
            } else if (mouseX >= this.helpX + visibleHelpWidth - 70) {
                this.helpPage = (this.helpPage + 1) % 3;
            } else if (mouseX >= this.helpX + visibleHelpWidth - 98) {
                this.helpPage = Math.floorMod(this.helpPage - 1, 3);
            } else {
                this.draggingHelp = true;
                this.dragOffsetX = (int) mouseX - this.helpX;
                this.dragOffsetY = (int) mouseY - this.helpY;
            }
            return true;
        }
        if (button == 0 && mouseY >= this.windowY && mouseY <= this.windowY + 24
                && mouseX >= this.windowX && mouseX <= this.windowX + windowWidth()) {
            this.draggingWindow = true;
            this.dragOffsetX = (int) mouseX - this.windowX;
            this.dragOffsetY = (int) mouseY - this.windowY;
            return true;
        }
        if ((button == 0 || button == 1) && insideCanvas(mouseX, mouseY)) {
            AioaBehaviorGraph.Node hit = nodeAt(mouseX, mouseY);
            if (hit != null) {
                if (button == 0 && mouseX >= screenNodeX(hit) + nodeWidth() - 8) {
                    this.selected = hit;
                    this.linkStart = hit;
                    this.linkOutput = defaultOutput(hit.type);
                    this.draggingLink = true;
                    this.linkMouseX = mouseX;
                    this.linkMouseY = mouseY;
                    this.status = "Drag the output port onto another node's input.";
                    return true;
                }
                if (button == 1) {
                    this.selected = hit;
                    this.linkStart = hit;
                    this.linkOutput = defaultOutput(hit.type);
                    this.status = "Quick-link: left-click a destination node.";
                    rebuildEditorWidgets();
                    return true;
                }
                if (this.linkStart != null && this.linkStart != hit) {
                    snapshot();
                    this.graph.edges.add(new AioaBehaviorGraph.Edge(this.linkStart.id, hit.id, this.linkOutput));
                    this.linkStart = null;
                    this.status = "Nodes linked.";
                    rebuildEditorWidgets();
                    return true;
                }
                this.selected = hit;
                long now = System.currentTimeMillis();
                boolean doubleClick = hit.id.equals(this.lastNodeClickId) && now - this.lastNodeClickAt <= 360L;
                this.lastNodeClickId = hit.id;
                this.lastNodeClickAt = now;
                this.draggingNode = true;
                this.dragOffsetX = (int) mouseX - screenNodeX(hit);
                this.dragOffsetY = (int) mouseY - screenNodeY(hit);
                this.status = hit.type.help;
                loadSelectedParameter(doubleClick);
                return true;
            }
            if (button == 1) {
                this.showContextMenu = true;
                this.contextMenuX = Math.min((int) mouseX, this.width - 132);
                this.contextMenuY = Math.min((int) mouseY, this.height - 132);
                return true;
            }
            if (button == 0 || button == 2) {
                this.draggingCanvas = true;
                this.dragOffsetX = (int) mouseX - this.canvasPanX;
                this.dragOffsetY = (int) mouseY - this.canvasPanY;
                this.status = "Moving canvas - release to finish.";
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (this.draggingLink) {
            this.linkMouseX = mouseX;
            this.linkMouseY = mouseY;
            return true;
        }
        if (this.draggingViewport) {
            this.viewportX = Math.max(0, Math.min(this.width - this.viewportWidth, (int) mouseX - this.dragOffsetX));
            this.viewportY = Math.max(0, Math.min(this.height - this.viewportHeight, (int) mouseY - this.dragOffsetY));
            return true;
        }
        if (this.resizingViewport) {
            this.viewportWidth = Math.max(120, Math.min(300, (int) mouseX - this.viewportX));
            this.viewportHeight = Math.max(90, Math.min(220, (int) mouseY - this.viewportY));
            return true;
        }
        if (this.draggingHelp) {
            this.helpX = Math.max(0, Math.min(this.width - 180, (int) mouseX - this.dragOffsetX));
            this.helpY = Math.max(0, Math.min(this.height - 86, (int) mouseY - this.dragOffsetY));
            return true;
        }
        if (this.resizingHelp) {
            this.helpWidth = Math.max(240, Math.min(this.width - this.helpX, (int) mouseX - this.helpX));
            this.helpHeight = Math.max(96, Math.min(this.height - this.helpY, (int) mouseY - this.helpY));
            return true;
        }
        if (this.draggingWindow) {
            this.windowX = Math.max(0, Math.min(this.width - windowWidth(), (int) mouseX - this.dragOffsetX));
            this.windowY = Math.max(0, Math.min(this.height - windowHeight(), (int) mouseY - this.dragOffsetY));
            rebuildEditorWidgets();
            return true;
        }
        if (this.draggingNode && this.selected != null) {
            this.selected.x = (int) Math.round(((int) mouseX - this.dragOffsetX - canvasLeft() - this.canvasPanX) / this.canvasZoom);
            this.selected.y = (int) Math.round(((int) mouseY - this.dragOffsetY - canvasTop() - this.canvasPanY) / this.canvasZoom);
            return true;
        }
        if (this.draggingCanvas) {
            this.canvasPanX = (int) mouseX - this.dragOffsetX;
            this.canvasPanY = (int) mouseY - this.dragOffsetY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (this.draggingLink) {
            connect(this.linkStart, nodeAt(mouseX, mouseY));
            this.draggingLink = false;
            this.linkStart = null;
            rebuildEditorWidgets();
            return true;
        }
        this.draggingWindow = false;
        this.draggingNode = false;
        this.draggingCanvas = false;
        this.draggingHelp = false;
        this.resizingHelp = false;
        this.draggingViewport = false;
        this.resizingViewport = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= this.windowX && mouseX < canvasLeft() && mouseY >= canvasTop() && mouseY <= canvasBottom()) {
            int pageSize = Math.max(4, (windowHeight() - 150) / 30);
            int pageCount = Math.max(1, (AioaBehaviorGraph.NodeType.values().length + pageSize - 1) / pageSize);
            this.palettePage = Math.max(0, Math.min(pageCount - 1, this.palettePage + (verticalAmount < 0 ? 1 : -1)));
            rebuildEditorWidgets();
            return true;
        }
        if (insideCanvas(mouseX, mouseY)) {
            if (!Screen.hasShiftDown()) {
                double oldZoom = this.canvasZoom;
                double graphX = (mouseX - canvasLeft() - this.canvasPanX) / oldZoom;
                double graphY = (mouseY - canvasTop() - this.canvasPanY) / oldZoom;
                this.canvasZoom = clampZoom(oldZoom + Math.copySign(0.1D, verticalAmount));
                this.canvasPanX = (int) Math.round(mouseX - canvasLeft() - graphX * this.canvasZoom);
                this.canvasPanY = (int) Math.round(mouseY - canvasTop() - graphY * this.canvasZoom);
                this.status = "Zoom " + zoomPercent() + "% - the wheel follows the cursor.";
            } else {
                this.canvasPanX += (int) Math.copySign(28, verticalAmount);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean insideCanvas(double x, double y) {
        return x >= canvasLeft() && x <= canvasRight() && y >= canvasTop() && y <= canvasBottom();
    }

    private AioaBehaviorGraph.Node nodeAt(double mouseX, double mouseY) {
        for (int i = this.graph.nodes.size() - 1; i >= 0; i--) {
            AioaBehaviorGraph.Node node = this.graph.nodes.get(i);
            int x = screenNodeX(node);
            int y = screenNodeY(node);
            if (mouseX >= x && mouseX <= x + nodeWidth() && mouseY >= y && mouseY <= y + nodeHeight()) return node;
        }
        return null;
    }

    private int screenNodeX(AioaBehaviorGraph.Node node) { return canvasLeft() + this.canvasPanX + (int) Math.round(node.x * this.canvasZoom); }
    private int screenNodeY(AioaBehaviorGraph.Node node) { return canvasTop() + this.canvasPanY + (int) Math.round(node.y * this.canvasZoom); }
    private int nodeWidth() { return Math.max(64, (int) Math.round(NODE_WIDTH * this.canvasZoom)); }
    private int nodeHeight() { return Math.max(25, (int) Math.round(NODE_HEIGHT * this.canvasZoom)); }
    private int zoomPercent() { return (int) Math.round(this.canvasZoom * 100.0D); }
    private static double clampZoom(double value) { return Math.max(0.5D, Math.min(1.75D, value)); }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == 257 && this.selected != null && (this.parameterKey.isFocused() || this.parameterValue.isFocused())) {
            setParameter();
            return true;
        }
        if (Screen.hasControlDown()) {
            if (keyCode == 90) { undo(); return true; }
            if (keyCode == 89) { redo(); return true; }
            if (keyCode == 83) { applyAndClose(); return true; }
            if (keyCode == 78) { newGraph(); return true; }
            if (keyCode == 68) { duplicateSelected(); return true; }
            if (keyCode == 48) { fitGraph(); return true; }
        }
        if (keyCode == 261) { deleteSelected(); return true; }
        if (keyCode == 72) { this.showHelp = !this.showHelp; return true; }
        if (keyCode == 70) { fitGraph(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(guiGraphics);
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        int right = this.windowX + windowWidth();
        int bottom = this.windowY + windowHeight();
        AioaScreenUtil.drawPanel(guiGraphics, this.windowX, this.windowY, right, bottom);
        guiGraphics.fill(this.windowX + 1, this.windowY + 1, right - 1, this.windowY + 24, 0xFF18231D);
        guiGraphics.drawString(this.font, "AIOA Behavior Graph Studio - drag this title bar", this.windowX + 10, this.windowY + 8, AioaScreenUtil.TEXT_MAIN);
        drawGraphTabs(guiGraphics);
        guiGraphics.fill(this.canvasLeft(), this.canvasTop(), this.canvasRight(), this.canvasBottom(), 0xF0090D0B);
        drawGrid(guiGraphics);
        drawGraph(guiGraphics);
        if (this.draggingLink && this.linkStart != null) {
            drawBezier(guiGraphics, screenNodeX(this.linkStart) + nodeWidth(), screenNodeY(this.linkStart) + nodeHeight() / 2,
                    (int) this.linkMouseX, (int) this.linkMouseY, 0xFF92F5B8);
        }
        drawViewport(guiGraphics);
        guiGraphics.fill(this.windowX + 1, this.canvasTop(), this.canvasLeft() - 1, bottom - 1, 0xE0101713);
        guiGraphics.fill(this.inspectorLeft() + 1, this.canvasTop(), right - 1, bottom - 1, 0xE0101713);
        guiGraphics.drawString(this.font, "NODE PALETTE", this.windowX + 12, this.windowY + 66, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, "GRAPH / INSPECTOR", this.inspectorLeft() + 10, this.windowY + 66, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, this.selected == null ? "No node selected" : friendly(this.selected.type), this.inspectorLeft() + 10, this.windowY + 174, AioaScreenUtil.TEXT_MAIN);
        if (this.selected != null) {
            AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal(this.selected.type.help),
                    this.inspectorLeft() + INSPECTOR_WIDTH / 2, this.windowY + 188, INSPECTOR_WIDTH - 24, AioaScreenUtil.TEXT_SUB);
        }
        drawPaletteScrollBar(guiGraphics);
        guiGraphics.fill(this.windowX + 1, bottom - 27, right - 1, bottom - 1, 0xFF111A15);
        guiGraphics.drawString(this.font, this.status, this.windowX + 10, bottom - 18, AioaScreenUtil.TEXT_SUB);
        String workspace = "Scale 2 | " + zoomPercent() + "% | Wheel zoom | Drag empty grid to pan";
        guiGraphics.drawString(this.font, workspace, Math.max(this.windowX + 10, right - this.font.width(workspace) - 10), bottom - 18, 0xFF76B991);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.showHelp) drawHelp(guiGraphics);
        if (this.showContextMenu) drawContextMenu(guiGraphics);
        this.finishUiRender(guiGraphics);
    }

    private void drawGrid(GuiGraphics graphics) {
        int drift = (int) ((System.currentTimeMillis() / 45L) % 20L);
        for (int x = canvasLeft() + Math.floorMod(this.canvasPanX + drift, 20); x < canvasRight(); x += 20) {
            graphics.fill(x, canvasTop(), x + 1, canvasBottom(), 0x221D4430);
        }
        for (int y = canvasTop() + Math.floorMod(this.canvasPanY + drift, 20); y < canvasBottom(); y += 20) {
            graphics.fill(canvasLeft(), y, canvasRight(), y + 1, 0x221D4430);
        }
        int scanY = canvasTop() + (int) ((System.currentTimeMillis() / 12L) % Math.max(1, canvasBottom() - canvasTop()));
        graphics.fill(canvasLeft(), scanY, canvasRight(), scanY + 1, 0x3301BF63);
    }

    private void drawGraph(GuiGraphics graphics) {
        graphics.enableScissor(canvasLeft(), canvasTop(), canvasRight(), canvasBottom());
        for (AioaBehaviorGraph.Edge edge : this.graph.edges) {
            AioaBehaviorGraph.Node from = findNode(edge.from);
            AioaBehaviorGraph.Node to = findNode(edge.to);
            if (from == null || to == null) continue;
            int x1 = screenNodeX(from) + nodeWidth();
            int y1 = screenNodeY(from) + nodeHeight() / 2;
            int x2 = screenNodeX(to);
            int y2 = screenNodeY(to) + nodeHeight() / 2;
            int mid = (x1 + x2) / 2;
            drawBezier(graphics, x1, y1, x2, y2, 0xFF01BF63);
            graphics.drawString(this.font, edge.output == null ? "next" : edge.output, mid + 4,
                    Math.min(y1, y2) + Math.abs(y2 - y1) / 2 - 4, 0xFF9AD6AE);
        }
        for (AioaBehaviorGraph.Node node : this.graph.nodes) {
            int x = screenNodeX(node);
            int y = screenNodeY(node);
            int nodeWidth = nodeWidth();
            int nodeHeight = nodeHeight();
            AioaScreenUtil.drawInsetPanel(graphics, x, y, x + nodeWidth, y + nodeHeight, node == this.selected);
            graphics.fill(x + 1, y + 1, x + nodeWidth - 1, y + Math.min(14, nodeHeight - 2), colorFor(node.type.category));
            graphics.drawString(this.font, this.font.plainSubstrByWidth(friendly(node.type), nodeWidth - 10), x + 6, y + 4, 0xFFFFFFFF);
            if (this.canvasZoom >= 0.72D) graphics.drawString(this.font, node.type.category, x + 6, y + 22, AioaScreenUtil.TEXT_SUB);
            graphics.fill(x - 3, y + nodeHeight / 2 - 2, x + 2, y + nodeHeight / 2 + 3, 0xFF6EFFBA);
            graphics.fill(x + nodeWidth - 2, y + nodeHeight / 2 - 2, x + nodeWidth + 3, y + nodeHeight / 2 + 3, 0xFF6EFFBA);
            if (node == this.selected) {
                int pulse = 70 + (int) (Math.sin(System.currentTimeMillis() / 120.0D) * 35.0D);
                graphics.fill(x + 3, y + nodeHeight - 4, x + nodeWidth - 3, y + nodeHeight - 2, (pulse << 24) | 0x006EFFBA);
            }
        }
        graphics.disableScissor();
    }

    private void drawBezier(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int reach = Math.max(36, Math.abs(x2 - x1) / 2);
        double previousX = x1;
        double previousY = y1;
        for (int step = 1; step <= 28; step++) {
            double t = step / 28.0D;
            double inverse = 1.0D - t;
            double x = inverse * inverse * inverse * x1 + 3 * inverse * inverse * t * (x1 + reach)
                    + 3 * inverse * t * t * (x2 - reach) + t * t * t * x2;
            double y = inverse * inverse * inverse * y1 + 3 * inverse * inverse * t * y1
                    + 3 * inverse * t * t * y2 + t * t * t * y2;
            int minX = (int) Math.floor(Math.min(previousX, x));
            int minY = (int) Math.floor(Math.min(previousY, y));
            int maxX = (int) Math.ceil(Math.max(previousX, x));
            int maxY = (int) Math.ceil(Math.max(previousY, y));
            graphics.fill(minX, minY, Math.max(minX + 2, maxX + 2), Math.max(minY + 2, maxY + 2), color);
            previousX = x;
            previousY = y;
        }
    }

    private void drawPaletteScrollBar(GuiGraphics graphics) {
        int pageSize = Math.max(4, (windowHeight() - 150) / 30);
        int pages = Math.max(1, (AioaBehaviorGraph.NodeType.values().length + pageSize - 1) / pageSize);
        int top = this.windowY + 82;
        int height = Math.max(30, windowHeight() - 154);
        int thumb = Math.max(18, height / pages);
        int y = top + (pages <= 1 ? 0 : (height - thumb) * this.palettePage / (pages - 1));
        graphics.fill(canvasLeft() - 5, top, canvasLeft() - 2, top + height, 0xFF23342A);
        graphics.fill(canvasLeft() - 6, y, canvasLeft() - 1, y + thumb, 0xFF5ACB88);
    }

    private void drawContextMenu(GuiGraphics graphics) {
        String[] actions = {"Undo  Ctrl+Z", "Redo  Ctrl+Y", "Duplicate  Ctrl+D", "Delete  Del", "New Graph  Ctrl+N", "Fit View  F"};
        graphics.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + 128, this.contextMenuY + 124, 0xFA111A15);
        for (int i = 0; i < actions.length; i++) {
            int y = this.contextMenuY + 4 + i * 20;
            graphics.fill(this.contextMenuX + 3, y, this.contextMenuX + 125, y + 18, 0xFF1B2921);
            graphics.drawString(this.font, actions[i], this.contextMenuX + 8, y + 5, AioaScreenUtil.TEXT_MAIN);
        }
    }

    private boolean contextMenuClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || mouseX < this.contextMenuX || mouseX > this.contextMenuX + 128
                || mouseY < this.contextMenuY || mouseY > this.contextMenuY + 124) return false;
        int action = Math.max(0, Math.min(5, ((int) mouseY - this.contextMenuY - 4) / 20));
        switch (action) {
            case 0 -> undo();
            case 1 -> redo();
            case 2 -> duplicateSelected();
            case 3 -> deleteSelected();
            case 4 -> newGraph();
            case 5 -> fitGraph();
        }
        this.showContextMenu = false;
        return true;
    }

    private void drawHelp(GuiGraphics graphics) {
        int width = Math.max(180, Math.min(this.helpWidth, this.width - this.helpX));
        int height = Math.max(86, Math.min(this.helpHeight, this.height - this.helpY));
        int left = this.helpX;
        int top = this.helpY;
        graphics.fill(left, top, left + width, top + height, 0xF2101A15);
        graphics.fill(left, top, left + width, top + 18, 0xFF1A2B21);
        String title = switch (this.helpPage) {
            case 1 -> "SCOPES & WORLD PICKING";
            case 2 -> "PORTS & SAFE NETWORKING";
            default -> "QUICK GUIDE";
        };
        graphics.drawString(this.font, title + " - drag", left + 10, top + 6, AioaScreenUtil.TEXT_MAIN);
        graphics.drawString(this.font, "<", left + width - 92, top + 5, AioaScreenUtil.TEXT_SUB);
        graphics.drawString(this.font, ">", left + width - 64, top + 5, AioaScreenUtil.TEXT_SUB);
        graphics.drawString(this.font, "x", left + width - 16, top + 5, 0xFFFF9A9A);
        String body = switch (this.helpPage) {
            case 1 -> "Choose Single Entity, Entity Type, Entity Tag, Managed Mobs, or All Mobs. Pick in world closes the UI; right-click the mob you want and the editor reopens bound to it. F7 cancels selection.";
            case 2 -> "Right-click a source node, select its output port, then left-click a destination. Server validation limits graph size, permissions, spawn ranges, and packet size before a graph can run.";
            default -> "Pick nodes and drag them on the grid. Drag empty grid space to pan and use the wheel to zoom at the cursor; Shift+wheel pans sideways. Drag output ports to inputs. Double-click nodes to cycle values. Ctrl+Z/Y undo/redo, Ctrl+D duplicates, Ctrl+S applies, F fits.";
        };
        AioaScreenUtil.drawWrappedCenteredText(graphics, this.font,
                Component.literal(body),
                left + width / 2, top + 26, width - 20, AioaScreenUtil.TEXT_SUB);
        graphics.fill(left + width - 10, top + height - 2, left + width, top + height, 0xFF6EFFBA);
        graphics.fill(left + width - 2, top + height - 10, left + width, top + height, 0xFF6EFFBA);
    }

    private void drawViewport(GuiGraphics graphics) {
        ResourceLocation entityId = previewEntityId();
        int width = Math.min(this.viewportWidth, Math.max(120, this.width - this.viewportX));
        int height = Math.min(this.viewportHeight, Math.max(90, this.height - this.viewportY));
        AioaScreenUtil.drawMobPreview(graphics, this.font, this.viewportX, this.viewportY, width, height, entityId, true,
                List.of(Component.literal("LIVE VIEWPORT"), Component.literal(this.selected == null ? "Select a node" : friendly(this.selected.type))));
        graphics.fill(this.viewportX + width - 10, this.viewportY + height - 2, this.viewportX + width, this.viewportY + height, 0xFF6EFFBA);
        graphics.fill(this.viewportX + width - 2, this.viewportY + height - 10, this.viewportX + width, this.viewportY + height, 0xFF6EFFBA);
    }

    private void drawGraphTabs(GuiGraphics graphics) {
        int tabStart = this.windowX + 330;
        int visibleTabs = Math.min(5, this.graphs.size());
        for (int i = 0; i < visibleTabs; i++) {
            int x = tabStart + i * 92;
            int color = i == this.graphIndex ? 0xFF315A42 : 0xFF1E3026;
            graphics.fill(x, this.windowY + 2, x + 88, this.windowY + 22, color);
            String name = this.font.plainSubstrByWidth(this.graphs.get(i).name, 78);
            graphics.drawString(this.font, name, x + 6, this.windowY + 8, i == this.graphIndex ? 0xFFFFFFFF : AioaScreenUtil.TEXT_SUB);
        }
    }

    private ResourceLocation previewEntityId() {
        if (this.graph.scope == AioaBehaviorGraph.Scope.ENTITY_TYPE) {
            ResourceLocation parsed = AioaEntityHelper.parseResourceLocation(this.graph.selector);
            if (parsed != null) return parsed;
        }
        if (this.graph.scope == AioaBehaviorGraph.Scope.SINGLE_ENTITY && this.minecraft != null && this.minecraft.level != null) {
            try {
                UUID selectedId = UUID.fromString(this.graph.selector);
                for (var entity : this.minecraft.level.entitiesForRendering()) {
                    if (entity instanceof Mob && entity.getUUID().equals(selectedId)) {
                        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (this.selected != null) {
            ResourceLocation parsed = AioaEntityHelper.parseResourceLocation(this.selected.parameters.get("entity"));
            if (parsed != null) return parsed;
        }
        return ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
    }

    private AioaBehaviorGraph.Node findNode(String id) {
        return this.graph.nodes.stream().filter(node -> node.id.equals(id)).findFirst().orElse(null);
    }

    private static String defaultOutput(AioaBehaviorGraph.NodeType type) {
        return outputsFor(type)[0];
    }

    private static String nextOutput(AioaBehaviorGraph.NodeType type, String current) {
        String[] outputs = outputsFor(type);
        int index = java.util.Arrays.asList(outputs).indexOf(current);
        return outputs[(Math.max(0, index) + 1) % outputs.length];
    }

    private static String[] outputsFor(AioaBehaviorGraph.NodeType type) {
        if (type == null) return new String[]{"next"};
        return switch (type) {
            case FIND_NEAREST_PLAYER, FIND_NEAREST_ANIMAL, FIND_NEAREST_MOB, FIND_ENTITY_TYPE -> new String[]{"found", "missing"};
            case HAS_TARGET, TARGET_IN_RANGE, HEALTH_BELOW, CAN_SEE_TARGET, IS_DAYTIME, IS_ON_GROUND, WAS_HURT -> new String[]{"true", "false"};
            case EVERY_TICKS, ON_FIRST_TICK -> new String[]{"ready", "waiting"};
            case RANDOM_CHANCE -> new String[]{"success", "fail"};
            default -> new String[]{"next"};
        };
    }

    private static void applyDefaultParameters(AioaBehaviorGraph.Node node) {
        switch (node.type) {
            case MOB_BASE -> node.parameter("entity", "auto").parameter("health", "20").parameter("damage", "3").parameter("speed", "0.23");
            case EVERY_TICKS -> node.parameter("ticks", "20");
            case RANDOM_CHANCE -> node.parameter("chance", "0.5");
            case FIND_NEAREST_PLAYER, FIND_NEAREST_ANIMAL, FIND_NEAREST_MOB -> node.parameter("range", "24");
            case FIND_ENTITY_TYPE -> node.parameter("entity", "minecraft:zombie").parameter("range", "24");
            case TARGET_IN_RANGE, ATTACK_TARGET -> node.parameter("range", "3");
            case HEALTH_BELOW -> node.parameter("percent", "0.5");
            case MOVE_TO_TARGET -> node.parameter("speed", "1.1");
            case FLEE_TARGET -> node.parameter("distance", "12").parameter("speed", "1.1");
            case WALK_BLOCKS -> node.parameter("blocks", "4").parameter("speed", "1.0");
            case WANDER -> node.parameter("radius", "8").parameter("speed", "0.9");
            case ROTATE_DEGREES -> node.parameter("degrees", "90");
            case STRAFE -> node.parameter("forward", "0").parameter("sideways", "1");
            case JUMP -> node.parameter("strength", "0.42");
            case TELEPORT_RELATIVE -> node.parameter("x", "0").parameter("y", "0").parameter("z", "0");
            case KNOCKBACK_TARGET -> node.parameter("strength", "0.6");
            case SET_AGGRESSIVE, SET_NO_AI, SET_PERSISTENT, SET_GLOWING, SET_SILENT, SET_INVULNERABLE -> node.parameter("value", "true");
            case SET_CUSTOM_NAME -> node.parameter("name", "AIOA Mob").parameter("visible", "true");
            case SET_MAX_HEALTH -> node.parameter("value", "20");
            case SET_ATTACK_DAMAGE -> node.parameter("value", "3");
            case SET_MOVEMENT_SPEED -> node.parameter("value", "0.23");
            case EQUIP_ITEM -> node.parameter("item", "minecraft:iron_sword").parameter("slot", "MAINHAND").parameter("dropChance", "0");
            case SPAWN_MOB -> node.parameter("entity", "minecraft:zombie").parameter("cooldown", "200").parameter("nearbyCap", "8");
            case PLAY_SOUND -> node.parameter("sound", "minecraft:entity.zombie.ambient").parameter("volume", "1").parameter("pitch", "1");
            default -> { }
        }
    }

    private static int colorFor(String category) {
        return switch (category) {
            case "Events" -> 0xFF7555C8;
            case "Sensing" -> 0xFF2676B8;
            case "Conditions" -> 0xFFC58A22;
            case "Movement" -> 0xFF247E58;
            case "Targeting" -> 0xFFB13D47;
            case "Interaction" -> 0xFF8D4FB0;
            case "Effects" -> 0xFFB05E2D;
            default -> 0xFF3C5347;
        };
    }

    private static String friendly(Object value) {
        String text = value.toString().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static <T> T next(T current, T[] values) {
        int index = java.util.Arrays.asList(values).indexOf(current);
        return values[(index + 1) % values.length];
    }

    @Override
    public void onClose() {
        this.transitionTo(this.parent);
    }
}
