package com.flubburr.aioa.client.config;

import com.flubburr.aioa.behavior.AioaBehaviorGraph;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

final class AioaDocsScreen extends AioaAnimatedScreen {
    private final Screen parent;
    private int tab;
    private int nodePage;

    AioaDocsScreen(Screen parent) {
        super(Component.translatable("aioa.docs.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int panelWidth = Math.min(820, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        String[] tabs = {
                Component.translatable("aioa.docs.tab.start").getString(),
                Component.translatable("aioa.docs.tab.editor").getString(),
                Component.translatable("aioa.docs.tab.nodes").getString(),
                Component.translatable("aioa.docs.tab.spawning").getString(),
                Component.translatable("aioa.docs.tab.multiplayer").getString()
        };
        int tabWidth = Math.max(72, (panelWidth - 32) / tabs.length);
        for (int i = 0; i < tabs.length; i++) {
            int selectedTab = i;
            this.addRenderableWidget(AioaScreenUtil.button(left + 16 + i * tabWidth, 54, tabWidth - 4, tabs[i], b -> {
                this.tab = selectedTab;
                this.rebuildDocsWidgets();
            }));
        }
        if (this.tab == 2) {
            this.addRenderableWidget(AioaScreenUtil.button(left + 20, this.height - 54, 90, Component.translatable("aioa.docs.previous").getString(), b -> this.nodePage = Math.max(0, this.nodePage - 1)));
            this.addRenderableWidget(AioaScreenUtil.button(left + 116, this.height - 54, 90, Component.translatable("aioa.docs.next").getString(), b -> {
                if ((this.nodePage + 1) * 8 < AioaBehaviorGraph.NodeType.values().length) this.nodePage++;
            }));
        }
        this.addRenderableWidget(AioaScreenUtil.button(left + panelWidth - 104, this.height - 54, 84, Component.translatable("gui.done").getString(), b -> this.onClose()));
    }

    private void rebuildDocsWidgets() { this.init(); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(graphics);
        AioaScreenUtil.drawScreenBackground(graphics, this.width, this.height);
        int panelWidth = Math.min(820, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        AioaScreenUtil.drawPanel(graphics, left, 18, left + panelWidth, this.height - 20);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, AioaScreenUtil.TEXT_MAIN);
        graphics.drawCenteredString(this.font, "Creator-friendly guide - no code required", this.width / 2, 42, AioaScreenUtil.TEXT_SUB);
        int top = 88;
        switch (this.tab) {
            case 1 -> drawEditorDocs(graphics, left, top, panelWidth);
            case 2 -> drawNodeDocs(graphics, left, top, panelWidth);
            case 3 -> drawSpawnDocs(graphics, left, top, panelWidth);
            case 4 -> drawMultiplayerDocs(graphics, left, top, panelWidth);
            default -> drawStartDocs(graphics, left, top, panelWidth);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        this.finishUiRender(graphics);
    }

    private void drawStartDocs(GuiGraphics g, int left, int top, int width) {
        card(g, left + 20, top, width - 40, "WELCOME TO MOB STUDIO", "Build mob behavior by placing readable nodes, linking their output ports, previewing the selected mob, validating, and applying. Start with one mob type before using global scopes.");
        AioaScreenUtil.drawMobPreview(g, this.font, left + 36, top + 76, 210, 150, new ResourceLocation("minecraft", "zombie"), true,
                List.of(Component.literal("LIVE MOB SHOWCASE"), Component.literal("Select, preview, spawn, direct")));
        card(g, left + 270, top + 76, width - 306, "FIRST GRAPH", "1. Open Behavior Graph Studio or press F7.  2. Choose a scope or use world picking.  3. Add event, sensing, action, and condition nodes.  4. Link ports.  5. Validate and press Done. Use Spawn Studio to create a test subject.");
    }

    private void drawEditorDocs(GuiGraphics g, int left, int top, int width) {
        card(g, left + 20, top, width - 40, "CANVAS CONTROLS", "Drag nodes with left click. Alt + left drag or middle drag moves the workspace. Ctrl + mouse wheel zooms toward the cursor. Shift + wheel pans sideways. F or Ctrl+0 fits the complete graph.");
        card(g, left + 20, top + 78, (width - 50) / 2, "KEYBOARD", "Ctrl+Z undo, Ctrl+Y redo, Ctrl+D duplicate, Ctrl+N new graph, Ctrl+S apply, Delete removes a node, H toggles help. The editor keeps a compact Scale 2 workspace independent of Minecraft's menu layout.");
        card(g, left + 30 + (width - 50) / 2, top + 78, (width - 50) / 2, "LINKING", "Right-click a node for quick-link, select the output port, then left-click its destination. Condition nodes expose true/false; sensors expose found/missing; timers expose ready/waiting.");
    }

    private void drawNodeDocs(GuiGraphics g, int left, int top, int width) {
        AioaBehaviorGraph.NodeType[] nodes = AioaBehaviorGraph.NodeType.values();
        int start = this.nodePage * 8;
        for (int i = start; i < Math.min(nodes.length, start + 8); i++) {
            AioaBehaviorGraph.NodeType node = nodes[i];
            int row = i - start;
            int column = row % 2;
            int y = top + (row / 2) * 66;
            card(g, left + 20 + column * ((width - 50) / 2 + 10), y, (width - 50) / 2,
                    node.name().replace('_', ' '), node.category + " - " + node.help);
        }
        g.drawString(this.font, "Node reference page " + (this.nodePage + 1) + " / " + ((nodes.length + 7) / 8), left + 220, this.height - 47, AioaScreenUtil.TEXT_SUB);
    }

    private void drawSpawnDocs(GuiGraphics g, int left, int top, int width) {
        card(g, left + 20, top, width - 40, "SPAWN STUDIO", "Create individual mobs or instanced groups with exact facing, position, AI, persistence, silence, invulnerability, glow, baby state, equipment, and names. Preview first, then spawn at the player or cursor target.");
        AioaScreenUtil.drawMobPreview(g, this.font, left + 30, top + 78, 190, 145, new ResourceLocation("minecraft", "skeleton"), true,
                List.of(Component.literal("INSTANCE PRESET"), Component.literal("No AI + face player")));
        AioaScreenUtil.drawMobPreview(g, this.font, left + 236, top + 78, 190, 145, new ResourceLocation("minecraft", "creeper"), true,
                List.of(Component.literal("CONTENT SHOT"), Component.literal("Persistent + custom name")));
        card(g, left + 442, top + 78, width - 472, "SAFE TESTING", "Creative/operator permission is required for server-side creation. Limits clamp group size, distance, graph size, and packet size. Test dangerous graphs on a copy of the world.");
    }

    private void drawMultiplayerDocs(GuiGraphics g, int left, int top, int width) {
        card(g, left + 20, top, width - 40, "SERVER AUTHORITY", "Behavior and spawn requests are validated by the server. Players without permission cannot push graphs or create mob instances. Mob behavior runs server-side so all clients observe the same result.");
        card(g, left + 20, top + 82, width - 40, "COMPATIBILITY", "AIOA uses standard mob navigation, registry IDs, goals, and bounded network payloads. Scope graphs narrowly when combining it with other AI mods. If another mod owns the same mob goal, use AIOA graph actions instead of enabling both global controllers.");
        card(g, left + 20, top + 164, width - 40, "LANGUAGES & ACCESSIBILITY", "Labels use Minecraft's language system and fall back to English when a translation is unavailable. The fixed compact editor, high contrast ports, text status feedback, and instant open/close behavior are designed for recording and live content creation.");
    }

    private void card(GuiGraphics g, int x, int y, int width, String title, String body) {
        AioaScreenUtil.drawInsetPanel(g, x, y, x + width, y + 60, false);
        g.drawString(this.font, title, x + 10, y + 9, 0xFF78E5A5);
        AioaScreenUtil.drawWrappedCenteredText(g, this.font, Component.literal(body), x + width / 2, y + 24, width - 18, AioaScreenUtil.TEXT_SUB);
    }

    @Override
    public void onClose() { this.transitionTo(this.parent); }
}
