package com.flubburr.aioa.client.config;

import com.flubburr.aioa.behavior.AioaBehaviorGraph;
import com.flubburr.aioa.compat.AioaEntityHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

final class AioaDocsScreen extends AioaAnimatedScreen {
    private enum Chapter {
        START("Getting started", BehaviorBoard.Part.ROOT),
        EDITOR("Graph controls", BehaviorBoard.Part.SIGNAL),
        NODES("Node reference", BehaviorBoard.Part.GATE),
        SPAWNING("Spawn studio", BehaviorBoard.Part.EGG),
        MULTIPLAYER("Multiplayer", BehaviorBoard.Part.TARGET),
        SCRIPTS("Scripts", BehaviorBoard.Part.BOOK);

        final String title;
        final BehaviorBoard.Part icon;

        Chapter(String title, BehaviorBoard.Part icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    private final Screen parent;
    private Chapter chapter = Chapter.START;
    private int nodePage;

    AioaDocsScreen(Screen parent) {
        super(Component.translatable("aioa.docs.title"));
        this.parent = parent;
    }

    private int panelWidth() { return Math.min(840, this.width - 20); }
    private int left() { return (this.width - panelWidth()) / 2; }
    private boolean sidebar() { return panelWidth() >= 600 && this.height >= 300; }
    private int bodyX() { return left() + (sidebar() ? 172 : 16); }
    private int bodyWidth() { return left() + panelWidth() - 16 - bodyX(); }
    private int bodyY() { return sidebar() ? 62 : 91; }
    private int rowsPerPage() { return Math.max(1, (this.height - bodyY() - 60) / 46); }

    @Override
    protected void init() {
        this.clearWidgets();
        if (sidebar()) {
            int y = bodyY();
            for (Chapter entry : Chapter.values()) {
                var button = this.addRenderableWidget(AioaScreenUtil.toolButton(left() + 8, y, 143, entry.title, entry.icon, "", b -> select(entry)));
                button.active = entry != this.chapter;
                y += 29;
            }
        } else {
            var chapters = Chapter.values();
            this.addRenderableWidget(AioaScreenUtil.menuButton(left() + 8, 48, 34, "<", b -> select(chapters[Math.floorMod(this.chapter.ordinal() - 1, chapters.length)])));
            this.addRenderableWidget(AioaScreenUtil.menuButton(left() + panelWidth() - 42, 48, 34, ">", b -> select(chapters[(this.chapter.ordinal() + 1) % chapters.length])));
        }
        if (this.chapter == Chapter.NODES) {
            int pages = nodePages();
            this.nodePage = Math.min(nodePage, pages - 1);
            this.addRenderableWidget(AioaScreenUtil.menuButton(bodyX(), this.height - 42, 44, "Prev", b -> {
                this.nodePage--; this.init();
            })).active = nodePage > 0;
            this.addRenderableWidget(AioaScreenUtil.menuButton(bodyX() + 48, this.height - 42, 44, "Next", b -> {
                this.nodePage++; this.init();
            })).active = nodePage + 1 < pages;
        }
        this.addRenderableWidget(AioaScreenUtil.button(left() + panelWidth() - 76, this.height - 42, 64, "Done", b -> this.onClose()));
    }

    private int nodePages() { return (AioaBehaviorGraph.NodeType.values().length + rowsPerPage() - 1) / rowsPerPage(); }
    private void select(Chapter entry) { this.chapter = entry; this.init(); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(g);
        AioaScreenUtil.drawScreenBackground(g, this.width, this.height);
        int x = left(), right = x + panelWidth(), bottom = this.height - 10;
        g.fill(x, 12, right, bottom, 0xFF18231D);
        g.fill(x, 12, right, 44, 0xFF24392B);
        BehaviorBoard.Part.BOOK.draw(g, x + 12, 20);
        g.drawString(this.font, "Field guide", x + 36, 24, BehaviorBoard.BONE, false);
        if (sidebar()) {
            g.fill(x, 44, x + 156, bottom, 0xFF1E2B23);
            g.fill(x + 156, 44, x + 157, bottom, 0xFF384A3E);
            g.drawString(this.font, chapter.title, bodyX(), bodyY() - 1, BehaviorBoard.PATINA, false);
        } else {
            g.drawCenteredString(this.font, chapter.title, this.width / 2, 57, BehaviorBoard.PATINA);
        }
        int y = bodyY() + (sidebar() ? 19 : 0);
        g.enableScissor(bodyX(), bodyY(), right - 10, this.height - 46);
        if (chapter == Chapter.NODES) {
            var types = AioaBehaviorGraph.NodeType.values();
            int first = nodePage * rowsPerPage();
            for (int i = first; i < Math.min(types.length, first + rowsPerPage()); i++) {
                var type = types[i];
                int row = y + (i - first) * 46;
                BehaviorBoard.Part.of(type).draw(g, bodyX(), row);
                g.drawString(this.font, type.name().replace('_', ' '), bodyX() + 24, row + 3, BehaviorBoard.BONE, false);
                paragraph(g, type.help, bodyX() + 24, row + 16, bodyWidth() - 24, 2);
            }
        } else {
            String[] entries = switch (chapter) {
                case START -> new String[]{"Build a first behavior", "Open the behavior editor with F7 in Creative. Find the starter graph's player search, then follow its two exits. Select a pin and drag to an input to change the route."};
                case EDITOR -> new String[]{"Move around", "Drag empty space or middle-drag to pan. Scroll to zoom at the cursor. Press F to frame the graph.", "Edit a node", "Press Ctrl+K to search. Double-click a node for parameters. Shift-drag on the canvas to select several nodes. Drag an occupied input to reroute its wire."};
                case SPAWNING -> new String[]{"Place a test mob", "In Spawn studio, choose an entity, position and facing. Set its count and equipment, then spawn it near the player or at the cursor."};
                case MULTIPLAYER -> new String[]{"Who can edit", "The server controls mob behavior and spawning. Open the editor in Creative. The server checks submitted graphs and spawn requests before applying them."};
                case SCRIPTS -> new String[]{"Run Lua", "Add a Script node, open its parameters and choose Open script editor. Use Check Lua to find syntax errors. The editor keeps a local draft, including code with errors.", "Read and act", "Lua can read health, max_health, target_exists, target_distance and tick. Call actions such as glow(true) or move_to_target(1.1). Connect the node to the graph to run it.", "Keep per-mob state", "Use set(\"counter\", get(\"counter\") + 1) to share a number with other nodes on the same mob. Existing scripts open in Legacy mode."};
                default -> throw new IllegalStateException();
            };
            for (int i = 0; i < entries.length; i += 2) {
                g.drawString(this.font, entries[i], bodyX(), y, BehaviorBoard.BONE, false);
                y = paragraph(g, entries[i + 1], bodyX(), y + 17, bodyWidth(), 20) + 16;
            }
            if ((chapter == Chapter.START || chapter == Chapter.SPAWNING) && this.height - 60 - y >= 70) {
                var mob = AioaEntityHelper.parseResourceLocation(chapter == Chapter.START ? "minecraft:zombie" : "minecraft:skeleton");
                AioaScreenUtil.drawMobPreview(g, this.font, bodyX(), y, bodyWidth(), Math.min(146, this.height - 60 - y),
                        mob, false, List.of());
            }
        }
        g.disableScissor();
        if (chapter == Chapter.NODES) g.drawString(this.font, (nodePage + 1) + " / " + nodePages(), bodyX() + 100, this.height - 34, AioaScreenUtil.TEXT_SUB, false);
        super.render(g, mouseX, mouseY, partialTick);
        this.finishUiRender(g);
    }

    private int paragraph(GuiGraphics g, String content, int x, int y, int width, int limit) {
        int count = 0;
        for (var line : this.font.split(Component.literal(content), width)) {
            if (++count > limit) break;
            g.drawString(this.font, line, x, y, AioaScreenUtil.TEXT_SUB, false);
            y += 11;
        }
        return y;
    }

    @Override
    public void onClose() { this.transitionTo(this.parent); }
}
