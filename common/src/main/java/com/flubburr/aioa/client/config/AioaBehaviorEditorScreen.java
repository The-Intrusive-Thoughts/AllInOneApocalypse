package com.flubburr.aioa.client.config;

import com.flubburr.aioa.behavior.AioaBehaviorGraph;
import com.flubburr.aioa.behavior.AioaBehaviorValidator;
import com.flubburr.aioa.behavior.AioaGraphLibrary;
import com.flubburr.aioa.behavior.AioaNodeSchema;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.network.AioaClientNetworking;
import com.flubburr.aioa.compat.AioaEntityHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.EnumMap;
import java.util.Map;

public final class AioaBehaviorEditorScreen extends AioaAnimatedScreen {
    private static final int WINDOW_WIDTH = 920;
    private static final int WINDOW_HEIGHT = 520;
    private static final int PALETTE_WIDTH = 170;
    private static final int INSPECTOR_WIDTH = 190;
    private static final int NODE_WIDTH = 142;
    private static final int NODE_HEIGHT = 46;

    private final Screen parent;
    private final AioaConfig editableConfig;
    private final boolean saveOnApply;
    private final List<AioaBehaviorGraph> graphs = new ArrayList<>();
    private final Deque<AioaBehaviorGraph> undo = new ArrayDeque<>();
    private final Deque<AioaBehaviorGraph> redo = new ArrayDeque<>();
    private AioaBehaviorGraph graph;
    private int graphIndex;
    private int tabOffset;
    private int viewportX;
    private int viewportY;
    private int viewportWidth = 320;
    private int viewportHeight = 220;
    private AioaBehaviorGraph.Node selected;
    private String selectedEdgeId;
    private AioaBehaviorGraph.Node linkStart;
    private String linkOutput = "next";
    private String linkInput = "exec";
    private int windowX;
    private int windowY;
    private int workspaceWidth = WINDOW_WIDTH;
    private int workspaceHeight = WINDOW_HEIGHT;
    private boolean configuredScaleApplied;
    private double effectiveUiScale = 1.0D;
    private boolean resizingWorkspace;
    private FloatingWindow resizingFloatingWindow;
    private int paletteX;
    private int paletteY;
    private int paletteWidth = PALETTE_WIDTH;
    private int paletteHeight = 390;
    private int inspectorX;
    private int inspectorY;
    private int inspectorWidth = INSPECTOR_WIDTH;
    private int inspectorHeight = 292;
    private int parametersX;
    private int parametersY;
    private int parametersWidth = 210;
    private int parametersHeight = 188;
    private boolean paletteCollapsed;
    private boolean inspectorCollapsed;
    private boolean parametersCollapsed = true;
    private FloatingWindow draggingFloatingWindow;
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
    private boolean previewPlaying;
    private int previewSpeed = 1;
    private final AioaViewportSimulation previewSimulation = new AioaViewportSimulation();
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
    private String layoutGraphId = "";
    private boolean graphDirty;
    private boolean runtimeDirty;
    private boolean rebuildingWidgets;
    private long lastMutationAt;
    private long lastAutosaveAt;
    private final List<FloatingWindow> floatingOrder = new ArrayList<>(List.of(
            FloatingWindow.PALETTE, FloatingWindow.INSPECTOR, FloatingWindow.PARAMETERS));
    private final Map<FloatingWindow, List<AbstractWidget>> floatingWidgets = new EnumMap<>(FloatingWindow.class);

    private AioaBehaviorEditorScreen(Screen parent, AioaConfig editableConfig, boolean saveOnApply) {
        super(Component.literal("AIOA Behavior Graph Studio"));
        this.parent = parent;
        this.editableConfig = editableConfig;
        this.saveOnApply = saveOnApply;
        if (editableConfig.behaviorGraphs.isEmpty()) {
            this.graphs.add(AioaBehaviorGraph.createStarter());
        } else {
            editableConfig.behaviorGraphs.forEach(graph -> this.graphs.add(graph.copy()));
        }
        this.graph = this.graphs.get(0);
    }

    public static Screen create(Screen parent, AioaConfig editableConfig) {
        return new AioaBehaviorEditorScreen(parent, editableConfig, false);
    }

    public static Screen createForWorld(Screen parent) {
        return new AioaBehaviorEditorScreen(parent, AioaConfigManager.getConfigCopy(), true);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        if (!this.configuredScaleApplied) {
            double minecraftScale = this.minecraft == null ? 2.0D : Math.max(1.0D, this.minecraft.getWindow().getGuiScale());
            this.effectiveUiScale = this.editableConfig.clientUi.editorScalePercent / 100.0D * Math.min(1.0D, 2.0D / minecraftScale);
            this.workspaceWidth = Math.max(minimumWorkspaceWidth(), (int) Math.round(WINDOW_WIDTH * this.effectiveUiScale));
            this.workspaceHeight = Math.max(minimumWorkspaceHeight(), (int) Math.round(WINDOW_HEIGHT * this.effectiveUiScale));
            this.canvasZoom = clampZoom(this.effectiveUiScale);
            this.configuredScaleApplied = true;
        }
        if (!this.graph.id.equals(this.layoutGraphId)) loadGraphLayout();
        clampLayoutToScreen();
        rebuildEditorWidgets();
    }

    private void loadGraphLayout() {
        AioaBehaviorGraph.EditorLayout layout = this.graph.editorLayout;
        if (!layout.initialized) {
            boolean wide = this.width >= 1000;
            this.windowX = wide ? Math.min(190, this.width / 6) : 8;
            this.windowY = 8;
            this.workspaceWidth = Math.max(minimumWorkspaceWidth(), Math.min(this.width - this.windowX - 8,
                    (int) Math.round(WINDOW_WIDTH * this.effectiveUiScale)));
            this.workspaceHeight = Math.max(minimumWorkspaceHeight(), Math.min(this.height - 16,
                    (int) Math.round(WINDOW_HEIGHT * this.effectiveUiScale)));
            this.viewportX = wide ? 16 : this.windowX + 10;
            this.viewportY = 10;
            this.viewportWidth = wide ? 300 : 260;
            this.viewportHeight = wide ? 210 : 180;
            this.paletteX = this.windowX + 8;
            this.paletteY = this.windowY + Math.max(180, this.workspaceHeight - 250);
            this.inspectorX = this.windowX + this.workspaceWidth - this.inspectorWidth - 8;
            this.inspectorY = this.windowY + 28;
            this.parametersX = this.inspectorX;
            this.parametersY = this.windowY + Math.max(170, this.workspaceHeight - this.parametersHeight - 34);
            this.helpX = this.paletteX + this.paletteWidth + 8;
            this.helpY = this.windowY + Math.max(190, this.workspaceHeight - this.helpHeight - 12);
            this.canvasPanX = 10;
            this.canvasPanY = 8;
            saveGraphLayout();
        } else {
            double sx = layout.screenWidth > 0 ? this.width / (double) layout.screenWidth : 1.0D;
            double sy = layout.screenHeight > 0 ? this.height / (double) layout.screenHeight : 1.0D;
            this.windowX = scaled(layout.workspaceX, sx); this.windowY = scaled(layout.workspaceY, sy);
            this.workspaceWidth = scaled(layout.workspaceWidth, sx); this.workspaceHeight = scaled(layout.workspaceHeight, sy);
            this.viewportX = scaled(layout.viewportX, sx); this.viewportY = scaled(layout.viewportY, sy);
            this.viewportWidth = scaled(layout.viewportWidth, sx); this.viewportHeight = scaled(layout.viewportHeight, sy);
            this.paletteX = scaled(layout.paletteX, sx); this.paletteY = scaled(layout.paletteY, sy);
            this.paletteWidth = scaled(layout.paletteWidth, sx); this.paletteHeight = scaled(layout.paletteHeight, sy);
            this.inspectorX = scaled(layout.inspectorX, sx); this.inspectorY = scaled(layout.inspectorY, sy);
            this.inspectorWidth = scaled(layout.inspectorWidth, sx); this.inspectorHeight = scaled(layout.inspectorHeight, sy);
            this.parametersX = scaled(layout.parametersX, sx); this.parametersY = scaled(layout.parametersY, sy);
            this.parametersWidth = scaled(layout.parametersWidth, sx); this.parametersHeight = scaled(layout.parametersHeight, sy);
            this.helpX = scaled(layout.helpX, sx); this.helpY = scaled(layout.helpY, sy);
            this.helpWidth = scaled(layout.helpWidth, sx); this.helpHeight = scaled(layout.helpHeight, sy);
            this.paletteCollapsed = layout.paletteCollapsed; this.inspectorCollapsed = layout.inspectorCollapsed;
            this.parametersCollapsed = layout.parametersCollapsed; this.showHelp = layout.showHelp;
            this.canvasPanX = layout.canvasPanX; this.canvasPanY = layout.canvasPanY; this.canvasZoom = clampZoom(layout.canvasZoom);
        }
        this.layoutGraphId = this.graph.id;
    }

    private static int scaled(int value, double scale) { return (int) Math.round(value * scale); }

