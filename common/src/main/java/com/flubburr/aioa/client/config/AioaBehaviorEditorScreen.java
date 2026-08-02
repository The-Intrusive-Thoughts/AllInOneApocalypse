package com.flubburr.aioa.client.config;

import com.flubburr.aioa.behavior.AioaBehaviorGraph;
import com.flubburr.aioa.behavior.AioaBehaviorValidator;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.network.AioaClientNetworking;
import com.flubburr.aioa.compat.AioaEntityHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
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
    private AioaBehaviorGraph.Node selected;
    private AioaBehaviorGraph.Node linkStart;
    private String linkOutput = "next";
    private int windowX;
    private int windowY;
    private int canvasPanX;
    private int canvasPanY;
    private int palettePage;
    private boolean draggingWindow;
    private boolean draggingNode;
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
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 58, "Help", button -> this.showHelp = !this.showHelp));
        x += 62;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 58, "< Graph", button -> switchGraph(-1)));
        x += 62;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 58, "Graph >", button -> switchGraph(1)));

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
                    this.graph.scope = AioaBehaviorGraph.Scope.ENTITY_TYPE;
                    this.graph.selector = id.toString();
                    this.selector.setValue(this.graph.selector);
                    this.scopeButton.setMessage(Component.literal("Scope: " + friendly(this.graph.scope)));
                    this.status = "Selected " + AioaScreenUtil.entityDisplayName(id) + ".";
                }))));
        this.addRenderableWidget(AioaScreenUtil.button(inspectorX, this.windowY + 370, inspectorWidth, "Pick in world (right-click)", button ->
                AioaMobSelectionController.arm(this)));

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

    private void addNode(AioaBehaviorGraph.NodeType type) {
        snapshot();
        AioaBehaviorGraph.Node node = new AioaBehaviorGraph.Node("node_" + UUID.randomUUID().toString().substring(0, 8), type,
                50 - this.canvasPanX + (this.graph.nodes.size() % 3) * 165,
                70 - this.canvasPanY + (this.graph.nodes.size() / 3) * 68);
        this.graph.nodes.add(node);
        applyDefaultParameters(node);
        this.selected = node;
        this.status = "Added " + friendly(type) + ".";
    }

    private void deleteSelected() {
        if (this.selected == null) return;
        snapshot();
        String id = this.selected.id;
        this.graph.nodes.removeIf(node -> node.id.equals(id));
        this.graph.edges.removeIf(edge -> edge.from.equals(id) || edge.to.equals(id));
        this.selected = null;
        this.linkStart = null;
        this.status = "Node deleted.";
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int visibleHelpWidth = Math.min(this.helpWidth, this.width - this.helpX);
        int visibleHelpHeight = Math.min(this.helpHeight, this.height - this.helpY);
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
                this.draggingNode = true;
                this.dragOffsetX = (int) mouseX - screenNodeX(hit);
                this.dragOffsetY = (int) mouseY - screenNodeY(hit);
                this.status = hit.type.help;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
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
            this.selected.x = (int) mouseX - this.dragOffsetX - canvasLeft() - this.canvasPanX;
            this.selected.y = (int) mouseY - this.dragOffsetY - canvasTop() - this.canvasPanY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingWindow = false;
        this.draggingNode = false;
        this.draggingHelp = false;
        this.resizingHelp = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (insideCanvas(mouseX, mouseY)) {
            this.canvasPanY += (int) delta * 24;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean insideCanvas(double x, double y) {
        return x >= canvasLeft() && x <= canvasRight() && y >= canvasTop() && y <= canvasBottom();
    }

    private AioaBehaviorGraph.Node nodeAt(double mouseX, double mouseY) {
        for (int i = this.graph.nodes.size() - 1; i >= 0; i--) {
            AioaBehaviorGraph.Node node = this.graph.nodes.get(i);
            int x = screenNodeX(node);
            int y = screenNodeY(node);
            if (mouseX >= x && mouseX <= x + NODE_WIDTH && mouseY >= y && mouseY <= y + NODE_HEIGHT) return node;
        }
        return null;
    }

    private int screenNodeX(AioaBehaviorGraph.Node node) { return canvasLeft() + this.canvasPanX + node.x; }
    private int screenNodeY(AioaBehaviorGraph.Node node) { return canvasTop() + this.canvasPanY + node.y; }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(guiGraphics);
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        int right = this.windowX + windowWidth();
        int bottom = this.windowY + windowHeight();
        AioaScreenUtil.drawPanel(guiGraphics, this.windowX, this.windowY, right, bottom);
        guiGraphics.fill(this.windowX + 1, this.windowY + 1, right - 1, this.windowY + 24, 0xFF18231D);
        guiGraphics.drawString(this.font, "AIOA Behavior Graph Studio - drag this title bar", this.windowX + 10, this.windowY + 8, AioaScreenUtil.TEXT_MAIN);
        guiGraphics.fill(this.canvasLeft(), this.canvasTop(), this.canvasRight(), this.canvasBottom(), 0xF0090D0B);
        drawGrid(guiGraphics);
        drawGraph(guiGraphics);
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
        guiGraphics.fill(this.windowX + 1, bottom - 27, right - 1, bottom - 1, 0xFF111A15);
        guiGraphics.drawString(this.font, this.status, this.windowX + 10, bottom - 18, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.showHelp) drawHelp(guiGraphics);
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
            int x1 = screenNodeX(from) + NODE_WIDTH;
            int y1 = screenNodeY(from) + NODE_HEIGHT / 2;
            int x2 = screenNodeX(to);
            int y2 = screenNodeY(to) + NODE_HEIGHT / 2;
            int mid = (x1 + x2) / 2;
            graphics.fill(Math.min(x1, mid), y1, Math.max(x1, mid) + 1, y1 + 2, 0xFF01BF63);
            graphics.fill(mid, Math.min(y1, y2), mid + 2, Math.max(y1, y2) + 1, 0xFF01BF63);
            graphics.fill(Math.min(mid, x2), y2, Math.max(mid, x2) + 1, y2 + 2, 0xFF01BF63);
            graphics.drawString(this.font, edge.output == null ? "next" : edge.output, mid + 4,
                    Math.min(y1, y2) + Math.abs(y2 - y1) / 2 - 4, 0xFF9AD6AE);
        }
        for (AioaBehaviorGraph.Node node : this.graph.nodes) {
            int x = screenNodeX(node);
            int y = screenNodeY(node);
            AioaScreenUtil.drawInsetPanel(graphics, x, y, x + NODE_WIDTH, y + NODE_HEIGHT, node == this.selected);
            graphics.fill(x + 1, y + 1, x + NODE_WIDTH - 1, y + 14, colorFor(node.type.category));
            graphics.drawString(this.font, friendly(node.type), x + 6, y + 4, 0xFFFFFFFF);
            graphics.drawString(this.font, node.type.category, x + 6, y + 22, AioaScreenUtil.TEXT_SUB);
            graphics.fill(x - 3, y + NODE_HEIGHT / 2 - 2, x + 2, y + NODE_HEIGHT / 2 + 3, 0xFF6EFFBA);
            graphics.fill(x + NODE_WIDTH - 2, y + NODE_HEIGHT / 2 - 2, x + NODE_WIDTH + 3, y + NODE_HEIGHT / 2 + 3, 0xFF6EFFBA);
            if (node == this.selected) {
                int pulse = 70 + (int) (Math.sin(System.currentTimeMillis() / 120.0D) * 35.0D);
                graphics.fill(x + 3, y + NODE_HEIGHT - 4, x + NODE_WIDTH - 3, y + NODE_HEIGHT - 2, (pulse << 24) | 0x006EFFBA);
            }
        }
        graphics.disableScissor();
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
            default -> "Pick nodes from the palette and drag them on the grid. Select a node to edit parameters, use the live viewport as context, validate, then Apply. New creates another independent behavior graph.";
        };
        AioaScreenUtil.drawWrappedCenteredText(graphics, this.font,
                Component.literal(body),
                left + width / 2, top + 26, width - 20, AioaScreenUtil.TEXT_SUB);
        graphics.fill(left + width - 10, top + height - 2, left + width, top + height, 0xFF6EFFBA);
        graphics.fill(left + width - 2, top + height - 10, left + width, top + height, 0xFF6EFFBA);
    }

    private void drawViewport(GuiGraphics graphics) {
        ResourceLocation entityId = previewEntityId();
        int width = Math.min(178, Math.max(120, canvasRight() - canvasLeft() - 20));
        int left = canvasRight() - width - 8;
        int top = canvasTop() + 8;
        AioaScreenUtil.drawMobPreview(graphics, this.font, left, top, width, 118, entityId, true,
                List.of(Component.literal("LIVE VIEWPORT"), Component.literal(this.selected == null ? "Select a node" : friendly(this.selected.type))));
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
        return new ResourceLocation("minecraft", "zombie");
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
