package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AioaBehaviorEngineSettingsScreen extends AioaScrollableScreen {
    private final Screen parent;
    private final AioaConfig editableConfig;
    private AioaConfig.BehaviorEngine draft;

    AioaBehaviorEngineSettingsScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Behavior Engine Limits & Safety"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        this.resetScrollLayout(620, 82, Math.max(150, this.height - 64));
        AioaConfig.BehaviorEngine source = this.editableConfig.behaviorEngine;
        if (this.draft == null) {
            this.draft = new AioaConfig.BehaviorEngine();
            this.draft.enabled = source.enabled;
            this.draft.tickInterval = source.tickInterval;
            this.draft.maxGraphsPerMob = source.maxGraphsPerMob;
            this.draft.maxStepsPerGraph = source.maxStepsPerGraph;
            this.draft.allowWorldNodes = source.allowWorldNodes;
            this.draft.maxNodeSpawnedMobsNearby = source.maxNodeSpawnedMobsNearby;
        }
        int x = this.panelLeft + 20;
        int width = this.panelWidth - 40;
        int y = 0;
        int step = AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.addScrollable(AioaScreenUtil.button(x, 0, width, AioaScreenUtil.boolLabel("Behavior engine", this.draft.enabled), b -> {
            this.draft.enabled = !this.draft.enabled;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Behavior engine", this.draft.enabled)));
        }), y); y += step;
        this.addScrollable(AioaScreenUtil.intSlider(x, 0, width, "Run every ticks", 1, 20, 1, this.draft.tickInterval, value -> this.draft.tickInterval = value), y); y += step;
        this.addScrollable(AioaScreenUtil.intSlider(x, 0, width, "Max graphs per mob", 1, 32, 1, this.draft.maxGraphsPerMob, value -> this.draft.maxGraphsPerMob = value), y); y += step;
        this.addScrollable(AioaScreenUtil.intSlider(x, 0, width, "Max node steps", 8, 256, 8, this.draft.maxStepsPerGraph, value -> this.draft.maxStepsPerGraph = value), y); y += step;
        this.addScrollable(AioaScreenUtil.button(x, 0, width, AioaScreenUtil.boolLabel("Allow spawn/world nodes", this.draft.allowWorldNodes), b -> {
            this.draft.allowWorldNodes = !this.draft.allowWorldNodes;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Allow spawn/world nodes", this.draft.allowWorldNodes)));
        }), y); y += step;
        this.addScrollable(AioaScreenUtil.intSlider(x, 0, width, "Spawn node nearby cap", 1, 64, 1,
                this.draft.maxNodeSpawnedMobsNearby, value -> this.draft.maxNodeSpawnedMobsNearby = value), y); y += step + 8;
        this.addScrollable(AioaScreenUtil.button(this.width / 2 - 172, 0, 164, "Done", b -> {
            this.editableConfig.behaviorEngine = this.draft;
            this.transitionTo(this.parent);
        }), y);
        this.addScrollable(AioaScreenUtil.button(this.width / 2 + 8, 0, 164, "Cancel", b -> this.transitionTo(this.parent)), y);
        this.finishScrollLayout(y + AioaScreenUtil.BUTTON_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(graphics);
        AioaScreenUtil.drawScreenBackground(graphics, this.width, this.height);
        AioaScreenUtil.drawPanel(graphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(graphics, this.font,
                Component.literal("Tune graph cadence and hard safety caps for large scenes, servers, and content-creation worlds."),
                this.width / 2, 50, this.panelWidth - 64, AioaScreenUtil.TEXT_SUB);
        AioaScreenUtil.drawClippedContent(graphics, this.panelLeft + 8, 82, this.panelLeft + this.panelWidth - 20, Math.max(150, this.height - 64),
                () -> AioaBehaviorEngineSettingsScreen.super.render(graphics, mouseX, mouseY, partialTick));
        this.finishUiRender(graphics);
    }

    @Override
    public void onClose() {
        this.transitionTo(this.parent);
    }
}