    private void clampLayoutToScreen() {
        this.windowX = Math.max(0, Math.min(this.width - 300, this.windowX));
        this.windowY = Math.max(0, Math.min(this.height - 180, this.windowY));
        this.workspaceWidth = Math.max(minimumWorkspaceWidth(), Math.min(this.workspaceWidth, this.width - this.windowX));
        this.workspaceHeight = Math.max(minimumWorkspaceHeight(), Math.min(this.workspaceHeight, this.height - this.windowY));
        this.paletteWidth = Math.max(130, Math.min(420, this.paletteWidth)); this.paletteHeight = Math.max(120, Math.min(720, this.paletteHeight));
        this.inspectorWidth = Math.max(170, Math.min(520, this.inspectorWidth)); this.inspectorHeight = Math.max(240, Math.min(720, this.inspectorHeight));
        this.parametersWidth = Math.max(190, Math.min(620, this.parametersWidth)); this.parametersHeight = Math.max(180, Math.min(520, this.parametersHeight));
        this.viewportWidth = Math.max(150, Math.min(900, this.viewportWidth)); this.viewportHeight = Math.max(100, Math.min(600, this.viewportHeight));
    }

    private void saveGraphLayout() {
        AioaBehaviorGraph.EditorLayout layout = this.graph.editorLayout;
        layout.initialized = true; layout.screenWidth = this.width; layout.screenHeight = this.height;
        layout.workspaceX = this.windowX; layout.workspaceY = this.windowY; layout.workspaceWidth = this.workspaceWidth; layout.workspaceHeight = this.workspaceHeight;
        layout.viewportX = this.viewportX; layout.viewportY = this.viewportY; layout.viewportWidth = this.viewportWidth; layout.viewportHeight = this.viewportHeight;
        layout.paletteX = this.paletteX; layout.paletteY = this.paletteY; layout.paletteWidth = this.paletteWidth; layout.paletteHeight = this.paletteHeight;
        layout.inspectorX = this.inspectorX; layout.inspectorY = this.inspectorY; layout.inspectorWidth = this.inspectorWidth; layout.inspectorHeight = this.inspectorHeight;
        layout.parametersX = this.parametersX; layout.parametersY = this.parametersY; layout.parametersWidth = this.parametersWidth; layout.parametersHeight = this.parametersHeight;
        layout.helpX = this.helpX; layout.helpY = this.helpY; layout.helpWidth = this.helpWidth; layout.helpHeight = this.helpHeight;
        layout.paletteCollapsed = this.paletteCollapsed; layout.inspectorCollapsed = this.inspectorCollapsed;
        layout.parametersCollapsed = this.parametersCollapsed; layout.showHelp = this.showHelp;
        layout.canvasPanX = this.canvasPanX; layout.canvasPanY = this.canvasPanY; layout.canvasZoom = this.canvasZoom;
    }

    private int minimumWorkspaceWidth() { return Math.min(620, Math.max(320, this.width - 16)); }
    private int minimumWorkspaceHeight() { return Math.min(360, Math.max(220, this.height - 16)); }
    private int windowWidth() { return Math.max(minimumWorkspaceWidth(), Math.min(this.workspaceWidth, this.width - this.windowX)); }
    private int windowHeight() { return Math.max(minimumWorkspaceHeight(), Math.min(this.workspaceHeight, this.height - this.windowY)); }
    private int inspectorLeft() { return this.inspectorX; }
    private int canvasLeft() { return this.windowX + 8; }
    private int canvasRight() { return this.windowX + windowWidth() - 8; }
    private int canvasTop() { return this.windowY + 58; }
    private int canvasBottom() { return this.windowY + windowHeight() - 28; }

