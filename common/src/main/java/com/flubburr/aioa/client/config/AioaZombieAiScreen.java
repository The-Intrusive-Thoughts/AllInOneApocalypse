package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class AioaZombieAiScreen extends Screen {

    private final Screen parent;
    private final AioaConfig editableConfig;
    private AioaConfig.ZombieTargetMode zombieTargetMode;
    private boolean zombiesCanClimbWalls;
    private boolean refinedZombieAi;
    private final List<AbstractWidget> scrollWidgets = new ArrayList<>();
    private final List<Integer> baseY = new ArrayList<>();
    private int scrollOffset;
    private int maxScroll;
    private int contentTop;
    private int contentBottom;

    AioaZombieAiScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Zombie AI and Targeting"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.scrollWidgets.clear();
        this.baseY.clear();
        AioaConfig.DaySurfaceSpawns config = this.editableConfig.daySurfaceSpawns;
        this.zombieTargetMode = config.zombieTargetMode;
        this.zombiesCanClimbWalls = config.zombiesCanClimbWalls;
        this.refinedZombieAi = config.refinedZombieAi;

        int centerX = this.width / 2;
        int panelWidth = AioaScreenUtil.panelWidth(this.width, 700);
        int width = panelWidth - 44;
        int leftX = centerX - (width / 2);
        int y = 0;
        int step = AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.contentTop = 76;
        this.contentBottom = this.height - 72;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, AioaScreenUtil.cycleLabel("Zombie target mode", this.zombieTargetMode), b -> {
            this.zombieTargetMode = AioaScreenUtil.next(this.zombieTargetMode, AioaConfig.ZombieTargetMode.values());
            b.setMessage(Component.literal(AioaScreenUtil.cycleLabel("Zombie target mode", this.zombieTargetMode)));
        }), y);
        y += step;
        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, AioaScreenUtil.boolLabel("Wall climbing", this.zombiesCanClimbWalls), b -> {
            this.zombiesCanClimbWalls = !this.zombiesCanClimbWalls;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Wall climbing", this.zombiesCanClimbWalls)));
        }), y);
        y += step;
        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, AioaScreenUtil.boolLabel("Refined chase AI", this.refinedZombieAi), b -> {
            this.refinedZombieAi = !this.refinedZombieAi;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Refined chase AI", this.refinedZombieAi)));
        }), y);
        y += step + 8;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, "Choose Refined AI Mobs", b ->
                this.minecraft.setScreen(AioaEntityToggleScreen.forAllEntities(
                        this,
                        "Refined AI Mobs",
                        "Pick every mob that should use the apocalypse chase/pathfinding behavior.",
                        this.editableConfig.daySurfaceSpawns.refinedAiEntityIds,
                        values -> this.editableConfig.daySurfaceSpawns.refinedAiEntityIds = values
                ))), y);
        y += step;
        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, "Choose Wall Climbers", b ->
                this.minecraft.setScreen(AioaEntityToggleScreen.forAllEntities(
                        this,
                        "Wall Climbing Mobs",
                        "Choose mobs that may climb walls while chasing targets.",
                        this.editableConfig.daySurfaceSpawns.wallClimbingEntityIds,
                        values -> this.editableConfig.daySurfaceSpawns.wallClimbingEntityIds = values
                ))), y);
        y += step;
        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, "Players-Only Overrides", b ->
                this.minecraft.setScreen(AioaEntityToggleScreen.forAllEntities(
                        this,
                        "Players Only Overrides",
                        "These mobs ignore the default target mode and only hunt players.",
                        this.editableConfig.daySurfaceSpawns.playerOnlyTargetEntityIds,
                        values -> this.editableConfig.daySurfaceSpawns.playerOnlyTargetEntityIds = values
                ))), y);
        y += step;
        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, "Animals-Only Overrides", b ->
                this.minecraft.setScreen(AioaEntityToggleScreen.forAllEntities(
                        this,
                        "Animals Only Overrides",
                        "These mobs ignore the default target mode and only target animals.",
                        this.editableConfig.daySurfaceSpawns.animalTargetEntityIds,
                        values -> this.editableConfig.daySurfaceSpawns.animalTargetEntityIds = values
                ))), y);
        y += step;
        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, "Other-Mobs Overrides", b ->
                this.minecraft.setScreen(AioaEntityToggleScreen.forAllEntities(
                        this,
                        "Other Mobs Overrides",
                        "These mobs ignore the default target mode and target non-player mobs.",
                        this.editableConfig.daySurfaceSpawns.otherMobTargetEntityIds,
                        values -> this.editableConfig.daySurfaceSpawns.otherMobTargetEntityIds = values
                ))), y);
        y += step;
        this.addScrollable(AioaScreenUtil.button(leftX, 0, width, "Everything Overrides", b ->
                this.minecraft.setScreen(AioaEntityToggleScreen.forAllEntities(
                        this,
                        "Everything Overrides",
                        "These mobs ignore the default target mode and attack almost anything alive.",
                        this.editableConfig.daySurfaceSpawns.everythingTargetEntityIds,
                        values -> this.editableConfig.daySurfaceSpawns.everythingTargetEntityIds = values
                ))), y);

        y += step + 8;
        this.addScrollable(AioaScreenUtil.button(centerX - 172, 0, 164, "Done", b -> {
            this.editableConfig.daySurfaceSpawns.zombieTargetMode = this.zombieTargetMode;
            this.editableConfig.daySurfaceSpawns.zombiesCanClimbWalls = this.zombiesCanClimbWalls;
            this.editableConfig.daySurfaceSpawns.refinedZombieAi = this.refinedZombieAi;
            this.minecraft.setScreen(this.parent);
        }), y);
        this.addScrollable(AioaScreenUtil.button(centerX + 8, 0, 164, "Cancel", b -> this.minecraft.setScreen(this.parent)), y);
        this.maxScroll = Math.max(0, (y + AioaScreenUtil.BUTTON_HEIGHT) - (this.contentBottom - this.contentTop));
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
        this.updateScrollLayout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AioaScreenUtil.drawBackdrop(guiGraphics, this.width, this.height);
        int panelWidth = AioaScreenUtil.panelWidth(this.width, 700);
        int panelLeft = AioaScreenUtil.panelLeft(this.width, panelWidth);
        AioaScreenUtil.drawPanel(guiGraphics, panelLeft, 24, panelLeft + panelWidth, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal("Choose which mobs use the special zombie behavior and targeting rules."), this.width / 2, 49, panelWidth - 72, AioaScreenUtil.TEXT_SUB);
        guiGraphics.enableScissor(panelLeft + 8, this.contentTop, panelLeft + panelWidth - 20, this.contentBottom);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
        AioaScreenUtil.drawScrollBar(guiGraphics, panelLeft + panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll, this.scrollOffset - ((int) delta * 24)));
        this.updateScrollLayout();
        return true;
    }

    private <T extends AbstractWidget> T addScrollable(T widget, int relativeY) {
        this.scrollWidgets.add(widget);
        this.baseY.add(relativeY);
        this.addRenderableWidget(widget);
        return widget;
    }

    private void updateScrollLayout() {
        for (int i = 0; i < this.scrollWidgets.size(); i++) {
            AbstractWidget widget = this.scrollWidgets.get(i);
            int y = this.contentTop + this.baseY.get(i) - this.scrollOffset;
            widget.setY(y);
            widget.visible = y + widget.getHeight() >= this.contentTop && y <= this.contentBottom;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
