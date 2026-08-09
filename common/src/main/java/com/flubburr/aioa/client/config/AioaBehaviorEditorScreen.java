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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.nio.file.Path;

public final class AioaBehaviorEditorScreen extends AioaAnimatedScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
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
    private final Set<String> selectedNodeIds = new LinkedHashSet<>();
    private Map<String, List<String>> nodeValidationIssues = Map.of();
    private Map<String, List<String>> edgeValidationIssues = Map.of();
    private String selectedEdgeId;
    private AioaBehaviorGraph.Node linkStart;
    private AioaBehaviorGraph.Edge reroutingEdge;
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
    private String paletteQuery = "";
    private EditBox paletteSearch;
    private long lastNodeClickAt;
    private String lastNodeClickId = "";
    private int parameterIndex;
    private boolean showContextMenu;
    private AioaBehaviorGraph.Node contextNode;
    private int contextMenuX;
    private int contextMenuY;
    private String activeTopMenu;
    private String activeGroupId;
    private boolean draggingWindow;
    private boolean draggingNode;
    private boolean marqueeSelecting;
    private int marqueeStartX;
    private int marqueeStartY;
    private int marqueeEndX;
    private int marqueeEndY;
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
    private boolean parameterSnapshotTaken;
    private final List<FloatingWindow> floatingOrder = new ArrayList<>(List.of(
            FloatingWindow.PALETTE, FloatingWindow.INSPECTOR));
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
        refreshValidationDiagnostics();
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
        for (String menu : List.of("File", "Edit", "View", "Graph", "Help")) {
            int width = menu.equals("Graph") ? 62 : 54;
            this.addRenderableWidget(AioaScreenUtil.button(x, toolbarY, width, menu, button -> {
                this.activeTopMenu = menu.equals(this.activeTopMenu) ? null : menu;
                this.showContextMenu = false;
            }));
            x += width + 4;
        }

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
        String activeParameter = currentParameterKey();
        String activeValue = this.selected == null || activeParameter.isBlank() ? ""
                : this.selected.parameters.getOrDefault(activeParameter, "");
        this.parameterKey = new EditBox(this.font, parameterContentX, this.parametersY + 28, parameterContentWidth, 22, Component.literal("Parameter name"));
        this.parameterKey.setValue(activeParameter);
        this.parameterKey.setMaxLength(48);
        this.parameterValue = new EditBox(this.font, parameterContentX, this.parametersY + 56, parameterContentWidth, 22, Component.literal("Parameter value"));
        this.parameterValue.setHint(Component.literal("value"));
        this.parameterValue.setMaxLength(8192);
        this.parameterValue.setValue(activeValue);
        if (!this.parametersCollapsed) {
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX, this.parametersY + 28,
                    parameterContentWidth, "Parameter: " + friendlyParameter(activeParameter), button -> cycleParameter(1)));
            boolean visual = addVisualParameterControl(parameterContentX, this.parametersY + 56, parameterContentWidth,
                    activeParameter, activeValue);
            if (!visual) addFloatingWidget(FloatingWindow.PARAMETERS, this.parameterValue);
            int half = (parameterContentWidth - 6) / 2;
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX, this.parametersY + 84, half, "< Parameter", button -> cycleParameter(-1)));
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX + half + 6, this.parametersY + 84, half, "Parameter >", button -> cycleParameter(1)));
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX, this.parametersY + 114, parameterContentWidth, parameterToolLabel(), button -> useParameterTool()));
            addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(parameterContentX, this.parametersY + 144, parameterContentWidth,
                    visual ? "Done" : "Apply text", button -> {
                        if (visual) closeParameterPopover(); else setParameter();
                    }));
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
        if (!this.inspectorCollapsed) {
            int issueCount = AioaBehaviorValidator.validate(this.graph).size();
            addFloatingWidget(FloatingWindow.INSPECTOR, AioaScreenUtil.button(inspectorX, inspectorContentY + 184, inspectorWidth,
                    issueCount == 0 ? "Auto Fix: graph is clean" : "Auto Fix Graph (" + issueCount + ")", button -> autoFixGraph()));
        }

        int paletteX = this.paletteX + 10;
        int paletteY = this.paletteY + 56;
        this.paletteSearch = new EditBox(this.font, paletteX, this.paletteY + 27, this.paletteWidth - 20, 20,
                Component.literal("Search nodes"));
        this.paletteSearch.setValue(this.paletteQuery);
        this.paletteSearch.setHint(Component.literal("Search nodes..."));
        this.paletteSearch.setResponder(value -> {
            this.paletteQuery = value;
            this.palettePage = 0;
            if (!this.rebuildingWidgets) {
                rebuildEditorWidgets();
                if (this.paletteSearch != null) this.paletteSearch.setFocused(true);
            }
        });
        if (!this.paletteCollapsed) addFloatingWidget(FloatingWindow.PALETTE, this.paletteSearch);
        String normalizedQuery = this.paletteQuery.trim().toLowerCase(Locale.ROOT);
        AioaBehaviorGraph.NodeType[] types = java.util.Arrays.stream(AioaBehaviorGraph.NodeType.values())
                .filter(type -> normalizedQuery.isBlank() || type.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || type.category.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || type.help.toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(java.util.Comparator.comparing((AioaBehaviorGraph.NodeType type) -> type.category)
                        .thenComparing(Enum::name)).toArray(AioaBehaviorGraph.NodeType[]::new);
        int pageSize = Math.max(2, (this.paletteHeight - 102) / 30);
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
                for (int above = index + 1; !covered && above < this.floatingOrder.size(); above++) {
                    FloatingWindow upper = this.floatingOrder.get(above);
                    covered = rectanglesOverlap(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                            floatingX(upper), floatingY(upper), floatingWidth(upper), floatingHeight(upper));
                }
                if (!covered && !this.parametersCollapsed) {
                    covered = rectanglesOverlap(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                            this.parametersX, this.parametersY, this.parametersWidth, this.parametersHeight);
                }
                widget.visible = !covered;
            }
        }
    }

    private static boolean rectanglesOverlap(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
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
            case SINGLE_ENTITY -> "Pick exact mob in world...";
            case ENTITY_TAG -> "Edit mob tag above";
            case MANAGED_MOBS -> "Target: managed mobs in area";
            case ALL_MOBS -> "Target: every mob";
        };
    }

    private void selectGraphTarget() {
        if (this.graph.scope == AioaBehaviorGraph.Scope.SINGLE_ENTITY) {
            AioaMobSelectionController.arm(this);
            return;
        }
        if (this.graph.scope == AioaBehaviorGraph.Scope.ENTITY_TAG) {
            this.selector.setFocused(true);
            this.status = "Enter a scoreboard mob tag directly in the Inspector; no mob browser is used for tags.";
            return;
        }
        if (this.graph.scope == AioaBehaviorGraph.Scope.MANAGED_MOBS || this.graph.scope == AioaBehaviorGraph.Scope.ALL_MOBS) {
            this.status = this.graph.scope == AioaBehaviorGraph.Scope.MANAGED_MOBS
                    ? "Managed mobs are selected automatically in the active area."
                    : "Every loaded mob is selected automatically; no picker is needed.";
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
        this.activeGroupId = null;
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
        this.activeGroupId = null;
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
        this.activeGroupId = null;
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
        this.activeGroupId = null;
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
        exportGraph(false);
    }

    private void exportGraphAs() {
        exportGraph(true);
    }

    private void exportGraph(boolean uniqueCopy) {
        syncFields();
        List<String> issues = AioaBehaviorValidator.validate(this.graph);
        if (!issues.isEmpty()) {
            this.status = "Export blocked: " + issues.get(0);
            return;
        }
        try {
            Path directory = AioaGraphLibrary.libraryDirectory(this.editableConfig);
            String slug = this.graph.name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-")
                    .replaceAll("^-+|-+$", "");
            if (slug.isBlank()) slug = "behavior";
            Path suggestion = directory.resolve(slug + (uniqueCopy ? "-copy" : "") + ".aioagraph");
            var chosen = AioaNativeFileDialog.chooseGraphToSave(suggestion);
            if (chosen.isEmpty()) {
                this.status = "Save cancelled.";
                return;
            }
            Path saved = AioaGraphLibrary.exportGraphTo(this.graph, chosen.get());
            this.status = "Saved " + saved.getFileName() + ".";
        } catch (java.io.IOException exception) {
            this.status = "Could not export graph: " + exception.getMessage();
        } catch (RuntimeException exception) {
            this.status = "The system file picker could not open: " + exception.getMessage();
        }
    }

    private void loadNewestGraph() {
        try {
            var chosen = AioaNativeFileDialog.chooseGraphToOpen(AioaGraphLibrary.libraryDirectory(this.editableConfig));
            if (chosen.isEmpty()) {
                this.status = "Import cancelled.";
                return;
            }
            var loaded = AioaGraphLibrary.loadGraph(chosen.get());
            if (loaded.isEmpty()) {
                this.status = "That file is not a valid .aioagraph project.";
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
        } catch (RuntimeException exception) {
            this.status = "The system file picker could not open: " + exception.getMessage();
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
        if (this.activeGroupId != null && type != AioaBehaviorGraph.NodeType.FUNCTION_GROUP) {
            node.parameters.put("_group", this.activeGroupId);
        }
        this.selected = node;
        this.selectedNodeIds.clear();
        this.selectedNodeIds.add(node.id);
        this.selectedEdgeId = null;
        this.status = "Added " + friendly(type) + ".";
        loadSelectedParameter(false);
    }

    private void deleteSelected() {
        if (this.selected == null && this.selectedNodeIds.isEmpty()) {
            if (this.selectedEdgeId != null) {
                snapshot();
                this.graph.edges.removeIf(edge -> edge.id.equals(this.selectedEdgeId));
                this.selectedEdgeId = null;
                this.status = "Link disconnected.";
            }
            return;
        }
        Set<String> requestedIds = new LinkedHashSet<>(this.selectedNodeIds);
        if (this.selected != null) requestedIds.add(this.selected.id);
        boolean protectedBase = this.graph.nodes.stream().anyMatch(node -> requestedIds.contains(node.id)
                && node.type == AioaBehaviorGraph.NodeType.MOB_BASE);
        requestedIds.removeIf(id -> this.graph.nodes.stream().anyMatch(node -> node.id.equals(id)
                && node.type == AioaBehaviorGraph.NodeType.MOB_BASE));
        if (requestedIds.isEmpty()) {
            this.status = "Base Mob is required and cannot be deleted.";
            return;
        }
        snapshot();
        Set<String> groupIds = this.graph.nodes.stream().filter(node -> requestedIds.contains(node.id)
                        && node.type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP)
                .map(node -> node.parameters.get("_groupId")).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> removedIds = this.graph.nodes.stream().filter(node -> requestedIds.contains(node.id)
                        || groupIds.contains(node.parameters.get("_group")))
                .map(node -> node.id).collect(java.util.stream.Collectors.toSet());
        this.graph.nodes.removeIf(node -> removedIds.contains(node.id));
        this.graph.edges.removeIf(edge -> removedIds.contains(edge.from) || removedIds.contains(edge.to));
        this.selected = null;
        this.selectedNodeIds.clear();
        this.selectedEdgeId = null;
        this.linkStart = null;
        this.status = removedIds.size() + " node" + (removedIds.size() == 1 ? "" : "s") + " deleted."
                + (protectedBase ? " Base Mob was kept." : "");
    }

    private void duplicateSelected() {
        Set<String> requestedIds = new LinkedHashSet<>(this.selectedNodeIds);
        if (this.selected != null) requestedIds.add(this.selected.id);
        List<AioaBehaviorGraph.Node> originals = this.graph.nodes.stream().filter(node -> requestedIds.contains(node.id)
                && node.type != AioaBehaviorGraph.NodeType.MOB_BASE).toList();
        if (originals.isEmpty()) {
            this.status = "Base Mob is unique and cannot be duplicated.";
            return;
        }
        snapshot();
        this.selectedNodeIds.clear();
        for (AioaBehaviorGraph.Node original : originals) {
            AioaBehaviorGraph.Node copy = original.copy();
            copy.id = "node_" + UUID.randomUUID().toString().substring(0, 8);
            copy.x += 28;
            copy.y += 28;
            this.graph.nodes.add(copy);
            this.selectedNodeIds.add(copy.id);
            this.selected = copy;
        }
        this.status = originals.size() + " node" + (originals.size() == 1 ? "" : "s") + " duplicated.";
    }

    private void fitGraph() {
        List<AioaBehaviorGraph.Node> visibleNodes = this.graph.nodes.stream().filter(this::isNodeVisible).toList();
        if (visibleNodes.isEmpty()) {
            this.canvasZoom = 1.0D;
            this.canvasPanX = 0;
            this.canvasPanY = 0;
            return;
        }
        int minX = visibleNodes.stream().mapToInt(node -> node.x).min().orElse(0);
        int minY = visibleNodes.stream().mapToInt(node -> node.y).min().orElse(0);
        int maxX = visibleNodes.stream().mapToInt(node -> node.x + NODE_WIDTH).max().orElse(NODE_WIDTH);
        int maxY = visibleNodes.stream().mapToInt(node -> node.y + NODE_HEIGHT).max().orElse(NODE_HEIGHT);
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
        if (advance) {
            this.parametersCollapsed = false;
            this.parameterSnapshotTaken = false;
            this.showContextMenu = false;
            this.activeTopMenu = null;
        }
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
        this.parameterSnapshotTaken = false;
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

    private String currentParameterKey() {
        if (this.selected == null || this.selected.parameters.isEmpty()) return "";
        List<String> keys = new ArrayList<>(this.selected.parameters.keySet());
        this.parameterIndex = Math.max(0, Math.min(this.parameterIndex, keys.size() - 1));
        return keys.get(this.parameterIndex);
    }

    private boolean addVisualParameterControl(int x, int y, int width, String key, String value) {
        if (this.selected == null || key.isBlank() || isFreeTextParameter(this.selected.type, key)) return false;
        NumericSpec numeric = numericSpec(this.selected.type, key);
        if (numeric != null) {
            double initial = parseDouble(value, numeric.minimum);
            if (numeric.integer) {
                addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.intSlider(x, y, width,
                        friendlyParameter(key), (int) numeric.minimum, (int) numeric.maximum, (int) numeric.step,
                        (int) Math.round(initial), changed -> visualParameterChanged(key, Integer.toString(changed))));
            } else {
                addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.decimalSlider(x, y, width,
                        friendlyParameter(key), numeric.minimum, numeric.maximum, numeric.step, initial,
                        changed -> friendlyParameter(key) + ": " + formatNumber(changed),
                        changed -> visualParameterChanged(key, formatNumber(changed))));
            }
            return true;
        }
        addFloatingWidget(FloatingWindow.PARAMETERS, AioaScreenUtil.button(x, y, width,
                visualParameterLabel(key, value), button -> useParameterTool()));
        return true;
    }

    private void visualParameterChanged(String key, String value) {
        if (this.selected == null) return;
        if (!this.rebuildingWidgets && !this.parameterSnapshotTaken) {
            snapshot();
            this.parameterSnapshotTaken = true;
        }
        this.selected.parameters.put(key, value);
        if (this.parameterValue != null) this.parameterValue.setValue(value);
        if (!this.rebuildingWidgets) {
            markDirty(true);
            this.status = friendlyParameter(key) + " set to " + value + ".";
        }
    }

    private String visualParameterLabel(String key, String value) {
        if ("item".equals(key)) return "Browse item icons...";
        if ("entity".equals(key)) return "Choose mob visually...";
        if (isBooleanParameter(this.selected.type, key)) return Boolean.parseBoolean(value) ? "Enabled  [ON]" : "Disabled  [OFF]";
        if ("slot".equals(key)) return "Armor / hand slot: " + friendlyParameter(value);
        if ("effect".equals(key)) return "Effect preset: " + value.replace("minecraft:", "");
        if ("sound".equals(key)) return "Sound preset: " + value.replace("minecraft:", "");
        if ("pattern".equals(key)) return "Pattern: " + friendlyParameter(value);
        if ("operation".equals(key) || "comparison".equals(key) || "phase".equals(key)) {
            return friendlyParameter(key) + ": " + value;
        }
        return friendlyParameter(key) + ": " + value;
    }

    private static boolean isFreeTextParameter(AioaBehaviorGraph.NodeType type, String key) {
        if (key.startsWith("_")) return true;
        if ("script".equals(key) || "message".equals(key) || "text".equals(key) || "tag".equals(key)) return true;
        if ("name".equals(key)) {
            return type == AioaBehaviorGraph.NodeType.SET_CUSTOM_NAME || type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP
                    || type == AioaBehaviorGraph.NodeType.COMMENT;
        }
        return false;
    }

    private static NumericSpec numericSpec(AioaBehaviorGraph.NodeType type, String key) {
        if ("chance".equals(key) || "percent".equals(key) || "dropChance".equals(key)) return new NumericSpec(0, 1, 0.05, false);
        if ("volume".equals(key)) return new NumericSpec(0, 4, 0.05, false);
        if ("pitch".equals(key)) return new NumericSpec(0.1, 2, 0.05, false);
        if ("degrees".equals(key)) return new NumericSpec(-360, 360, 5, false);
        if ("ticks".equals(key) || "duration".equals(key) || "cooldown".equals(key)) return new NumericSpec(0, 72000, 20, true);
        if ("seconds".equals(key)) return new NumericSpec(0, 600, 1, true);
        if ("count".equals(key) || "points".equals(key) || "nearbyCap".equals(key) || "amplifier".equals(key)) {
            return new NumericSpec(0, "amplifier".equals(key) ? 255 : 128, 1, true);
        }
        if ("range".equals(key) || "radius".equals(key) || "distance".equals(key) || "blocks".equals(key)
                || "capRadius".equals(key)) return new NumericSpec(0, 128, 0.5, false);
        if ("speed".equals(key)) return new NumericSpec(0, 4, 0.05, false);
        if ("x".equals(key) || "y".equals(key) || "z".equals(key) || key.startsWith("offset")
                || "forward".equals(key) || "sideways".equals(key) || "horizontal".equals(key)
                || "vertical".equals(key) || "lift".equals(key)) return new NumericSpec(-16, 16, 0.05, false);
        if ("strength".equals(key) || "power".equals(key)) return new NumericSpec(0, 16, 0.05, false);
        if ("amount".equals(key) || "health".equals(key) || "damage".equals(key)) return new NumericSpec(0, 2048, 0.5, false);
        if ("value".equals(key) && !isBooleanNode(type)) {
            if (type == AioaBehaviorGraph.NodeType.SET_MOVEMENT_SPEED || type == AioaBehaviorGraph.NodeType.SET_KNOCKBACK_RESISTANCE) {
                return new NumericSpec(0, type == AioaBehaviorGraph.NodeType.SET_KNOCKBACK_RESISTANCE ? 1 : 4, 0.05, false);
            }
            return new NumericSpec(-2048, 2048, 0.5, false);
        }
        return null;
    }

    private static double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.00001D) return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String friendlyParameter(String value) {
        if (value == null || value.isBlank()) return "Value";
        String text = value.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private record NumericSpec(double minimum, double maximum, double step, boolean integer) { }

    private String parameterToolLabel() {
        if (this.selected == null || this.parameterKey == null) return "Parameter tools";
        String key = this.parameterKey.getValue();
        if ("item".equals(key)) return "Browse all item icons...";
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
        if ("item".equals(key)) {
            ResourceLocation current = AioaEntityHelper.parseResourceLocation(this.parameterValue.getValue());
            this.transitionTo(new AioaItemPickerScreen(this, current, id -> {
                if (!this.parameterSnapshotTaken) {
                    snapshot();
                    this.parameterSnapshotTaken = true;
                }
                this.selected.parameters.put("item", id.toString());
                this.status = "Equipment item set to " + id.getPath().replace('_', ' ') + ".";
                markDirty(true);
            }));
            return;
        }
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
        refreshValidationDiagnostics();
        List<String> issues = AioaBehaviorValidator.validate(this.graph);
        this.status = issues.isEmpty() ? "Graph is valid and ready to run." : issues.get(0) + (issues.size() > 1 ? " (+" + (issues.size() - 1) + ")" : "");
        if (issues.isEmpty()) LOGGER.info("AIOA graph validation passed: '{}' ({} nodes, {} links)", this.graph.name, this.graph.nodes.size(), this.graph.edges.size());
        else LOGGER.warn("AIOA graph validation failed for '{}': {} | node diagnostics={} | link diagnostics={}",
                this.graph.name, issues, this.nodeValidationIssues, this.edgeValidationIssues);
    }

    private void refreshValidationDiagnostics() {
        this.nodeValidationIssues = AioaBehaviorValidator.nodeIssues(this.graph);
        this.edgeValidationIssues = AioaBehaviorValidator.edgeIssues(this.graph);
    }

    private void autoFixGraph() {
        syncFields();
        snapshot();
        int repairs = 0;
        List<AioaBehaviorGraph.Node> bases = this.graph.nodes.stream()
                .filter(node -> node.type == AioaBehaviorGraph.NodeType.MOB_BASE).toList();
        AioaBehaviorGraph.Node base;
        if (bases.isEmpty()) {
            base = new AioaBehaviorGraph.Node("node_" + UUID.randomUUID().toString().substring(0, 8),
                    AioaBehaviorGraph.NodeType.MOB_BASE, 20, 40);
            applyDefaultParameters(base);
            this.graph.nodes.add(0, base);
            repairs++;
        } else {
            base = bases.get(0);
            if (this.graph.nodes.get(0) != base) {
                this.graph.nodes.remove(base);
                this.graph.nodes.add(0, base);
                repairs++;
            }
            if (bases.size() > 1) {
                Set<String> extras = bases.subList(1, bases.size()).stream().map(node -> node.id)
                        .collect(java.util.stream.Collectors.toSet());
                this.graph.nodes.removeIf(node -> extras.contains(node.id));
                this.graph.edges.removeIf(edge -> extras.contains(edge.from) || extras.contains(edge.to));
                repairs += extras.size();
            }
        }

        Set<String> nodeIds = new HashSet<>();
        for (AioaBehaviorGraph.Node node : this.graph.nodes) {
            if (node.id == null || node.id.isBlank() || !nodeIds.add(node.id)) {
                node.id = "node_" + UUID.randomUUID().toString().substring(0, 8);
                nodeIds.add(node.id);
                repairs++;
            }
            AioaBehaviorGraph.Node defaults = new AioaBehaviorGraph.Node("defaults", node.type, node.x, node.y);
            applyDefaultParameters(defaults);
            for (Map.Entry<String, String> entry : defaults.parameters.entrySet()) {
                if (node.parameters.getOrDefault(entry.getKey(), "").isBlank()) {
                    node.parameters.put(entry.getKey(), entry.getValue());
                    repairs++;
                }
            }
            Set<String> supported = AioaBehaviorValidator.supportedParameters(node.type);
            int before = node.parameters.size();
            node.parameters.keySet().removeIf(key -> !key.startsWith("_") && !supported.contains(key));
            repairs += before - node.parameters.size();
        }

        Map<String, List<String>> parameterIssues = AioaBehaviorValidator.nodeIssues(this.graph);
        for (AioaBehaviorGraph.Node node : new ArrayList<>(this.graph.nodes)) {
            List<String> issues = parameterIssues.getOrDefault(node.id, List.of());
            if (issues.isEmpty()) continue;
            AioaBehaviorGraph.Node defaults = new AioaBehaviorGraph.Node("defaults", node.type, node.x, node.y);
            applyDefaultParameters(defaults);
            for (Map.Entry<String, String> entry : defaults.parameters.entrySet()) {
                String key = entry.getKey();
                boolean invalid = issues.stream().anyMatch(issue -> issue.contains("needs " + key + " ")
                        || ("entity".equals(key) && issue.contains("valid mob selection"))
                        || ("item".equals(key) && issue.contains("valid registered item"))
                        || ("effect".equals(key) && issue.contains("valid effect")));
                if (invalid && !entry.getValue().equals(node.parameters.put(key, entry.getValue()))) repairs++;
            }
            if (node.type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP && issues.stream().anyMatch(issue -> issue.contains("empty"))) {
                String groupId = node.parameters.get("_groupId");
                AioaBehaviorGraph.Node note = new AioaBehaviorGraph.Node("node_" + UUID.randomUUID().toString().substring(0, 8),
                        AioaBehaviorGraph.NodeType.COMMENT, node.x + 180, node.y).parameter("text", "Add function nodes here");
                note.parameters.put("_group", groupId);
                this.graph.nodes.add(note);
                repairs++;
            }
        }

        Map<String, AioaBehaviorGraph.Node> nodesById = this.graph.nodes.stream()
                .collect(java.util.stream.Collectors.toMap(node -> node.id, node -> node, (first, ignored) -> first));
        int edgeCount = this.graph.edges.size();
        this.graph.edges.removeIf(edge -> !nodesById.containsKey(edge.from) || !nodesById.containsKey(edge.to) || edge.from.equals(edge.to));
        repairs += edgeCount - this.graph.edges.size();
        Set<String> edgeIds = new HashSet<>();
        for (AioaBehaviorGraph.Edge edge : this.graph.edges) {
            if (edge.id == null || edge.id.isBlank() || !edgeIds.add(edge.id)) {
                edge.id = UUID.randomUUID().toString();
                edgeIds.add(edge.id);
                repairs++;
            }
            AioaBehaviorGraph.Node source = nodesById.get(edge.from);
            AioaBehaviorGraph.Node target = nodesById.get(edge.to);
            if (!AioaNodeSchema.emits(source.type, edge.output)) {
                edge.output = AioaNodeSchema.outputs(source.type).get(0);
                repairs++;
            }
            if (!AioaNodeSchema.accepts(target.type, edge.input)) {
                edge.input = AioaNodeSchema.inputs(target.type).stream().findFirst().orElse("exec");
                repairs++;
            }
        }

        if (this.graph.selector.isBlank() && (this.graph.scope == AioaBehaviorGraph.Scope.ENTITY_TYPE
                || this.graph.scope == AioaBehaviorGraph.Scope.ENTITY_TAG || this.graph.scope == AioaBehaviorGraph.Scope.SINGLE_ENTITY)) {
            this.graph.scope = AioaBehaviorGraph.Scope.MANAGED_MOBS;
            repairs++;
        }

        refreshValidationDiagnostics();
        AioaBehaviorGraph.Node chain = base;
        List<AioaBehaviorGraph.Node> disconnected = this.graph.nodes.stream()
                .filter(node -> this.nodeValidationIssues.getOrDefault(node.id, List.of()).stream()
                        .anyMatch(issue -> issue.contains("not connected")))
                .sorted(java.util.Comparator.comparingInt((AioaBehaviorGraph.Node node) -> node.x).thenComparingInt(node -> node.y)).toList();
        for (AioaBehaviorGraph.Node node : disconnected) {
            if (node == base || node.type == AioaBehaviorGraph.NodeType.MOB_BASE) continue;
            String output = AioaNodeSchema.outputs(chain.type).contains("next") ? "next" : AioaNodeSchema.outputs(chain.type).get(0);
            this.graph.edges.add(new AioaBehaviorGraph.Edge(UUID.randomUUID().toString(), chain.id, output, node.id, "exec"));
            chain = node;
            repairs++;
        }

        refreshValidationDiagnostics();
        List<String> remaining = AioaBehaviorValidator.validate(this.graph);
        markDirty(true);
        rebuildEditorWidgets();
        this.status = remaining.isEmpty()
                ? "Auto Fix completed " + repairs + " safe repair" + (repairs == 1 ? "" : "s") + ". Graph is valid."
                : "Auto Fix completed " + repairs + " repairs; " + remaining.size() + " creator decision" + (remaining.size() == 1 ? " remains." : "s remain.");
        LOGGER.info("AIOA Auto Fix '{}' applied {} repairs; remaining issues={}", this.graph.name, repairs, remaining);
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
        refreshValidationDiagnostics();
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.activeTopMenu != null && topMenuClicked(mouseX, mouseY, button)) return true;
        if (this.activeTopMenu != null && !(mouseY >= this.windowY + 28 && mouseY <= this.windowY + 52)) {
            this.activeTopMenu = null;
        }
        if (this.showContextMenu) {
            if (contextMenuClicked(mouseX, mouseY, button)) return true;
            this.showContextMenu = false;
            this.contextNode = null;
        }
        if (!this.parametersCollapsed) {
            boolean insideParameters = mouseX >= this.parametersX && mouseX <= this.parametersX + this.parametersWidth
                    && mouseY >= this.parametersY && mouseY <= this.parametersY + this.parametersHeight;
            if (button == 0 && insideParameters && mouseY <= this.parametersY + 20
                    && mouseX >= this.parametersX + this.parametersWidth - 22) {
                closeParameterPopover();
                return true;
            }
            if (insideParameters) return super.mouseClicked(mouseX, mouseY, button);
            if (button == 0) closeParameterPopover();
        }
        FloatingWindow focusedWindow = floatingAt(mouseX, mouseY);
        if (focusedWindow != null) bringToFront(focusedWindow);
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
            for (FloatingWindow window : this.floatingOrder) {
                if (floatingResizeHandleClicked(mouseX, mouseY, window)) {
                    this.resizingFloatingWindow = window;
                    return true;
                }
            }
        }
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
                        this.reroutingEdge = incoming.copy();
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
                    if (!this.parametersCollapsed) closeParameterPopover();
                    this.activeTopMenu = null;
                    this.selected = hit;
                    if (!this.selectedNodeIds.contains(hit.id)) {
                        this.selectedNodeIds.clear();
                        this.selectedNodeIds.add(hit.id);
                    }
                    this.selectedEdgeId = null;
                    this.contextNode = hit;
                    this.showContextMenu = true;
                    this.contextMenuX = Math.min((int) mouseX, this.width - 168);
                    this.contextMenuY = Math.min((int) mouseY, this.height - 152);
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
                if (Screen.hasShiftDown()) {
                    if (!this.selectedNodeIds.add(hit.id)) {
                        this.selectedNodeIds.remove(hit.id);
                        if (this.selected == hit) this.selected = null;
                        this.status = this.selectedNodeIds.size() + " nodes selected.";
                        return true;
                    }
                } else if (!this.selectedNodeIds.contains(hit.id)) {
                    this.selectedNodeIds.clear();
                    this.selectedNodeIds.add(hit.id);
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
                if (doubleClick && hit.type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP) {
                    this.activeGroupId = hit.parameters.get("_groupId");
                    this.selected = null;
                    this.parametersCollapsed = true;
                    this.status = "Entered function group " + hit.parameters.getOrDefault("name", "Group") + ". Add and link nodes normally; use Graph > Exit group when done.";
                    rebuildEditorWidgets();
                    return true;
                }
                if (doubleClick) {
                    this.parametersX = Math.max(0, Math.min(this.width - this.parametersWidth, (int) mouseX + 12));
                    this.parametersY = Math.max(0, Math.min(this.height - this.parametersHeight, (int) mouseY + 10));
                }
                loadSelectedParameter(doubleClick);
                return true;
            }
            AioaBehaviorGraph.Edge edge = edgeAt(mouseX, mouseY);
            if (button == 0 && edge != null) {
                snapshot();
                this.reroutingEdge = edge.copy();
                this.graph.edges.removeIf(existing -> existing.id.equals(edge.id));
                this.linkStart = findNode(edge.from);
                this.linkOutput = edge.output;
                this.linkInput = edge.input;
                this.draggingLink = this.linkStart != null;
                this.linkMouseX = mouseX;
                this.linkMouseY = mouseY;
                this.selected = this.linkStart;
                this.selectedEdgeId = null;
                this.status = "Link picked up. Drop it on a new input, or release on empty space to cancel.";
                return true;
            }
            if (button == 1) {
                if (!this.parametersCollapsed) closeParameterPopover();
                this.activeTopMenu = null;
                this.contextNode = null;
                this.showContextMenu = true;
                this.contextMenuX = Math.min((int) mouseX, this.width - 132);
                this.contextMenuY = Math.min((int) mouseY, this.height - 132);
                return true;
            }
            if (button == 0 && Screen.hasShiftDown()) {
                this.marqueeSelecting = true;
                this.marqueeStartX = this.marqueeEndX = (int) mouseX;
                this.marqueeStartY = this.marqueeEndY = (int) mouseY;
                this.status = "Drag a box around nodes to add them to the selection.";
                return true;
            }
            if (button == 0 || button == 2) {
                if (button == 0) {
                    this.selected = null;
                    this.selectedNodeIds.clear();
                }
                this.draggingCanvas = true;
                this.dragOffsetX = (int) mouseX - this.canvasPanX;
                this.dragOffsetY = (int) mouseY - this.canvasPanY;
                this.status = "Moving canvas - release to finish.";
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.marqueeSelecting) {
            this.marqueeEndX = (int) mouseX;
            this.marqueeEndY = (int) mouseY;
            return true;
        }
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
            int nextX = (int) Math.round(((int) mouseX - this.dragOffsetX - canvasLeft() - this.canvasPanX) / this.canvasZoom);
            int nextY = (int) Math.round(((int) mouseY - this.dragOffsetY - canvasTop() - this.canvasPanY) / this.canvasZoom);
            int dx = nextX - this.selected.x;
            int dy = nextY - this.selected.y;
            if (this.selectedNodeIds.isEmpty()) this.selectedNodeIds.add(this.selected.id);
            for (AioaBehaviorGraph.Node node : this.graph.nodes) {
                if (this.selectedNodeIds.contains(node.id)) {
                    node.x += dx;
                    node.y += dy;
                }
            }
            return true;
        }
        if (this.draggingCanvas) {
            this.canvasPanX = (int) mouseX - this.dragOffsetX;
            this.canvasPanY = (int) mouseY - this.dragOffsetY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingLink) {
            AioaBehaviorGraph.Node target = nodeAt(mouseX, mouseY);
            String targetInput = target == null ? null : inputAt(target, mouseX, mouseY);
            if (target != null && targetInput == null) targetInput = AioaNodeSchema.inputs(target.type).stream().findFirst().orElse(null);
            if (targetInput != null) {
                this.linkInput = targetInput;
                connect(this.linkStart, target);
            } else if (this.reroutingEdge != null) {
                this.graph.edges.add(this.reroutingEdge.copy());
                this.status = "Reroute cancelled; the original link was restored.";
            } else {
                this.status = "Link cancelled.";
            }
            this.draggingLink = false;
            this.linkStart = null;
            this.reroutingEdge = null;
            rebuildEditorWidgets();
            return true;
        }
        if (this.marqueeSelecting) {
            int left = Math.min(this.marqueeStartX, this.marqueeEndX);
            int right = Math.max(this.marqueeStartX, this.marqueeEndX);
            int top = Math.min(this.marqueeStartY, this.marqueeEndY);
            int bottom = Math.max(this.marqueeStartY, this.marqueeEndY);
            for (AioaBehaviorGraph.Node node : this.graph.nodes) {
                if (isNodeVisible(node) && screenNodeX(node) < right && screenNodeX(node) + nodeWidth() > left
                        && screenNodeY(node) < bottom && screenNodeY(node) + nodeHeight() > top) {
                    this.selectedNodeIds.add(node.id);
                    if (this.selected == null) this.selected = node;
                }
            }
            this.marqueeSelecting = false;
            this.status = this.selectedNodeIds.size() + " node" + (this.selectedNodeIds.size() == 1 ? "" : "s") + " selected.";
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
        return super.mouseReleased(mouseX, mouseY, button);
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
            if (!Screen.hasShiftDown()) {
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
            if (!isNodeVisible(node)) continue;
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
            if (from == null || to == null || !isNodeVisible(from) || !isNodeVisible(to)) continue;
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && this.selected != null && (this.parameterKey.isFocused() || this.parameterValue.isFocused())) {
            setParameter();
            return true;
        }
        if (Screen.hasControlDown()) {
            if (keyCode == 65) {
                this.selectedNodeIds.clear();
                this.graph.nodes.stream().filter(this::isNodeVisible).forEach(node -> this.selectedNodeIds.add(node.id));
                this.selected = this.graph.nodes.stream().filter(this::isNodeVisible).findFirst().orElse(null);
                this.status = this.selectedNodeIds.size() + " nodes selected.";
                return true;
            }
            if (keyCode == 90) { undo(); return true; }
            if (keyCode == 89) { redo(); return true; }
            if (keyCode == 83) { exportGraph(false); return true; }
            if (keyCode == 78) { newGraph(); return true; }
            if (keyCode == 68) { duplicateSelected(); return true; }
            if (keyCode == 48) { fitGraph(); return true; }
        }
        if (keyCode == 261) { deleteSelected(); return true; }
        if (keyCode == 72) { this.showHelp = !this.showHelp; return true; }
        if (keyCode == 70) { fitGraph(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        String graphTitle = "GRAPH: " + this.graph.name + (this.activeGroupId == null ? "" : "  >  " + activeGroupName()) + "  :: drag / resize";
        guiGraphics.drawString(this.font, this.font.plainSubstrByWidth(graphTitle, 310), this.windowX + 10, this.windowY + 8, AioaScreenUtil.TEXT_MAIN);
        drawGraphTabs(guiGraphics, mouseX, mouseY);
        guiGraphics.fill(this.canvasLeft(), this.canvasTop(), this.canvasRight(), this.canvasBottom(), 0xF0090D0B);
        drawGrid(guiGraphics);
        drawGraph(guiGraphics);
        if (this.marqueeSelecting) {
            int left = Math.min(this.marqueeStartX, this.marqueeEndX);
            int rightBox = Math.max(this.marqueeStartX, this.marqueeEndX);
            int top = Math.min(this.marqueeStartY, this.marqueeEndY);
            int bottomBox = Math.max(this.marqueeStartY, this.marqueeEndY);
            guiGraphics.fill(left, top, rightBox, bottomBox, 0x332FEA83);
            guiGraphics.renderOutline(left, top, Math.max(1, rightBox - left), Math.max(1, bottomBox - top), 0xFF6EFFBA);
        }
        if (this.draggingLink && this.linkStart != null) {
            drawBezier(guiGraphics, screenNodeX(this.linkStart) + nodeWidth(), outputPortY(this.linkStart, this.linkOutput),
                    (int) this.linkMouseX, (int) this.linkMouseY, 0xFF92F5B8);
        }
        drawViewport(guiGraphics, mouseX, mouseY);
        drawFloatingWindows(guiGraphics);
        if (!this.parametersCollapsed) drawParameterPopover(guiGraphics);
        guiGraphics.fill(this.windowX + 1, bottom - 27, right - 1, bottom - 1, 0xFF111A15);
        guiGraphics.drawString(this.font, this.status, this.windowX + 10, bottom - 18, AioaScreenUtil.TEXT_SUB);
        String workspace = "UI " + this.editableConfig.clientUi.editorScalePercent + "% | Graph " + zoomPercent()
                + "% | Shift-drag select | Ctrl+A all | " + this.selectedNodeIds.size() + " selected";
        guiGraphics.drawString(this.font, workspace, Math.max(this.windowX + 10, right - this.font.width(workspace) - 10), bottom - 18, 0xFF76B991);
        guiGraphics.fill(right - 12, bottom - 2, right, bottom, 0xFF6EFFBA);
        guiGraphics.fill(right - 2, bottom - 12, right, bottom, 0xFF6EFFBA);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!this.parametersCollapsed) drawParameterVisualForeground(guiGraphics);
        if (this.parametersCollapsed) drawNodeTooltip(guiGraphics, mouseX, mouseY);
        if (this.showHelp && this.parametersCollapsed) drawHelp(guiGraphics);
        if (this.showContextMenu) drawContextMenu(guiGraphics);
        if (this.activeTopMenu != null) drawTopMenu(guiGraphics);
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
        drawGroupFrames(graphics);
        for (AioaBehaviorGraph.Edge edge : this.graph.edges) {
            AioaBehaviorGraph.Node from = findNode(edge.from);
            AioaBehaviorGraph.Node to = findNode(edge.to);
            if (from == null || to == null || !isNodeVisible(from) || !isNodeVisible(to)) continue;
            int x1 = screenNodeX(from) + nodeWidth();
            int y1 = outputPortY(from, edge.output);
            int x2 = screenNodeX(to);
            int y2 = inputPortY(to, edge.input);
            int mid = (x1 + x2) / 2;
            boolean badLink = this.edgeValidationIssues.containsKey(edge.id);
            int edgeColor = badLink ? 0xFFFF405C : edge.id.equals(this.selectedEdgeId) ? 0xFF45A9FF : 0xFF01BF63;
            drawBezier(graphics, x1, y1, x2, y2, edgeColor);
            graphics.drawString(this.font, edge.output == null ? "next" : edge.output, mid + 4,
                    Math.min(y1, y2) + Math.abs(y2 - y1) / 2 - 4, 0xFF9AD6AE);
        }
        for (AioaBehaviorGraph.Node node : this.graph.nodes) {
            if (!isNodeVisible(node)) continue;
            int x = screenNodeX(node);
            int y = screenNodeY(node);
            int nodeWidth = nodeWidth();
            int nodeHeight = nodeHeight();
            boolean selectedNode = this.selectedNodeIds.contains(node.id) || node == this.selected;
            boolean badNode = this.nodeValidationIssues.containsKey(node.id);
            AioaScreenUtil.drawInsetPanel(graphics, x, y, x + nodeWidth, y + nodeHeight, selectedNode);
            if (badNode) {
                int pulse = 150 + (int) (Math.sin(System.currentTimeMillis() / 130.0D) * 70.0D);
                int red = (pulse << 24) | 0x00FF405C;
                graphics.fill(x, y, x + nodeWidth, y + 2, red);
                graphics.fill(x, y + nodeHeight - 2, x + nodeWidth, y + nodeHeight, red);
                graphics.fill(x, y, x + 2, y + nodeHeight, red);
                graphics.fill(x + nodeWidth - 2, y, x + nodeWidth, y + nodeHeight, red);
                graphics.drawString(this.font, "!", x + nodeWidth - 10, y + 4, 0xFFFFFFFF);
                String explanation = this.nodeValidationIssues.get(node.id).get(0);
                int bubbleWidth = Math.min(210, Math.max(92, this.font.width(explanation) + 12));
                int bubbleX = Math.max(canvasLeft() + 2, Math.min(canvasRight() - bubbleWidth - 2, x));
                int bubbleY = Math.max(canvasTop() + 2, y - 17);
                graphics.fill(bubbleX, bubbleY, bubbleX + bubbleWidth, bubbleY + 14, 0xF5221014);
                graphics.fill(bubbleX, bubbleY + 13, bubbleX + bubbleWidth, bubbleY + 14, 0xFFFF405C);
                graphics.drawString(this.font, this.font.plainSubstrByWidth(explanation, bubbleWidth - 10), bubbleX + 5, bubbleY + 3, 0xFFFFCED5);
            }
            graphics.fill(x + 1, y + 1, x + nodeWidth - 1, y + Math.min(14, nodeHeight - 2), colorFor(node.type.category));
            String nodeLabel = node.parameters.getOrDefault("_label", friendly(node.type));
            graphics.drawString(this.font, this.font.plainSubstrByWidth(nodeLabel, nodeWidth - 10), x + 6, y + 4, 0xFFFFFFFF);
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
            if (selectedNode) {
                int pulse = 70 + (int) (Math.sin(System.currentTimeMillis() / 120.0D) * 35.0D);
                graphics.fill(x + 3, y + nodeHeight - 4, x + nodeWidth - 3, y + nodeHeight - 2, (pulse << 24) | 0x006EFFBA);
            }
        }
        graphics.disableScissor();
    }

    private void drawGroupFrames(GuiGraphics graphics) {
        if (this.activeGroupId == null) return;
        this.graph.nodes.stream().map(node -> node.parameters.get("_group")).filter(java.util.Objects::nonNull)
                .filter(name -> !name.isBlank()).distinct().forEach(group -> {
                    List<AioaBehaviorGraph.Node> members = this.graph.nodes.stream()
                            .filter(node -> group.equals(node.parameters.get("_group"))).toList();
                    if (members.isEmpty()) return;
                    int left = members.stream().mapToInt(this::screenNodeX).min().orElse(0) - 16;
                    int top = members.stream().mapToInt(this::screenNodeY).min().orElse(0) - 28;
                    int right = members.stream().mapToInt(node -> screenNodeX(node) + nodeWidth()).max().orElse(0) + 16;
                    int bottom = members.stream().mapToInt(node -> screenNodeY(node) + nodeHeight()).max().orElse(0) + 16;
                    graphics.fill(left, top, right, top + 18, 0xAA25573B);
                    graphics.fill(left, top + 18, left + 1, bottom, 0x886EFFBA);
                    graphics.fill(right - 1, top + 18, right, bottom, 0x886EFFBA);
                    graphics.fill(left, bottom - 1, right, bottom, 0x886EFFBA);
                    graphics.drawString(this.font, "[GROUP] " + this.font.plainSubstrByWidth(group, Math.max(20, right - left - 58)),
                            left + 7, top + 5, 0xFFD6FFE3);
                });
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
        graphics.pose().pushPose();
        graphics.pose().translate(x1, y1, 0.0D);
        graphics.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        graphics.fill(0, -1, length, 1, color);
        graphics.pose().popPose();
    }

    private void drawPaletteScrollBar(GuiGraphics graphics) {
        if (this.paletteCollapsed) return;
        int pageSize = Math.max(2, (this.paletteHeight - 102) / 30);
        String query = this.paletteQuery.trim().toLowerCase(Locale.ROOT);
        long matching = java.util.Arrays.stream(AioaBehaviorGraph.NodeType.values()).filter(type -> query.isBlank()
                || type.name().toLowerCase(Locale.ROOT).contains(query)
                || type.category.toLowerCase(Locale.ROOT).contains(query)
                || type.help.toLowerCase(Locale.ROOT).contains(query)).count();
        int pages = Math.max(1, (int) ((matching + pageSize - 1) / pageSize));
        int top = this.paletteY + 56;
        int height = Math.max(30, this.paletteHeight - 92);
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

    private void drawParameterPopover(GuiGraphics graphics) {
        graphics.fill(this.parametersX, this.parametersY, this.parametersX + this.parametersWidth,
                this.parametersY + this.parametersHeight, 0xFC101713);
        graphics.fill(this.parametersX, this.parametersY, this.parametersX + this.parametersWidth,
                this.parametersY + 20, 0xFF1A2B21);
        graphics.fill(this.parametersX, this.parametersY, this.parametersX + 2,
                this.parametersY + this.parametersHeight, 0xFF6EFFBA);
        String title = this.selected == null ? "NODE SETTINGS" : "SETTINGS :: "
                + this.selected.parameters.getOrDefault("_label", friendly(this.selected.type));
        graphics.drawString(this.font, this.font.plainSubstrByWidth(title, this.parametersWidth - 36),
                this.parametersX + 8, this.parametersY + 6, AioaScreenUtil.TEXT_MAIN);
        graphics.drawString(this.font, "x", this.parametersX + this.parametersWidth - 16,
                this.parametersY + 6, 0xFFFF9A9A);
    }

    private void drawParameterVisualForeground(GuiGraphics graphics) {
        if (this.selected == null || !"item".equals(currentParameterKey())) return;
        ResourceLocation id = AioaEntityHelper.parseResourceLocation(this.selected.parameters.getOrDefault("item", "minecraft:air"));
        if (id == null) return;
        graphics.renderItem(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)),
                this.parametersX + 14, this.parametersY + 59);
    }

    private void closeParameterPopover() {
        setParameterIfPresent();
        this.parametersCollapsed = true;
        rebuildEditorWidgets();
    }

    private static void drawResizeHandle(GuiGraphics graphics, int right, int bottom) {
        graphics.fill(right - 14, bottom - 2, right, bottom, 0xFF6EFFBA);
        graphics.fill(right - 2, bottom - 14, right, bottom, 0xFF6EFFBA);
        graphics.fill(right - 9, bottom - 5, right - 5, bottom - 3, 0xAA6EFFBA);
    }

    private void drawContextMenu(GuiGraphics graphics) {
        String[] actions = contextActions();
        int width = this.contextNode == null ? 154 : 166;
        graphics.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + width,
                this.contextMenuY + 8 + actions.length * 20, 0xFA111A15);
        for (int i = 0; i < actions.length; i++) {
            int y = this.contextMenuY + 4 + i * 20;
            graphics.fill(this.contextMenuX + 3, y, this.contextMenuX + width - 3, y + 18, 0xFF1B2921);
            graphics.drawString(this.font, actions[i], this.contextMenuX + 8, y + 5, AioaScreenUtil.TEXT_MAIN);
        }
    }

    private String[] contextActions() {
        if (this.contextNode != null) return new String[]{"[N] Edit name", "[P] Edit parameters", "[D] Duplicate",
                "[L] Start link", "[G] Group / enter", "[X] Delete", "[F] Fit view"};
        return new String[]{"[U] Undo  Ctrl+Z", "[R] Redo  Ctrl+Y", "[D] Duplicate", "[X] Delete",
                "[N] New graph", "[F] Fit view"};
    }

    private boolean contextMenuClicked(double mouseX, double mouseY, int button) {
        String[] actions = contextActions();
        int width = this.contextNode == null ? 154 : 166;
        if (button != 0 || mouseX < this.contextMenuX || mouseX > this.contextMenuX + width
                || mouseY < this.contextMenuY || mouseY > this.contextMenuY + 8 + actions.length * 20) return false;
        int action = Math.max(0, Math.min(actions.length - 1, ((int) mouseY - this.contextMenuY - 4) / 20));
        if (this.contextNode != null) {
            this.selected = this.contextNode;
            switch (action) {
                case 0 -> openSyntheticParameter("_label", this.selected.parameters.getOrDefault("_label", friendly(this.selected.type)));
                case 1 -> openParameterPopover(this.contextMenuX, this.contextMenuY);
                case 2 -> duplicateSelected();
                case 3 -> {
                    this.linkStart = this.selected;
                    this.linkOutput = defaultOutput(this.selected.type);
                    this.status = "Drag or click a destination input for " + this.linkOutput + ".";
                }
                case 4 -> {
                    if (this.selected.type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP) {
                        this.activeGroupId = this.selected.parameters.get("_groupId"); rebuildEditorWidgets();
                    } else addSelectedToGroup(false);
                }
                case 5 -> deleteSelected();
                case 6 -> fitGraph();
            }
        } else {
            switch (action) {
                case 0 -> undo();
                case 1 -> redo();
                case 2 -> duplicateSelected();
                case 3 -> deleteSelected();
                case 4 -> newGraph();
                case 5 -> fitGraph();
            }
        }
        this.showContextMenu = false;
        this.contextNode = null;
        return true;
    }

    private void openParameterPopover(int x, int y) {
        if (this.selected == null || this.selected.parameters.isEmpty()) return;
        this.showContextMenu = false;
        this.contextNode = null;
        this.activeTopMenu = null;
        this.parameterSnapshotTaken = false;
        this.parametersX = Math.max(0, Math.min(this.width - this.parametersWidth, x + 10));
        this.parametersY = Math.max(0, Math.min(this.height - this.parametersHeight, y + 8));
        this.parametersCollapsed = false;
        rebuildEditorWidgets();
        loadSelectedParameter(false);
    }

    private void openSyntheticParameter(String key, String value) {
        if (this.selected == null) return;
        this.selected.parameters.putIfAbsent(key, value);
        this.parameterIndex = new ArrayList<>(this.selected.parameters.keySet()).indexOf(key);
        openParameterPopover(this.contextMenuX, this.contextMenuY);
    }

    private void addSelectedToGroup(boolean forceNew) {
        if (this.selected == null) return;
        if (this.selected.type == AioaBehaviorGraph.NodeType.MOB_BASE) {
            this.status = "Base Mob must stay in the parent graph and cannot be placed inside a function group.";
            return;
        }
        snapshot();
        String group = this.selected.parameters.get("_group");
        if (group == null || group.isBlank()) {
            String groupId = forceNew ? null : this.graph.nodes.stream()
                    .filter(node -> node.type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP)
                    .map(node -> node.parameters.get("_groupId")).filter(java.util.Objects::nonNull).findFirst().orElse(null);
            if (groupId != null) {
                this.selected.parameters.put("_group", groupId);
                pruneGroupBoundaryEdges();
                this.status = "Added node to " + groupName(groupId) + ".";
                return;
            }
            groupId = "group_" + UUID.randomUUID().toString().substring(0, 8);
            group = this.graph.nodes.stream().filter(node -> node != this.selected).map(node -> node.parameters.get("_group"))
                    .filter(java.util.Objects::nonNull).filter(name -> !name.isBlank()).findFirst().orElse(null);
            String displayName = "Function " + (1 + this.graph.nodes.stream()
                    .filter(node -> node.type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP).count());
            AioaBehaviorGraph.Node call = new AioaBehaviorGraph.Node("node_" + UUID.randomUUID().toString().substring(0, 8),
                    AioaBehaviorGraph.NodeType.FUNCTION_GROUP, this.selected.x - 180, this.selected.y)
                    .parameter("name", displayName).parameter("_groupId", groupId);
            this.graph.nodes.add(call);
            this.selected.parameters.put("_group", groupId);
            pruneGroupBoundaryEdges();
            this.selected = call;
            this.status = "Created " + displayName + ". Double-click its call node to edit the internal graph.";
        } else {
            this.parameterIndex = new ArrayList<>(this.selected.parameters.keySet()).indexOf("_group");
            openParameterPopover(this.contextMenuX, this.contextMenuY);
            this.status = "This node is inside " + groupName(group) + ".";
        }
    }

    private void pruneGroupBoundaryEdges() {
        this.graph.edges.removeIf(edge -> {
            AioaBehaviorGraph.Node from = findNode(edge.from);
            AioaBehaviorGraph.Node to = findNode(edge.to);
            if (from == null || to == null) return true;
            return !java.util.Objects.equals(from.parameters.get("_group"), to.parameters.get("_group"));
        });
    }

    private void drawTopMenu(GuiGraphics graphics) {
        String[] items = topMenuItems(this.activeTopMenu);
        int x = topMenuX(this.activeTopMenu);
        int y = this.windowY + 54;
        int width = 178;
        graphics.fill(x, y, x + width, y + 6 + items.length * 20, 0xFC101713);
        for (int i = 0; i < items.length; i++) {
            int rowY = y + 3 + i * 20;
            graphics.fill(x + 2, rowY, x + width - 2, rowY + 18, 0xFF1B2921);
            graphics.drawString(this.font, items[i], x + 8, rowY + 5, AioaScreenUtil.TEXT_MAIN);
        }
    }

    private boolean topMenuClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        String[] items = topMenuItems(this.activeTopMenu);
        int x = topMenuX(this.activeTopMenu);
        int y = this.windowY + 54;
        if (mouseX < x || mouseX > x + 178 || mouseY < y || mouseY > y + 6 + items.length * 20) return false;
        int action = Math.max(0, Math.min(items.length - 1, ((int) mouseY - y - 3) / 20));
        runTopMenuAction(this.activeTopMenu, action);
        this.activeTopMenu = null;
        return true;
    }

    private String[] topMenuItems(String menu) {
        if ("File".equals(menu)) return new String[]{"New graph  Ctrl+N", "Save project...  Ctrl+S", "Save project as...", "Import project...", "Autosave now", "Done"};
        if ("Edit".equals(menu)) return new String[]{"Undo  Ctrl+Z", "Redo  Ctrl+Y", "Duplicate  Ctrl+D", "Delete  Del"};
        if ("View".equals(menu)) return new String[]{"Fit graph  F", "Toggle node palette", "Toggle inspector", "Toggle quick guide"};
        if ("Graph".equals(menu)) return new String[]{"Validate graph", "Auto Fix graph", "Start link from selection", "Create function group", this.activeGroupId == null ? "Stop preview" : "Exit function group"};
        return new String[]{"Quick guide", "Documentation", "Keyboard shortcuts"};
    }

    private int topMenuX(String menu) {
        int x = this.windowX + 8;
        for (String name : List.of("File", "Edit", "View", "Graph", "Help")) {
            if (name.equals(menu)) return x;
            x += (name.equals("Graph") ? 62 : 54) + 4;
        }
        return x;
    }

    private void runTopMenuAction(String menu, int action) {
        if ("File".equals(menu)) switch (action) {
            case 0 -> newGraph(); case 1 -> exportGraph(false); case 2 -> exportGraphAs(); case 3 -> loadNewestGraph();
            case 4 -> saveNow(); case 5 -> applyAndClose();
        } else if ("Edit".equals(menu)) switch (action) {
            case 0 -> undo(); case 1 -> redo(); case 2 -> duplicateSelected(); case 3 -> deleteSelected();
        } else if ("View".equals(menu)) switch (action) {
            case 0 -> fitGraph(); case 1 -> { this.paletteCollapsed = !this.paletteCollapsed; rebuildEditorWidgets(); }
            case 2 -> { this.inspectorCollapsed = !this.inspectorCollapsed; rebuildEditorWidgets(); }
            case 3 -> this.showHelp = !this.showHelp;
        } else if ("Graph".equals(menu)) switch (action) {
            case 0 -> validateGraph();
            case 1 -> autoFixGraph();
            case 2 -> { this.linkStart = this.selected; if (this.selected != null) this.linkOutput = defaultOutput(this.selected.type); }
            case 3 -> addSelectedToGroup(true); case 4 -> { if (this.activeGroupId == null) stopPreview(); else { this.activeGroupId = null; this.selected = null; rebuildEditorWidgets(); } }
        } else switch (action) {
            case 0 -> this.showHelp = true; case 1 -> this.transitionTo(new AioaDocsScreen(this));
            case 2 -> this.status = "Ctrl+Z/Y undo/redo, Ctrl+D duplicate, Del delete, F fit, wheel zoom, Shift+wheel pan.";
        }
    }

    private void saveNow() {
        this.graphDirty = true;
        this.lastMutationAt = 0L;
        this.lastAutosaveAt = 0L;
        autosaveIfReady();
    }

    private void drawNodeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!insideCanvas(mouseX, mouseY) || this.showContextMenu || this.activeTopMenu != null) return;
        AioaBehaviorGraph.Node node = nodeAt(mouseX, mouseY);
        if (node == null) return;
        int width = Math.min(260, Math.max(150, this.font.width(node.type.help) + 16));
        int x = Math.min(this.width - width - 4, mouseX + 12);
        int y = Math.min(this.height - 34, mouseY + 12);
        graphics.fill(x, y, x + width, y + 30, 0xF5111915);
        graphics.drawString(this.font, node.type.category + " / double-click to edit", x + 7, y + 5, 0xFF78E5A5);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(node.type.help, width - 14), x + 7, y + 17, AioaScreenUtil.TEXT_SUB);
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

    private boolean isNodeVisible(AioaBehaviorGraph.Node node) {
        String membership = node.parameters.get("_group");
        return this.activeGroupId == null ? membership == null || membership.isBlank()
                : this.activeGroupId.equals(membership);
    }

    private String groupName(String groupId) {
        return this.graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP
                        && groupId.equals(node.parameters.get("_groupId")))
                .map(node -> node.parameters.getOrDefault("name", "Function group")).findFirst().orElse("Function group");
    }

    private String activeGroupName() { return groupName(this.activeGroupId); }

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
            case EQUIP_ARMOR -> node.parameter("item", "minecraft:iron_chestplate").parameter("slot", "CHEST").parameter("dropChance", "0");
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
            case FUNCTION_GROUP -> node.parameter("name", "Function group")
                    .parameter("_groupId", "group_" + UUID.randomUUID().toString().substring(0, 8));
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