    private void rebuildEditorWidgets() {
        if (!this.rebuildingWidgets && this.parameterKey != null && this.parameterValue != null) setParameterIfPresent();
        this.rebuildingWidgets = true;
        this.clearWidgets();
        this.floatingWidgets.clear();
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
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 58, Component.translatable("aioa.editor.help").getString(), button -> {
            this.showHelp = !this.showHelp;
            saveGraphLayout();
        }));
        x += 62;
        this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, 58, Component.translatable("aioa.editor.docs").getString(), button ->
                this.transitionTo(new AioaDocsScreen(this))));

        int right = this.windowX + windowWidth() - 8;
        this.addRenderableWidget(AioaScreenUtil.button(right - 88, toolbarY, 80, "Done", button -> applyAndClose()));

        int inspectorX = this.inspectorX + 10;
        int inspectorWidth = this.inspectorWidth - 20;
        int inspectorContentY = this.inspectorY + 28;
        this.graphName = new EditBox(this.font, inspectorX, inspectorContentY, inspectorWidth, 22, Component.literal("Graph name"));
        this.graphName.setValue(this.graph.name);
        this.graphName.setResponder(value -> {
            if (!this.rebuildingWidgets) {
                this.graph.name = value.trim().isBlank() ? "Untitled behavior" : value.trim();
                markDirty(true);
            }
        });
        if (!this.inspectorCollapsed) addFloatingWidget(FloatingWindow.INSPECTOR, this.graphName);
        Button newScopeButton = AioaScreenUtil.button(inspectorX, inspectorContentY + 28, inspectorWidth,
                "Scope: " + friendly(this.graph.scope), button -> {
                    snapshot();
                    this.graph.scope = next(this.graph.scope, AioaBehaviorGraph.Scope.values());
                    if (this.graph.scope == AioaBehaviorGraph.Scope.MANAGED_MOBS
                            || this.graph.scope == AioaBehaviorGraph.Scope.ALL_MOBS) this.graph.selector = "";
                    rebuildEditorWidgets();
                });
        this.scopeButton = newScopeButton;
        if (!this.inspectorCollapsed) addFloatingWidget(FloatingWindow.INSPECTOR, newScopeButton);
        this.selector = new EditBox(this.font, inspectorX, inspectorContentY + 58, inspectorWidth, 22, Component.literal("Scope selector"));
        this.selector.setValue(this.graph.selector);
        this.selector.setHint(Component.literal("scoreboard tag"));
        this.selector.setResponder(value -> {
            if (!this.rebuildingWidgets && this.graph.scope == AioaBehaviorGraph.Scope.ENTITY_TAG) {
                this.graph.selector = value.trim();
                markDirty(true);
            }
        });
        if (!this.inspectorCollapsed && this.graph.scope == AioaBehaviorGraph.Scope.ENTITY_TAG) {
            addFloatingWidget(FloatingWindow.INSPECTOR, this.selector);
        } else if (!this.inspectorCollapsed) {
            addFloatingWidget(FloatingWindow.INSPECTOR, AioaScreenUtil.button(inspectorX, inspectorContentY + 58, inspectorWidth,
                    graphTargetLabel(), button -> selectGraphTarget()));
        }

        int parameterContentX = this.parametersX + 10;
        int parameterContentWidth = this.parametersWidth - 20;
        this.parameterKey = new EditBox(this.font, parameterContentX, this.parametersY + 28, parameterContentWidth, 22, Component.literal("Parameter name"));
        this.parameterKey.setHint(Component.literal("range / speed / ticks..."));
        this.parameterKey.setMaxLength(48);
        if (!this.parametersCollapsed) addFloatingWidget(FloatingWindow.PARAMETERS, this.parameterKey);
        this.parameterValue = new EditBox(this.font, parameterContentX, this.parametersY + 56, parameterContentWidth, 22, Component.literal("Parameter value"));
        this.parameterValue.setHint(Component.literal("value"));
        this.parameterValue.setMaxLength(2048);
        if (!this.parametersCollapsed) addFloatingWidget(FloatingWindow.PARAMETERS, this.parameterValue);
        if (!this.parametersCollapsed) {
            int half = (parameterContentWidth - 6) / 2;
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX, this.parametersY + 84, half, "< Parameter", button -> cycleParameter(-1)));
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX + half + 6, this.parametersY + 84, half, "Parameter >", button -> cycleParameter(1)));
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX, this.parametersY + 114, parameterContentWidth, parameterToolLabel(), button -> useParameterTool()));
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX, this.parametersY + 144, parameterContentWidth, "Apply value", button -> setParameter()));
        }
        if (!this.inspectorCollapsed) addFloatingWidget(FloatingWindow.INSPECTOR, AioaScreenUtil.button(inspectorX, inspectorContentY + 88, inspectorWidth, "Spawn a mob...", button ->
                this.transitionTo(AioaSpawnStudioScreen.create(this))));
        if (!this.inspectorCollapsed) addFloatingWidget(FloatingWindow.INSPECTOR, AioaScreenUtil.button(inspectorX, inspectorContentY + 120, inspectorWidth, "Browse mob types...", button ->
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
        if (!this.inspectorCollapsed) addFloatingWidget(FloatingWindow.INSPECTOR, AioaScreenUtil.button(inspectorX, inspectorContentY + 152, inspectorWidth, "Pick in world (right-click)", button ->
                AioaMobSelectionController.arm(this)));

        int paletteX = this.paletteX + 10;
        int paletteY = this.paletteY + 28;
        AioaBehaviorGraph.NodeType[] types = java.util.Arrays.stream(AioaBehaviorGraph.NodeType.values())
                .sorted(java.util.Comparator.comparing((AioaBehaviorGraph.NodeType type) -> type.category)
                        .thenComparing(Enum::name)).toArray(AioaBehaviorGraph.NodeType[]::new);
        int pageSize = Math.max(3, (this.paletteHeight - 74) / 30);
        int start = this.palettePage * pageSize;
        for (int i = start; !this.paletteCollapsed && i < Math.min(types.length, start + pageSize); i++) {
            AioaBehaviorGraph.NodeType type = types[i];
            String paletteLabel = "[" + type.category + "] " + friendly(type);
            addFloatingWidget(FloatingWindow.PALETTE, AioaScreenUtil.button(paletteX, paletteY, this.paletteWidth - 20, paletteLabel, button -> addNode(type)));
            paletteY += 30;
        }
        if (!this.paletteCollapsed) addFloatingWidget(FloatingWindow.PALETTE, AioaScreenUtil.button(paletteX, this.paletteY + this.paletteHeight - 30, 70, "< Prev", button -> {
            this.palettePage = Math.max(0, this.palettePage - 1); rebuildEditorWidgets();
        }));
        if (!this.paletteCollapsed) addFloatingWidget(FloatingWindow.PALETTE, AioaScreenUtil.button(paletteX + 76, this.paletteY + this.paletteHeight - 30, 70, "Next >", button -> {
            if ((this.palettePage + 1) * pageSize < types.length) this.palettePage++;
            rebuildEditorWidgets();
        }));
        restoreParameterFieldsAfterRebuild();
        updateFloatingWidgetVisibility();
        this.rebuildingWidgets = false;
    }

    private enum FloatingWindow { PALETTE, INSPECTOR, PARAMETERS }

    private <T extends AbstractWidget> T addFloatingWidget(FloatingWindow window, T widget) {
        this.floatingWidgets.computeIfAbsent(window, ignored -> new ArrayList<>()).add(widget);
        return this.addRenderableWidget(widget);
    }

    private void bringToFront(FloatingWindow window) {
        if (this.floatingOrder.get(this.floatingOrder.size() - 1) == window) return;
        this.floatingOrder.remove(window);
        this.floatingOrder.add(window);
        updateFloatingWidgetVisibility();
    }

    private void updateFloatingWidgetVisibility() {
        for (int index = 0; index < this.floatingOrder.size(); index++) {
            FloatingWindow window = this.floatingOrder.get(index);
            boolean collapsed = floatingHeight(window) <= 20;
            for (AbstractWidget widget : this.floatingWidgets.getOrDefault(window, List.of())) {
                boolean covered = collapsed;
                int centerX = widget.getX() + widget.getWidth() / 2;
                int centerY = widget.getY() + widget.getHeight() / 2;
                for (int above = index + 1; !covered && above < this.floatingOrder.size(); above++) {
                    FloatingWindow upper = this.floatingOrder.get(above);
                    covered = centerX >= floatingX(upper) && centerX <= floatingX(upper) + floatingWidth(upper)
                            && centerY >= floatingY(upper) && centerY <= floatingY(upper) + floatingHeight(upper);
                }
                widget.visible = !covered;
            }
        }
    }

    private FloatingWindow floatingAt(double mouseX, double mouseY) {
        for (int index = this.floatingOrder.size() - 1; index >= 0; index--) {
            FloatingWindow window = this.floatingOrder.get(index);
            if (mouseX >= floatingX(window) && mouseX <= floatingX(window) + floatingWidth(window)
                    && mouseY >= floatingY(window) && mouseY <= floatingY(window) + floatingHeight(window)) return window;
        }
        return null;
    }

    private String graphTargetLabel() {
        return switch (this.graph.scope) {
            case ENTITY_TYPE -> "Choose mob type...";
            case SINGLE_ENTITY -> "Pick mob in world...";
            case ENTITY_TAG -> "Choose tagged mob type...";
            case MANAGED_MOBS -> "Target: managed mobs";
            case ALL_MOBS -> "Target: every mob";
        };
    }

    private void selectGraphTarget() {
        if (this.graph.scope == AioaBehaviorGraph.Scope.SINGLE_ENTITY) {
            AioaMobSelectionController.arm(this);
            return;
        }
        if (this.graph.scope == AioaBehaviorGraph.Scope.MANAGED_MOBS || this.graph.scope == AioaBehaviorGraph.Scope.ALL_MOBS) {
            this.status = "This scope selects mobs automatically; no raw id is needed.";
            return;
        }
        this.transitionTo(new AioaEntityPickerScreen(this, "Choose Graph Mob", AioaScreenUtil.allEntityIds(),
                "Pick a mob by its translated name and preview instead of typing a registry id.", "Done", id -> {
            snapshot();
            this.graph.scope = AioaBehaviorGraph.Scope.ENTITY_TYPE;
            this.graph.selector = id.toString();
            this.selector.setValue(this.graph.selector);
            this.status = "Graph target: " + AioaScreenUtil.entityDisplayName(id);
        }));
    }

    private void newGraph() {
        syncFields();
        saveGraphLayout();
        this.graph = AioaBehaviorGraph.createStarter();
        this.graph.name = "Behavior " + (this.graphs.size() + 1);
        this.graphs.add(this.graph);
        this.graphIndex = this.graphs.size() - 1;
        ensureActiveTabVisible();
        this.selected = null;
        this.selectedEdgeId = null;
        this.undo.clear();
        this.redo.clear();
        this.layoutGraphId = "";
        loadGraphLayout();
        this.status = "Created graph " + (this.graphIndex + 1) + " of " + this.graphs.size() + ".";
        markDirty(true);
        rebuildEditorWidgets();
    }

    private void switchGraph(int direction) {
        if (this.graphs.size() < 2) {
            this.status = "Only one graph exists. Use New to add another.";
            return;
        }
        syncFields();
        saveGraphLayout();
        this.graphIndex = Math.floorMod(this.graphIndex + direction, this.graphs.size());
        ensureActiveTabVisible();
        this.graph = this.graphs.get(this.graphIndex);
        this.selected = null;
        this.selectedEdgeId = null;
        this.linkStart = null;
        this.undo.clear();
        this.redo.clear();
        this.layoutGraphId = "";
        loadGraphLayout();
        this.status = "Graph " + (this.graphIndex + 1) + " of " + this.graphs.size() + ": " + this.graph.name;
        rebuildEditorWidgets();
    }

    private void selectGraph(int index) {
        if (index < 0 || index >= this.graphs.size() || index == this.graphIndex) return;
        syncFields();
        saveGraphLayout();
        this.graphIndex = index;
        ensureActiveTabVisible();
        this.graph = this.graphs.get(index);
        this.selected = null;
        this.selectedEdgeId = null;
        this.linkStart = null;
        this.undo.clear();
        this.redo.clear();
        this.layoutGraphId = "";
        loadGraphLayout();
        this.status = "Opened graph tab: " + this.graph.name;
        rebuildEditorWidgets();
    }

    private void closeGraph(int index) {
        if (this.graphs.size() <= 1 || index < 0 || index >= this.graphs.size()) return;
        syncFields();
        saveGraphLayout();
        String removed = this.graphs.remove(index).name;
        this.graphIndex = Math.max(0, Math.min(this.graphs.size() - 1, index > this.graphIndex ? this.graphIndex : this.graphIndex - 1));
        ensureActiveTabVisible();
        this.graph = this.graphs.get(this.graphIndex);
        this.selected = null;
        this.selectedEdgeId = null;
        this.undo.clear();
        this.redo.clear();
        this.layoutGraphId = "";
        loadGraphLayout();
        this.status = "Closed graph tab: " + removed;
        markDirty(true);
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
        this.selectedEdgeId = null;
        this.status = "Added " + friendly(type) + ".";
        loadSelectedParameter(false);
    }

    private void deleteSelected() {
        if (this.selected == null) {
            if (this.selectedEdgeId != null) {
                snapshot();
                this.graph.edges.removeIf(edge -> edge.id.equals(this.selectedEdgeId));
                this.selectedEdgeId = null;
                this.status = "Link disconnected.";
            }
            return;
        }
        if (this.selected.type == AioaBehaviorGraph.NodeType.MOB_BASE) {
            this.status = "Base Mob is required and cannot be deleted.";
            return;
        }
        snapshot();
        String id = this.selected.id;
        this.graph.nodes.removeIf(node -> node.id.equals(id));
        this.graph.edges.removeIf(edge -> edge.from.equals(id) || edge.to.equals(id));
        this.selected = null;
        this.selectedEdgeId = null;
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
        List<String> issues = AioaBehaviorValidator.validate(this.graph);
        this.status = issues.isEmpty() ? "Parameter updated; graph remains valid." : "Saved draft value: " + issues.get(0);
    }

    private void loadSelectedParameter(boolean advance) {
        if (this.selected == null || this.selected.parameters.isEmpty()) return;
        boolean opened = advance && this.parametersCollapsed;
        if (advance) this.parametersCollapsed = false;
        List<String> keys = new ArrayList<>(this.selected.parameters.keySet());
        this.parameterIndex = Math.max(0, Math.min(this.parameterIndex, keys.size() - 1));
        String key = keys.get(this.parameterIndex);
        this.parameterKey.setValue(key);
        this.parameterValue.setValue(this.selected.parameters.getOrDefault(key, ""));
        this.parameterValue.setFocused(true);
        this.status = "Editing " + friendly(this.selected.type) + " / " + key + ". Press Enter or Apply value.";
        if (opened) {
            rebuildEditorWidgets();
            loadSelectedParameter(false);
        }
    }

    private void cycleParameter(int direction) {
        if (this.selected == null || this.selected.parameters.isEmpty()) return;
        setParameterIfPresent();
        int count = this.selected.parameters.size();
        this.parameterIndex = Math.floorMod(this.parameterIndex + direction, count);
        loadSelectedParameter(false);
        rebuildEditorWidgets();
        loadSelectedParameter(false);
    }

    private void setParameterIfPresent() {
        if (this.selected != null && this.parameterKey != null && !this.parameterKey.getValue().isBlank()) {
            String key = this.parameterKey.getValue().trim();
            String value = this.parameterValue.getValue().trim();
            if (!value.equals(this.selected.parameters.get(key))) {
                this.selected.parameters.put(key, value);
                markDirty(true);
            }
        }
    }

    private void restoreParameterFieldsAfterRebuild() {
        if (this.parametersCollapsed || this.selected == null || this.selected.parameters.isEmpty()
                || this.parameterKey == null || this.parameterValue == null) return;
        List<String> keys = new ArrayList<>(this.selected.parameters.keySet());
        this.parameterIndex = Math.max(0, Math.min(this.parameterIndex, keys.size() - 1));
        String key = keys.get(this.parameterIndex);
        this.parameterKey.setValue(key);
        this.parameterValue.setValue(this.selected.parameters.getOrDefault(key, ""));
    }

    private String parameterToolLabel() {
        if (this.selected == null || this.parameterKey == null) return "Parameter tools";
        String key = this.parameterKey.getValue();
        if ("entity".equals(key)) return "Choose mob...";
        if ("name".equals(key) && this.selected.type == AioaBehaviorGraph.NodeType.FIND_PLAYER_NAME) return "Choose online player...";
        if ("name".equals(key) && isVariableNode(this.selected.type)) return "Cycle variable name";
        if (this.selected.type == AioaBehaviorGraph.NodeType.SCRIPT) return "Open script documentation";
        if (isBooleanParameter(this.selected.type, key)) return "Toggle true / false";
        if ("pattern".equals(key)) return "Cycle particle pattern";
        if ("operation".equals(key)) return "Cycle math operation";
        if ("comparison".equals(key)) return "Cycle comparison";
        if ("effect".equals(key)) return "Cycle common effect";
        if ("slot".equals(key)) return "Cycle equipment slot";
        if ("phase".equals(key)) return "Cycle boss phase";
        if ("sound".equals(key)) return "Cycle common sound";
        return "Reset recommended value";
    }

    private void useParameterTool() {
        if (this.selected == null || this.parameterKey == null) return;
        String key = this.parameterKey.getValue();
        if ("entity".equals(key)) {
            this.transitionTo(new AioaEntityPickerScreen(this, "Choose Node Mob", AioaScreenUtil.allEntityIds(),
                    "Select a mob by name; the registry value is stored safely for this node.", "Use Mob", id -> {
                this.parameterValue.setValue(id.toString());
                setParameter();
            }));
            return;
        }
        if ("name".equals(key) && this.selected.type == AioaBehaviorGraph.NodeType.FIND_PLAYER_NAME) {
            this.transitionTo(new AioaPlayerPickerScreen(this, name -> {
                snapshot();
                this.selected.parameters.put("name", name);
                this.status = "Player target set to " + name + ".";
            }));
            return;
        }
        if (this.selected.type == AioaBehaviorGraph.NodeType.SCRIPT) {
            String currentScript = this.parameterValue.getValue();
            this.transitionTo(new AioaScriptEditorScreen(this, currentScript, script -> {
                snapshot();
                this.selected.parameters.put("script", script);
                this.parameterIndex = new ArrayList<>(this.selected.parameters.keySet()).indexOf("script");
                this.status = "Script commands updated.";
            }));
            return;
        }
        if ("name".equals(key) && isVariableNode(this.selected.type)) {
            this.parameterValue.setValue(cycleText(this.parameterValue.getValue(), "value", "counter", "phase", "timer", "distance"));
        } else if (isBooleanParameter(this.selected.type, key)) {
            this.parameterValue.setValue(Boolean.toString(!Boolean.parseBoolean(this.parameterValue.getValue())));
        } else if ("pattern".equals(key)) {
            this.parameterValue.setValue(cycleText(this.parameterValue.getValue(), "circle", "spiral", "burst"));
        } else if ("operation".equals(key)) {
            this.parameterValue.setValue(cycleText(this.parameterValue.getValue(), "add", "subtract", "multiply", "divide", "min", "max"));
        } else if ("comparison".equals(key)) {
            this.parameterValue.setValue(cycleText(this.parameterValue.getValue(), ">=", ">", "<", "<=", "==", "!="));
        } else if ("effect".equals(key)) {
            this.parameterValue.setValue(cycleText(this.parameterValue.getValue(), "minecraft:speed", "minecraft:strength",
                    "minecraft:resistance", "minecraft:regeneration", "minecraft:slowness", "minecraft:weakness"));
        } else if ("slot".equals(key)) {
            this.parameterValue.setValue(cycleText(this.parameterValue.getValue(), "MAINHAND", "OFFHAND", "HEAD", "CHEST", "LEGS", "FEET"));
        } else if ("phase".equals(key)) {
            this.parameterValue.setValue(cycleText(this.parameterValue.getValue(), "1", "2", "3", "4"));
        } else if ("sound".equals(key)) {
            this.parameterValue.setValue(cycleText(this.parameterValue.getValue(), "minecraft:entity.zombie.ambient",
                    "minecraft:entity.ender_dragon.growl", "minecraft:entity.warden.roar", "minecraft:entity.lightning_bolt.thunder"));
        } else {
            AioaBehaviorGraph.Node defaults = new AioaBehaviorGraph.Node("defaults", this.selected.type, 0, 0);
            applyDefaultParameters(defaults);
            this.parameterValue.setValue(defaults.parameters.getOrDefault(key, this.parameterValue.getValue()));
        }
        setParameter();
        rebuildEditorWidgets();
        loadSelectedParameter(false);
    }

    private static String cycleText(String current, String... values) {
        int index = java.util.Arrays.asList(values).indexOf(current);
        return values[(index + 1) % values.length];
    }

    private static boolean isBooleanNode(AioaBehaviorGraph.NodeType type) {
        return switch (type) {
            case SET_AGGRESSIVE, SET_NO_AI, SET_PERSISTENT, SET_GLOWING, SET_SILENT, SET_INVULNERABLE, SET_TARGET_GLOWING -> true;
            default -> false;
        };
    }

    private static boolean isBooleanParameter(AioaBehaviorGraph.NodeType type, String key) {
        if ("value".equals(key) && isBooleanNode(type)) return true;
        return switch (type) {
            case AREA_DAMAGE -> "includeAllies".equals(key);
            case APPLY_EFFECT_SELF, APPLY_EFFECT_TARGET -> "ambient".equals(key) || "particles".equals(key);
            case SUMMON_LIGHTNING -> "atTarget".equals(key) || "visualOnly".equals(key);
            case EXPLOSION -> "atTarget".equals(key) || "breakBlocks".equals(key) || "fire".equals(key);
            default -> false;
        };
    }

    private static boolean isVariableNode(AioaBehaviorGraph.NodeType type) {
        return type == AioaBehaviorGraph.NodeType.SET_VARIABLE || type == AioaBehaviorGraph.NodeType.MATH_VARIABLE
                || type == AioaBehaviorGraph.NodeType.COMPARE_VARIABLE;
    }

    private void connect(AioaBehaviorGraph.Node from, AioaBehaviorGraph.Node to) {
        if (from == null || to == null || from == to) {
            this.status = "A node cannot connect into itself.";
            return;
        }
        boolean duplicate = this.graph.edges.stream().anyMatch(edge -> edge.from.equals(from.id)
                && edge.to.equals(to.id) && edge.output.equals(this.linkOutput) && edge.input.equals(this.linkInput));
        if (duplicate) {
            this.status = "That connection already exists.";
            return;
        }
        snapshot();
        this.graph.edges.add(new AioaBehaviorGraph.Edge(UUID.randomUUID().toString(), from.id, this.linkOutput,
                to.id, this.linkInput));
        this.selectedEdgeId = null;
        this.status = "Connected " + this.linkOutput + " to " + this.linkInput + ".";
    }

    private void snapshot() {
        this.undo.push(this.graph.copy());
        while (this.undo.size() > 40) this.undo.removeLast();
        this.redo.clear();
        markDirty(true);
    }

    private void undo() {
        if (this.undo.isEmpty()) return;
        this.redo.push(this.graph.copy());
        this.graph = this.undo.pop();
        this.graphs.set(this.graphIndex, this.graph);
        this.selected = null;
        this.selectedEdgeId = null;
        markDirty(true);
        this.status = "Undo";
        rebuildEditorWidgets();
    }

    private void redo() {
        if (this.redo.isEmpty()) return;
        this.undo.push(this.graph.copy());
        this.graph = this.redo.pop();
        this.graphs.set(this.graphIndex, this.graph);
        this.selected = null;
        this.selectedEdgeId = null;
        markDirty(true);
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
        AioaBehaviorGraph.Node base = this.graph.nodes.stream()
                .filter(node -> node.type == AioaBehaviorGraph.NodeType.MOB_BASE).findFirst().orElse(null);
        if (base != null) {
            ResourceLocation mobId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            base.parameters.put("entity", mobId == null ? "auto" : mobId.toString());
        }
        this.status = "Bound graph to " + mob.getDisplayName().getString() + ".";
        markDirty(true);
        rebuildEditorWidgets();
    }

    private void syncFields() {
        this.graph.name = this.graphName.getValue().trim();
        if (this.graph.scope == AioaBehaviorGraph.Scope.ENTITY_TAG) this.graph.selector = this.selector.getValue().trim();
        this.graph.sanitize();
        saveGraphLayout();
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
        AioaConfigManager.save(this.editableConfig);
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            AioaClientNetworking.sendGraphUpdate(AioaGraphUpdateHandler.createWorkspaceRequest(this.graphs, false));
        }
        this.graphDirty = false;
        this.runtimeDirty = false;
        this.transitionTo(this.parent);
    }

    private void markDirty(boolean affectsRuntime) {
        this.graphDirty = true;
        this.runtimeDirty |= affectsRuntime;
        this.lastMutationAt = System.currentTimeMillis();
        if (affectsRuntime) this.previewSimulation.reset(this.graph);
    }

    private void autosaveIfReady() {
        long now = System.currentTimeMillis();
        if (!this.graphDirty || now - this.lastMutationAt < 850L || now - this.lastAutosaveAt < 850L) return;
        syncFields();
        this.editableConfig.behaviorGraphs = this.graphs.stream().map(AioaBehaviorGraph::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        AioaConfigManager.save(this.editableConfig);
        boolean valid = this.graphs.stream().allMatch(saved -> AioaBehaviorValidator.validate(saved).isEmpty());
        if (this.runtimeDirty && valid && this.minecraft != null && this.minecraft.getConnection() != null) {
            AioaClientNetworking.sendGraphUpdate(AioaGraphUpdateHandler.createWorkspaceRequest(this.graphs, true));
            this.runtimeDirty = false;
            this.status = "Autosaved and synced.";
        } else if (!valid) {
            this.status = "Draft autosaved locally; fix validation issues before runtime sync.";
        } else {
            this.status = "Autosaved.";
        }
        this.graphDirty = false;
        this.lastAutosaveAt = now;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean eventDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        FloatingWindow focusedWindow = floatingAt(mouseX, mouseY);
        if (focusedWindow != null) bringToFront(focusedWindow);
        if (this.showContextMenu && contextMenuClicked(mouseX, mouseY, button)) return true;
        this.showContextMenu = false;
        int visibleHelpWidth = Math.min(this.helpWidth, this.width - this.helpX);
        int visibleHelpHeight = Math.min(this.helpHeight, this.height - this.helpY);
        if (button == 0 && mouseY >= this.windowY + 2 && mouseY <= this.windowY + 22) {
            int tabStart = this.windowX + 330;
            int visibleTabs = Math.min(visibleTabSlots(), Math.max(0, this.graphs.size() - this.tabOffset));
            List<Integer> hitOrder = new ArrayList<>();
            for (int slot = visibleTabs - 1; slot >= 0; slot--) {
                int index = this.tabOffset + slot;
                if (index != this.graphIndex) hitOrder.add(index);
            }
            if (this.graphIndex >= this.tabOffset && this.graphIndex < this.tabOffset + visibleTabs) hitOrder.add(0, this.graphIndex);
            for (int index : hitOrder) {
                int slot = index - this.tabOffset;
                int tabX = tabStart + slot * 82;
                if (mouseX >= tabX && mouseX < tabX + 108) {
                    if (mouseX >= tabX + 92 && this.graphs.size() > 1) {
                        closeGraph(index);
                        return true;
                    }
                    selectGraph(index);
                    return true;
                }
            }
        }
        if (button == 0 && mouseX >= this.windowX + windowWidth() - 14 && mouseX <= this.windowX + windowWidth()
                && mouseY >= this.windowY + windowHeight() - 14 && mouseY <= this.windowY + windowHeight()) {
            this.resizingWorkspace = true;
            return true;
        }
        if (button == 0) {
            for (FloatingWindow window : FloatingWindow.values()) {
                if (floatingResizeHandleClicked(mouseX, mouseY, window)) {
                    this.resizingFloatingWindow = window;
                    return true;
                }
            }
        }
        if (button == 0 && floatingHeaderClicked(mouseX, mouseY, FloatingWindow.PARAMETERS)) return true;
        if (button == 0 && floatingHeaderClicked(mouseX, mouseY, FloatingWindow.INSPECTOR)) return true;
        if (button == 0 && floatingHeaderClicked(mouseX, mouseY, FloatingWindow.PALETTE)) return true;
        if (button == 0 && mouseX >= this.viewportX + this.viewportWidth - 12 && mouseX <= this.viewportX + this.viewportWidth
                && mouseY >= this.viewportY + this.viewportHeight - 12 && mouseY <= this.viewportY + this.viewportHeight) {
            this.resizingViewport = true;
            return true;
        }
        if (button == 0 && mouseX >= this.viewportX && mouseX <= this.viewportX + this.viewportWidth
                && mouseY >= this.viewportY + 20 && mouseY <= this.viewportY + 39) {
            int localX = (int) mouseX - this.viewportX;
            if (localX >= 7 && localX < 35) { playPreview(); return true; }
            if (localX >= 38 && localX < 72) { pausePreview(); return true; }
            if (localX >= 75 && localX < 109) { stopPreview(); return true; }
            int speedStart = Math.max(114, this.viewportWidth - 92);
            if (localX >= speedStart && localX < speedStart + 28) { setPreviewSpeed(1); return true; }
            if (localX >= speedStart + 30 && localX < speedStart + 58) { setPreviewSpeed(2); return true; }
            if (localX >= speedStart + 60 && localX < speedStart + 88) { setPreviewSpeed(3); return true; }
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
                String clickedOutput = outputAt(hit, mouseX, mouseY);
                String clickedInput = inputAt(hit, mouseX, mouseY);
                if (button == 0 && clickedOutput != null) {
                    this.selected = hit;
                    this.selectedEdgeId = null;
                    this.linkStart = hit;
                    this.linkOutput = clickedOutput;
                    this.linkInput = "exec";
                    this.draggingLink = true;
                    this.linkMouseX = mouseX;
                    this.linkMouseY = mouseY;
                    this.status = "Drag the output port onto another node's input.";
                    return true;
                }
                if (button == 0 && clickedInput != null) {
                    AioaBehaviorGraph.Edge incoming = this.graph.edges.stream()
                            .filter(edge -> edge.to.equals(hit.id) && edge.input.equals(clickedInput))
                            .reduce((first, second) -> second).orElse(null);
                    if (incoming != null) {
                        snapshot();
                        this.graph.edges.removeIf(edge -> edge.id.equals(incoming.id));
                        this.linkStart = findNode(incoming.from);
                        this.linkOutput = incoming.output;
                        this.linkInput = clickedInput;
                        this.draggingLink = this.linkStart != null;
                        this.linkMouseX = mouseX;
                        this.linkMouseY = mouseY;
                        this.selected = hit;
                        this.selectedEdgeId = null;
                        this.status = "Rerouting link; drop it onto another input.";
                        return true;
                    }
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
                this.selectedEdgeId = null;
                long now = System.currentTimeMillis();
                boolean doubleClick = hit.id.equals(this.lastNodeClickId) && now - this.lastNodeClickAt <= 360L;
                if (!hit.id.equals(this.lastNodeClickId)) this.parameterIndex = 0;
                this.lastNodeClickId = hit.id;
                this.lastNodeClickAt = now;
                this.draggingNode = true;
                this.dragOffsetX = (int) mouseX - screenNodeX(hit);
                this.dragOffsetY = (int) mouseY - screenNodeY(hit);
                this.status = hit.type.help;
                loadSelectedParameter(doubleClick);
                return true;
            }
            AioaBehaviorGraph.Edge edge = edgeAt(mouseX, mouseY);
            if (button == 0 && edge != null) {
                this.selected = null;
                this.selectedEdgeId = edge.id;
                this.status = "Selected link " + edge.output + " -> " + edge.input + ". Press Delete to disconnect.";
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
        return super.mouseClicked(event, eventDoubleClick);
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
            this.viewportWidth = Math.max(150, Math.min(this.width - this.viewportX, (int) mouseX - this.viewportX));
            this.viewportHeight = Math.max(100, Math.min(this.height - this.viewportY, (int) mouseY - this.viewportY));
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
        if (this.resizingWorkspace) {
            this.workspaceWidth = Math.max(620, Math.min(this.width - this.windowX, (int) mouseX - this.windowX));
            this.workspaceHeight = Math.max(360, Math.min(this.height - this.windowY, (int) mouseY - this.windowY));
            rebuildEditorWidgets();
            return true;
        }
        if (this.resizingFloatingWindow != null) {
            resizeFloatingWindow(this.resizingFloatingWindow, (int) mouseX, (int) mouseY);
            rebuildEditorWidgets();
            return true;
        }
        if (this.draggingFloatingWindow != null) {
            moveFloatingWindow(this.draggingFloatingWindow, (int) mouseX - this.dragOffsetX, (int) mouseY - this.dragOffsetY);
            rebuildEditorWidgets();
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
            AioaBehaviorGraph.Node target = nodeAt(mouseX, mouseY);
            String targetInput = target == null ? null : inputAt(target, mouseX, mouseY);
            if (target != null && targetInput == null) targetInput = AioaNodeSchema.inputs(target.type).stream().findFirst().orElse(null);
            if (targetInput != null) this.linkInput = targetInput;
            connect(this.linkStart, targetInput == null ? null : target);
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
        this.resizingWorkspace = false;
        this.resizingFloatingWindow = null;
        this.draggingFloatingWindow = null;
        saveGraphLayout();
        markDirty(false);
        return super.mouseReleased(event);
    }

    private boolean floatingHeaderClicked(double mouseX, double mouseY, FloatingWindow window) {
        int x = floatingX(window);
        int y = floatingY(window);
        int width = floatingWidth(window);
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + 20) return false;
        if (mouseX >= x + width - 22) {
            switch (window) {
                case PALETTE -> this.paletteCollapsed = !this.paletteCollapsed;
                case INSPECTOR -> this.inspectorCollapsed = !this.inspectorCollapsed;
                case PARAMETERS -> this.parametersCollapsed = !this.parametersCollapsed;
            }
            rebuildEditorWidgets();
            saveGraphLayout();
        } else {
            this.draggingFloatingWindow = window;
            this.dragOffsetX = (int) mouseX - x;
            this.dragOffsetY = (int) mouseY - y;
        }
        return true;
    }

    private void moveFloatingWindow(FloatingWindow window, int x, int y) {
        x = Math.max(0, Math.min(this.width - floatingWidth(window), x));
        y = Math.max(0, Math.min(this.height - 20, y));
        switch (window) {
            case PALETTE -> { this.paletteX = x; this.paletteY = y; }
            case INSPECTOR -> { this.inspectorX = x; this.inspectorY = y; }
            case PARAMETERS -> { this.parametersX = x; this.parametersY = y; }
        }
    }

    private int floatingX(FloatingWindow window) { return switch (window) {
        case PALETTE -> this.paletteX; case INSPECTOR -> this.inspectorX; case PARAMETERS -> this.parametersX;
    }; }
    private int floatingY(FloatingWindow window) { return switch (window) {
        case PALETTE -> this.paletteY; case INSPECTOR -> this.inspectorY; case PARAMETERS -> this.parametersY;
    }; }
    private int floatingWidth(FloatingWindow window) { return switch (window) {
        case PALETTE -> this.paletteWidth; case INSPECTOR -> this.inspectorWidth; case PARAMETERS -> this.parametersWidth;
    }; }

    private int floatingHeight(FloatingWindow window) { return switch (window) {
        case PALETTE -> this.paletteCollapsed ? 20 : this.paletteHeight;
        case INSPECTOR -> this.inspectorCollapsed ? 20 : this.inspectorHeight;
        case PARAMETERS -> this.parametersCollapsed ? 20 : this.parametersHeight;
    }; }

    private boolean floatingResizeHandleClicked(double mouseX, double mouseY, FloatingWindow window) {
        if (floatingHeight(window) <= 20) return false;
        int right = floatingX(window) + floatingWidth(window);
        int bottom = floatingY(window) + floatingHeight(window);
        return mouseX >= right - 14 && mouseX <= right + 2 && mouseY >= bottom - 14 && mouseY <= bottom + 2;
    }

    private void resizeFloatingWindow(FloatingWindow window, int mouseX, int mouseY) {
        int width = Math.max(window == FloatingWindow.PALETTE ? 130 : 170, mouseX - floatingX(window));
        int minimumHeight = window == FloatingWindow.PALETTE ? 120 : window == FloatingWindow.INSPECTOR ? 240 : 180;
        int height = Math.max(minimumHeight, mouseY - floatingY(window));
        width = Math.min(width, Math.max(180, this.width - floatingX(window)));
        height = Math.min(height, Math.max(120, this.height - floatingY(window)));
        switch (window) {
            case PALETTE -> { this.paletteWidth = width; this.paletteHeight = height; }
            case INSPECTOR -> { this.inspectorWidth = width; this.inspectorHeight = height; }
            case PARAMETERS -> { this.parametersWidth = width; this.parametersHeight = height; }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double delta) {
        if (mouseY >= this.windowY + 2 && mouseY <= this.windowY + 24
                && mouseX >= this.windowX + 320 && mouseX <= this.windowX + windowWidth()) {
            int maxOffset = Math.max(0, this.graphs.size() - visibleTabSlots());
            this.tabOffset = Math.max(0, Math.min(maxOffset, this.tabOffset + (delta < 0 ? 1 : -1)));
            return true;
        }
        if (!this.paletteCollapsed && mouseX >= this.paletteX && mouseX < this.paletteX + this.paletteWidth
                && mouseY >= this.paletteY && mouseY <= this.paletteY + this.paletteHeight) {
            int pageSize = Math.max(3, (this.paletteHeight - 74) / 30);
            int pageCount = Math.max(1, (AioaBehaviorGraph.NodeType.values().length + pageSize - 1) / pageSize);
            this.palettePage = Math.max(0, Math.min(pageCount - 1, this.palettePage + (delta < 0 ? 1 : -1)));
            rebuildEditorWidgets();
            return true;
        }
        if (insideCanvas(mouseX, mouseY)) {
            if (!shiftDown()) {
                double oldZoom = this.canvasZoom;
                double graphX = (mouseX - canvasLeft() - this.canvasPanX) / oldZoom;
                double graphY = (mouseY - canvasTop() - this.canvasPanY) / oldZoom;
                this.canvasZoom = clampZoom(oldZoom + Math.copySign(0.1D, delta));
                this.canvasPanX = (int) Math.round(mouseX - canvasLeft() - graphX * this.canvasZoom);
                this.canvasPanY = (int) Math.round(mouseY - canvasTop() - graphY * this.canvasZoom);
                this.status = "Zoom " + zoomPercent() + "% - the wheel follows the cursor.";
            } else {
                this.canvasPanX += (int) Math.copySign(28, delta);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, delta);
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

    private String outputAt(AioaBehaviorGraph.Node node, double mouseX, double mouseY) {
        int right = screenNodeX(node) + nodeWidth();
        if (Math.abs(mouseX - right) > 9.0D) return null;
        return AioaNodeSchema.outputs(node.type).stream()
                .filter(output -> Math.abs(mouseY - outputPortY(node, output)) <= 7.0D)
                .findFirst().orElse(null);
    }

    private String inputAt(AioaBehaviorGraph.Node node, double mouseX, double mouseY) {
        int left = screenNodeX(node);
        if (Math.abs(mouseX - left) > 9.0D) return null;
        return AioaNodeSchema.inputs(node.type).stream()
                .filter(input -> Math.abs(mouseY - inputPortY(node, input)) <= 7.0D)
                .findFirst().orElse(null);
    }

    private int outputPortY(AioaBehaviorGraph.Node node, String output) {
        List<String> outputs = AioaNodeSchema.outputs(node.type);
        int index = Math.max(0, outputs.indexOf(AioaNodeSchema.normalizeOutput(output)));
        return distributedPortY(node, index, outputs.size());
    }

    private int inputPortY(AioaBehaviorGraph.Node node, String input) {
        List<String> inputs = AioaNodeSchema.inputs(node.type);
        int index = Math.max(0, inputs.indexOf(AioaNodeSchema.normalizeInput(input)));
        return distributedPortY(node, index, Math.max(1, inputs.size()));
    }

    private int distributedPortY(AioaBehaviorGraph.Node node, int index, int count) {
        int top = screenNodeY(node);
        int height = nodeHeight();
        if (count <= 1) return top + height / 2;
        int usable = Math.max(10, height - 21);
        return top + 17 + (int) Math.round(index * (usable / (double) (count - 1)));
    }

    private AioaBehaviorGraph.Edge edgeAt(double mouseX, double mouseY) {
        for (int edgeIndex = this.graph.edges.size() - 1; edgeIndex >= 0; edgeIndex--) {
            AioaBehaviorGraph.Edge edge = this.graph.edges.get(edgeIndex);
            AioaBehaviorGraph.Node from = findNode(edge.from);
            AioaBehaviorGraph.Node to = findNode(edge.to);
            if (from == null || to == null) continue;
            double x1 = screenNodeX(from) + nodeWidth();
            double y1 = outputPortY(from, edge.output);
            double x2 = screenNodeX(to);
            double y2 = inputPortY(to, edge.input);
            double reach = Math.max(36.0D, Math.abs(x2 - x1) / 2.0D);
            double previousX = x1;
            double previousY = y1;
            for (int step = 1; step <= 32; step++) {
                double t = step / 32.0D;
                double inverse = 1.0D - t;
                double x = inverse * inverse * inverse * x1 + 3 * inverse * inverse * t * (x1 + reach)
                        + 3 * inverse * t * t * (x2 - reach) + t * t * t * x2;
                double y = inverse * inverse * inverse * y1 + 3 * inverse * inverse * t * y1
                        + 3 * inverse * t * t * y2 + t * t * t * y2;
                if (distanceToSegment(mouseX, mouseY, previousX, previousY, x, y) <= 5.0D) return edge;
                previousX = x;
                previousY = y;
            }
        }
        return null;
    }

    private static double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 1.0E-9D) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0.0D, Math.min(1.0D, ((px - x1) * dx + (py - y1) * dy) / lengthSquared));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    private int screenNodeX(AioaBehaviorGraph.Node node) { return canvasLeft() + this.canvasPanX + (int) Math.round(node.x * this.canvasZoom); }
    private int screenNodeY(AioaBehaviorGraph.Node node) { return canvasTop() + this.canvasPanY + (int) Math.round(node.y * this.canvasZoom); }
    private int nodeWidth() { return Math.max(64, (int) Math.round(NODE_WIDTH * this.canvasZoom)); }
    private int nodeHeight() { return Math.max(25, (int) Math.round(NODE_HEIGHT * this.canvasZoom)); }
    private int zoomPercent() { return (int) Math.round(this.canvasZoom * 100.0D); }
    private static double clampZoom(double value) { return Math.max(0.05D, Math.min(16.0D, value)); }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == 257 && this.selected != null && (this.parameterKey.isFocused() || this.parameterValue.isFocused())) {
            setParameter();
            return true;
        }
        if (controlDown()) {
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

    private boolean controlDown() {
        if (this.minecraft == null) return false;
        long window = this.minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private boolean shiftDown() {
        if (this.minecraft == null) return false;
        long window = this.minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        autosaveIfReady();
        this.beginUiRender(guiGraphics);
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        int right = this.windowX + windowWidth();
        int bottom = this.windowY + windowHeight();
        AioaScreenUtil.drawPanel(guiGraphics, this.windowX, this.windowY, right, bottom);
        guiGraphics.fill(this.windowX + 1, this.windowY + 1, right - 1, this.windowY + 24, 0xFF18231D);
        guiGraphics.drawString(this.font, "GRAPH: " + this.graph.name + "  :: drag / resize", this.windowX + 10, this.windowY + 8, AioaScreenUtil.TEXT_MAIN);
        drawGraphTabs(guiGraphics, mouseX, mouseY);
        guiGraphics.fill(this.canvasLeft(), this.canvasTop(), this.canvasRight(), this.canvasBottom(), 0xF0090D0B);
        drawGrid(guiGraphics);
        drawGraph(guiGraphics);
        if (this.draggingLink && this.linkStart != null) {
            drawBezier(guiGraphics, screenNodeX(this.linkStart) + nodeWidth(), outputPortY(this.linkStart, this.linkOutput),
                    (int) this.linkMouseX, (int) this.linkMouseY, 0xFF92F5B8);
        }
        drawViewport(guiGraphics, mouseX, mouseY);
        drawFloatingWindows(guiGraphics);
        guiGraphics.fill(this.windowX + 1, bottom - 27, right - 1, bottom - 1, 0xFF111A15);
        guiGraphics.drawString(this.font, this.status, this.windowX + 10, bottom - 18, AioaScreenUtil.TEXT_SUB);
        String workspace = "UI " + this.editableConfig.clientUi.editorScalePercent + "% | Graph " + zoomPercent() + "% | Wheel zoom | Drag grid to pan";
        guiGraphics.drawString(this.font, workspace, Math.max(this.windowX + 10, right - this.font.width(workspace) - 10), bottom - 18, 0xFF76B991);
        guiGraphics.fill(right - 12, bottom - 2, right, bottom, 0xFF6EFFBA);
        guiGraphics.fill(right - 2, bottom - 12, right, bottom, 0xFF6EFFBA);
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
            int y1 = outputPortY(from, edge.output);
            int x2 = screenNodeX(to);
            int y2 = inputPortY(to, edge.input);
            int mid = (x1 + x2) / 2;
            int edgeColor = edge.id.equals(this.selectedEdgeId) ? 0xFFFFC857 : 0xFF01BF63;
            drawBezier(graphics, x1, y1, x2, y2, edgeColor);
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
            for (String input : AioaNodeSchema.inputs(node.type)) {
                int portY = inputPortY(node, input);
                graphics.fill(x - 4, portY - 3, x + 3, portY + 4, 0xFF6EFFBA);
                if (this.canvasZoom >= 0.78D) graphics.drawString(this.font, input, x + 5, portY - 4, 0xFF8ABF9D);
            }
            for (String output : AioaNodeSchema.outputs(node.type)) {
                int portY = outputPortY(node, output);
                graphics.fill(x + nodeWidth - 3, portY - 3, x + nodeWidth + 4, portY + 4, 0xFF6EFFBA);
                if (this.canvasZoom >= 0.78D) {
                    String label = this.font.plainSubstrByWidth(output, Math.max(18, nodeWidth / 3));
                    graphics.drawString(this.font, label, x + nodeWidth - this.font.width(label) - 6, portY - 4, 0xFFB2DDC0);
                }
            }
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
        for (int step = 1; step <= 48; step++) {
            double t = step / 48.0D;
            double inverse = 1.0D - t;
            double x = inverse * inverse * inverse * x1 + 3 * inverse * inverse * t * (x1 + reach)
                    + 3 * inverse * t * t * (x2 - reach) + t * t * t * x2;
            double y = inverse * inverse * inverse * y1 + 3 * inverse * inverse * t * y1
                    + 3 * inverse * t * t * y2 + t * t * t * y2;
            drawThinSegment(graphics, previousX, previousY, x, y, color);
            previousX = x;
            previousY = y;
        }
    }

    private static void drawThinSegment(GuiGraphics graphics, double x1, double y1, double x2, double y2, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        int length = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy)) + 1);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x1, (float) y1);
        graphics.pose().rotate((float) Math.atan2(dy, dx));
        graphics.fill(0, -1, length, 1, color);
        graphics.pose().popMatrix();
    }

    private void drawPaletteScrollBar(GuiGraphics graphics) {
        if (this.paletteCollapsed) return;
        int pageSize = Math.max(3, (this.paletteHeight - 74) / 30);
        int pages = Math.max(1, (AioaBehaviorGraph.NodeType.values().length + pageSize - 1) / pageSize);
        int top = this.paletteY + 28;
        int height = Math.max(30, this.paletteHeight - 64);
        int thumb = Math.max(18, height / pages);
        int y = top + (pages <= 1 ? 0 : (height - thumb) * this.palettePage / (pages - 1));
        graphics.fill(this.paletteX + this.paletteWidth - 5, top, this.paletteX + this.paletteWidth - 2, top + height, 0xFF23342A);
        graphics.fill(this.paletteX + this.paletteWidth - 6, y, this.paletteX + this.paletteWidth - 1, y + thumb, 0xFF5ACB88);
    }

    private void drawFloatingPanel(GuiGraphics graphics, FloatingWindow window, String title, int height) {
        int x = floatingX(window);
        int y = floatingY(window);
        int width = floatingWidth(window);
        graphics.fill(x, y, x + width, y + height, 0xF2101713);
        graphics.fill(x, y, x + width, y + 20, 0xFF1A2B21);
        graphics.fill(x, y, x + 2, y + height, 0xFF396B4D);
        graphics.drawString(this.font, title + "  ::", x + 8, y + 6, AioaScreenUtil.TEXT_MAIN);
        boolean collapsed = switch (window) {
            case PALETTE -> this.paletteCollapsed;
            case INSPECTOR -> this.inspectorCollapsed;
            case PARAMETERS -> this.parametersCollapsed;
        };
        graphics.drawString(this.font, collapsed ? "+" : "-", x + width - 16, y + 6, 0xFF92F5B8);
        if (!collapsed) drawResizeHandle(graphics, x + width, y + height);
    }

    private void drawFloatingWindows(GuiGraphics graphics) {
        for (FloatingWindow window : this.floatingOrder) {
            String title = switch (window) {
                case PALETTE -> "NODE PALETTE";
                case INSPECTOR -> "INSPECTOR :: " + (this.selected == null ? "GRAPH" : friendly(this.selected.type));
                case PARAMETERS -> "NODE PARAMETERS";
            };
            drawFloatingPanel(graphics, window, title, floatingHeight(window));
            if (window == FloatingWindow.INSPECTOR && !this.inspectorCollapsed
                    && this.selected != null && this.inspectorHeight >= 270) {
                AioaScreenUtil.drawWrappedCenteredText(graphics, this.font, Component.literal(this.selected.type.help),
                        this.inspectorX + this.inspectorWidth / 2, this.inspectorY + 216,
                        this.inspectorWidth - 24, AioaScreenUtil.TEXT_SUB);
            }
            if (window == FloatingWindow.PALETTE) drawPaletteScrollBar(graphics);
        }
    }

    private static void drawResizeHandle(GuiGraphics graphics, int right, int bottom) {
        graphics.fill(right - 14, bottom - 2, right, bottom, 0xFF6EFFBA);
        graphics.fill(right - 2, bottom - 14, right, bottom, 0xFF6EFFBA);
        graphics.fill(right - 9, bottom - 5, right - 5, bottom - 3, 0xAA6EFFBA);
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

    private void drawViewport(GuiGraphics graphics, int mouseX, int mouseY) {
        ResourceLocation entityId = previewEntityId();
        int width = Math.min(this.viewportWidth, Math.max(120, this.width - this.viewportX));
        int height = Math.min(this.viewportHeight, Math.max(90, this.height - this.viewportY));
        this.previewSimulation.advance(this.graph, this.previewPlaying, this.previewSpeed);
        AioaScreenUtil.drawEntityViewport(graphics, this.font, this.viewportX, this.viewportY, width, height, entityId, this.previewSimulation.frame(this.previewPlaying));
        drawViewportButton(graphics, this.viewportX + 7, this.viewportY + 22, 28, "PLAY", this.previewPlaying);
        drawViewportButton(graphics, this.viewportX + 38, this.viewportY + 22, 34, "PAUSE", !this.previewPlaying && !this.previewSimulation.stopped());
        drawViewportButton(graphics, this.viewportX + 75, this.viewportY + 22, 34, "STOP", this.previewSimulation.stopped());
        int speedStart = this.viewportX + Math.max(114, width - 92);
        drawViewportButton(graphics, speedStart, this.viewportY + 22, 28, "1x", this.previewSpeed == 1);
        drawViewportButton(graphics, speedStart + 30, this.viewportY + 22, 28, "2x", this.previewSpeed == 2);
        drawViewportButton(graphics, speedStart + 60, this.viewportY + 22, 28, "3x", this.previewSpeed == 3);
        graphics.fill(this.viewportX + width - 10, this.viewportY + height - 2, this.viewportX + width, this.viewportY + height, 0xFF6EFFBA);
        graphics.fill(this.viewportX + width - 2, this.viewportY + height - 10, this.viewportX + width, this.viewportY + height, 0xFF6EFFBA);
    }

    private void playPreview() { this.previewPlaying = true; this.previewSimulation.resume(); this.status = "Viewport simulation playing."; }
    private void pausePreview() { this.previewPlaying = false; this.previewSimulation.pause(); this.status = "Viewport simulation paused."; }
    private void stopPreview() { this.previewPlaying = false; this.previewSimulation.reset(this.graph); this.status = "Viewport simulation stopped and reset."; }
    private void setPreviewSpeed(int speed) { this.previewSpeed = speed; this.status = "Viewport simulation speed: " + speed + "x"; }

    private void drawViewportButton(GuiGraphics graphics, int x, int y, int width, String label, boolean active) {
        graphics.fill(x, y, x + width, y + 14, active ? 0xFF2D8F59 : 0xFF17271E);
        graphics.drawCenteredString(this.font, label, x + width / 2, y + 3, active ? 0xFFFFFFFF : 0xFF9ACBAE);
    }

    private void drawGraphTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabStart = this.windowX + 330;
        int visibleTabs = Math.min(visibleTabSlots(), Math.max(0, this.graphs.size() - this.tabOffset));
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < visibleTabs; slot++) {
            int index = this.tabOffset + slot;
            if ((pass == 0 && index == this.graphIndex) || (pass == 1 && index != this.graphIndex)) continue;
            int x = tabStart + slot * 82;
            int color = index == this.graphIndex ? 0xFF315A42 : 0xFF1E3026;
            graphics.fill(x, this.windowY + 2, x + 108, this.windowY + 22, color);
            graphics.fill(x, this.windowY + 2, x + 108, this.windowY + 4, index == this.graphIndex ? 0xFF6EFFBA : 0xFF396B4D);
            boolean hovered = mouseX >= x && mouseX < x + 108 && mouseY >= this.windowY + 2 && mouseY <= this.windowY + 22;
            String name = this.font.plainSubstrByWidth(this.graphs.get(index).name, hovered ? 80 : 94);
            graphics.drawString(this.font, name, x + 6, this.windowY + 8, index == this.graphIndex ? 0xFFFFFFFF : AioaScreenUtil.TEXT_SUB);
            if (hovered && this.graphs.size() > 1) graphics.drawString(this.font, "x", x + 96, this.windowY + 8, 0xFFFF9A9A);
            }
        }
        if (this.graphs.size() > visibleTabs) {
            String range = (this.tabOffset + 1) + "-" + (this.tabOffset + visibleTabs) + "/" + this.graphs.size();
            graphics.drawString(this.font, range, this.windowX + windowWidth() - this.font.width(range) - 8,
                    this.windowY + 8, 0xFF76B991);
        }
    }

    private int visibleTabSlots() {
        return Math.max(1, Math.min(8, (windowWidth() - 350) / 82));
    }

    private void ensureActiveTabVisible() {
        int slots = visibleTabSlots();
        if (this.graphIndex < this.tabOffset) this.tabOffset = this.graphIndex;
        if (this.graphIndex >= this.tabOffset + slots) this.tabOffset = this.graphIndex - slots + 1;
        this.tabOffset = Math.max(0, Math.min(this.tabOffset, Math.max(0, this.graphs.size() - slots)));
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
        return AioaNodeSchema.outputs(type).toArray(String[]::new);
    }

    private static void applyDefaultParameters(AioaBehaviorGraph.Node node) {
        switch (node.type) {
            case MOB_BASE -> node.parameter("entity", "auto").parameter("health", "20").parameter("damage", "3").parameter("speed", "0.23");
            case EVERY_TICKS, DELAY_TICKS -> node.parameter("ticks", "20");
            case EVERY_SECONDS, DELAY_SECONDS -> node.parameter("seconds", "1");
            case REPEAT_COUNT -> node.parameter("count", "3");
            case RANDOM_CHANCE -> node.parameter("chance", "0.5");
            case COOLDOWN -> node.parameter("ticks", "100");
            case FIND_NEAREST_PLAYER, FIND_NEAREST_ANIMAL, FIND_NEAREST_MOB -> node.parameter("range", "24");
            case FIND_PLAYER_NAME -> node.parameter("name", "Player").parameter("range", "64");
            case FIND_ENTITY_TYPE -> node.parameter("entity", "minecraft:zombie").parameter("range", "24");
            case TARGET_IN_RANGE, ATTACK_TARGET -> node.parameter("range", "3");
            case HEALTH_BELOW -> node.parameter("percent", "0.5");
            case TARGET_HEALTH_BELOW -> node.parameter("percent", "0.5");
            case MOVE_TO_TARGET -> node.parameter("speed", "1.1");
            case FLEE_TARGET -> node.parameter("distance", "12").parameter("speed", "1.1");
            case WALK_BLOCKS -> node.parameter("blocks", "4").parameter("speed", "1.0");
            case WANDER -> node.parameter("radius", "8").parameter("speed", "0.9");
            case ROTATE_DEGREES -> node.parameter("degrees", "90");
            case STRAFE -> node.parameter("forward", "0").parameter("sideways", "1");
            case JUMP -> node.parameter("strength", "0.42");
            case TELEPORT_RELATIVE -> node.parameter("x", "0").parameter("y", "0").parameter("z", "0");
            case TELEPORT_TO_TARGET -> node.parameter("offsetX", "0").parameter("offsetY", "0").parameter("offsetZ", "0");
            case ORBIT_TARGET -> node.parameter("radius", "5").parameter("degrees", "35").parameter("speed", "1.1");
            case DASH_TO_TARGET -> node.parameter("strength", "1.25").parameter("lift", "0.15");
            case KNOCKBACK_TARGET -> node.parameter("strength", "0.6");
            case DAMAGE_TARGET -> node.parameter("amount", "6");
            case AREA_DAMAGE -> node.parameter("radius", "4").parameter("amount", "4").parameter("includeAllies", "false");
            case SET_FIRE_TARGET -> node.parameter("seconds", "4");
            case LAUNCH_TARGET -> node.parameter("horizontal", "0.7").parameter("vertical", "0.65");
            case SET_AGGRESSIVE, SET_NO_AI, SET_PERSISTENT, SET_GLOWING, SET_SILENT, SET_INVULNERABLE, SET_TARGET_GLOWING -> node.parameter("value", "true");
            case SET_CUSTOM_NAME -> node.parameter("name", "AIOA Mob").parameter("visible", "true");
            case SET_MAX_HEALTH -> node.parameter("value", "20");
            case SET_ATTACK_DAMAGE -> node.parameter("value", "3");
            case SET_MOVEMENT_SPEED -> node.parameter("value", "0.23");
            case SET_ARMOR -> node.parameter("value", "0");
            case SET_FOLLOW_RANGE -> node.parameter("value", "32");
            case SET_KNOCKBACK_RESISTANCE -> node.parameter("value", "0");
            case EQUIP_ITEM -> node.parameter("item", "minecraft:iron_sword").parameter("slot", "MAINHAND").parameter("dropChance", "0");
            case SPAWN_MOB -> node.parameter("entity", "minecraft:zombie").parameter("cooldown", "200").parameter("nearbyCap", "8");
            case PLAY_SOUND -> node.parameter("sound", "minecraft:entity.zombie.ambient").parameter("volume", "1").parameter("pitch", "1");
            case APPLY_EFFECT_SELF, APPLY_EFFECT_TARGET -> node.parameter("effect", "minecraft:speed").parameter("duration", "200")
                    .parameter("amplifier", "0").parameter("ambient", "false").parameter("particles", "true");
            case SUMMON_LIGHTNING -> node.parameter("atTarget", "true").parameter("visualOnly", "true");
            case EXPLOSION -> node.parameter("power", "2").parameter("atTarget", "false").parameter("breakBlocks", "false").parameter("fire", "false");
            case SAY_IN_CHAT -> node.parameter("message", "{mob} reached this node").parameter("range", "32");
            case ACTION_BAR -> node.parameter("message", "{mob}: phase changed").parameter("range", "32");
            case PARTICLE_PATTERN -> node.parameter("pattern", "circle").parameter("points", "16").parameter("radius", "1.5");
            case HEAL_SELF, HEAL_TARGET -> node.parameter("amount", "4");
            case SET_VELOCITY -> node.parameter("x", "0").parameter("y", "0.42").parameter("z", "0");
            case ADD_TAG, REMOVE_TAG, HAS_TAG -> node.parameter("tag", "aioa_custom");
            case SET_VARIABLE -> node.parameter("name", "value").parameter("value", "0");
            case MATH_VARIABLE -> node.parameter("name", "value").parameter("operation", "add").parameter("value", "1");
            case COMPARE_VARIABLE -> node.parameter("name", "value").parameter("comparison", ">=").parameter("value", "1");
            case SET_PHASE -> node.parameter("phase", "1");
            case SCRIPT -> node.parameter("script", "say={mob} started; rotate=90; glow=true");
            case SET_BODY_ROTATION, SET_HEAD_ROTATION -> node.parameter("degrees", "0");
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
            case "Values" -> 0xFF346C8C;
            case "Scripting" -> 0xFF6751A6;
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
        syncFields();
        this.editableConfig.behaviorGraphs = this.graphs.stream().map(AioaBehaviorGraph::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        AioaConfigManager.save(this.editableConfig);
        if (this.minecraft != null && this.minecraft.getConnection() != null
                && this.graphs.stream().allMatch(saved -> AioaBehaviorValidator.validate(saved).isEmpty())) {
            AioaClientNetworking.sendGraphUpdate(AioaGraphUpdateHandler.createWorkspaceRequest(this.graphs, true));
        }
        this.transitionTo(this.parent);
    }
}
