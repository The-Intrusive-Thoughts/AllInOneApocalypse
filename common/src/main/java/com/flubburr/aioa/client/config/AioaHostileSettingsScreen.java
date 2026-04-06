package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class AioaHostileSettingsScreen extends AioaScrollableScreen {

    private final Screen parent;
    private final AioaConfig editableConfig;

    private boolean enabled;
    private boolean overworldOnly;
    private boolean ignoreStructureSpawns;
    private boolean ignoreSpawnerSpawns;
    private boolean ignoreSpecialSpawns;
    private boolean coreExpanded = false;
    private boolean exceptionsExpanded = false;
    private Button coreHeader;
    private Button exceptionsHeader;
    private final List<Button> coreButtons = new ArrayList<>();
    private final List<Button> exceptionButtons = new ArrayList<>();

    AioaHostileSettingsScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Hostile Spawn Rules"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 76, 68, 110);
        int contentBottom = AioaScreenUtil.adaptiveContentBottom(this.height, this.height - 68, 40, contentTop, 110);
        this.resetScrollLayout(620, contentTop, contentBottom);
        this.enabled = this.editableConfig.hostileSpawnControl.enabled;
        this.overworldOnly = this.editableConfig.hostileSpawnControl.overworldOnly;
        this.ignoreStructureSpawns = this.editableConfig.hostileSpawnControl.ignoreStructureSpawns;
        this.ignoreSpawnerSpawns = this.editableConfig.hostileSpawnControl.ignoreSpawnerSpawns;
        this.ignoreSpecialSpawns = this.editableConfig.hostileSpawnControl.ignoreSpecialSpawns;

        int centerX = this.width / 2;
        int width = this.panelWidth - 40;
        int y = 0;
        int step = AioaScreenUtil.BUTTON_HEIGHT + 6;

        this.coreButtons.clear();
        this.exceptionButtons.clear();
        this.coreHeader = this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.sectionLabel("Core rules", this.coreExpanded), b -> {
            this.coreExpanded = !this.coreExpanded;
            this.init();
        }), y);
        y += step;
        this.coreButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Hostile nullification", this.enabled), b -> {
            this.enabled = !this.enabled;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Hostile nullification", this.enabled)));
        }), y));
        if (this.coreExpanded) {
            y += step;
        }
        this.coreButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Overworld only", this.overworldOnly), b -> {
            this.overworldOnly = !this.overworldOnly;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Overworld only", this.overworldOnly)));
        }), y));
        if (this.coreExpanded) {
            y += step;
        }
        y += 6;
        this.exceptionsHeader = this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.sectionLabel("Exceptions", this.exceptionsExpanded), b -> {
            this.exceptionsExpanded = !this.exceptionsExpanded;
            this.init();
        }), y);
        y += step;
        this.exceptionButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Leave structure spawns alone", this.ignoreStructureSpawns), b -> {
            this.ignoreStructureSpawns = !this.ignoreStructureSpawns;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Leave structure spawns alone", this.ignoreStructureSpawns)));
        }), y));
        if (this.exceptionsExpanded) {
            y += step;
        }
        this.exceptionButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Leave spawners alone", this.ignoreSpawnerSpawns), b -> {
            this.ignoreSpawnerSpawns = !this.ignoreSpawnerSpawns;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Leave spawners alone", this.ignoreSpawnerSpawns)));
        }), y));
        if (this.exceptionsExpanded) {
            y += step;
        }
        this.exceptionButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Leave scripted/special spawns alone", this.ignoreSpecialSpawns), b -> {
            this.ignoreSpecialSpawns = !this.ignoreSpecialSpawns;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Leave scripted/special spawns alone", this.ignoreSpecialSpawns)));
        }), y));
        if (this.exceptionsExpanded) {
            y += step;
        }
        this.refreshVisibility();

        this.addScrollable(AioaScreenUtil.button(centerX - 172, 0, 164, "Done", b -> {
            this.editableConfig.hostileSpawnControl.enabled = this.enabled;
            this.editableConfig.hostileSpawnControl.overworldOnly = this.overworldOnly;
            this.editableConfig.hostileSpawnControl.ignoreStructureSpawns = this.ignoreStructureSpawns;
            this.editableConfig.hostileSpawnControl.ignoreSpawnerSpawns = this.ignoreSpawnerSpawns;
            this.editableConfig.hostileSpawnControl.ignoreSpecialSpawns = this.ignoreSpecialSpawns;
            this.minecraft.setScreen(this.parent);
        }), y);
        this.addScrollable(AioaScreenUtil.button(centerX + 8, 0, 164, "Cancel", b -> this.minecraft.setScreen(this.parent)), y);
        this.finishScrollLayout(y + AioaScreenUtil.BUTTON_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        AioaScreenUtil.drawPanel(guiGraphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal("Control which hostile spawns are blocked and which exceptions are still allowed through."), this.width / 2, 49, this.panelWidth - 72, AioaScreenUtil.TEXT_SUB);
        AioaScreenUtil.drawClippedContent(guiGraphics, this.panelLeft + 8, this.contentTop, this.panelLeft + this.panelWidth - 20, this.contentBottom,
                () -> AioaHostileSettingsScreen.super.render(guiGraphics, mouseX, mouseY, partialTick));
        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
    }

    private void refreshVisibility() {
        this.coreHeader.setMessage(Component.literal(AioaScreenUtil.sectionLabel("Core rules", this.coreExpanded)));
        for (Button button : this.coreButtons) {
            this.setScrollableShown(button, this.coreExpanded);
        }
        this.exceptionsHeader.setMessage(Component.literal(AioaScreenUtil.sectionLabel("Exceptions", this.exceptionsExpanded)));
        for (Button button : this.exceptionButtons) {
            this.setScrollableShown(button, this.exceptionsExpanded);
        }
        this.updateScrollLayout();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
