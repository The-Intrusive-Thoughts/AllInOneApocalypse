package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AioaHostileSettingsScreen extends Screen {

    private final Screen parent;
    private final AioaConfig editableConfig;

    private boolean enabled;
    private boolean overworldOnly;
    private boolean ignoreStructureSpawns;
    private boolean ignoreSpawnerSpawns;
    private boolean ignoreSpecialSpawns;

    AioaHostileSettingsScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Hostile Spawn Rules"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        this.enabled = this.editableConfig.hostileSpawnControl.enabled;
        this.overworldOnly = this.editableConfig.hostileSpawnControl.overworldOnly;
        this.ignoreStructureSpawns = this.editableConfig.hostileSpawnControl.ignoreStructureSpawns;
        this.ignoreSpawnerSpawns = this.editableConfig.hostileSpawnControl.ignoreSpawnerSpawns;
        this.ignoreSpecialSpawns = this.editableConfig.hostileSpawnControl.ignoreSpecialSpawns;

        int centerX = this.width / 2;
        int width = 320;
        int y = 54;

        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Hostile nullification", this.enabled), b -> {
            this.enabled = !this.enabled;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Hostile nullification", this.enabled)));
        }));
        y += 24;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Overworld only", this.overworldOnly), b -> {
            this.overworldOnly = !this.overworldOnly;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Overworld only", this.overworldOnly)));
        }));
        y += 24;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Leave structure spawns alone", this.ignoreStructureSpawns), b -> {
            this.ignoreStructureSpawns = !this.ignoreStructureSpawns;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Leave structure spawns alone", this.ignoreStructureSpawns)));
        }));
        y += 24;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Leave spawners alone", this.ignoreSpawnerSpawns), b -> {
            this.ignoreSpawnerSpawns = !this.ignoreSpawnerSpawns;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Leave spawners alone", this.ignoreSpawnerSpawns)));
        }));
        y += 24;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Leave scripted/special spawns alone", this.ignoreSpecialSpawns), b -> {
            this.ignoreSpecialSpawns = !this.ignoreSpecialSpawns;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Leave scripted/special spawns alone", this.ignoreSpecialSpawns)));
        }));

        int bottomY = this.height - 30;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 154, bottomY, 150, "Done", b -> {
            this.editableConfig.hostileSpawnControl.enabled = this.enabled;
            this.editableConfig.hostileSpawnControl.overworldOnly = this.overworldOnly;
            this.editableConfig.hostileSpawnControl.ignoreStructureSpawns = this.ignoreStructureSpawns;
            this.editableConfig.hostileSpawnControl.ignoreSpawnerSpawns = this.ignoreSpawnerSpawns;
            this.editableConfig.hostileSpawnControl.ignoreSpecialSpawns = this.ignoreSpecialSpawns;
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 4, bottomY, 150, "Cancel", b -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 190, 24, this.width / 2 + 190, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        guiGraphics.drawCenteredString(this.font, Component.literal("Control which hostile natural-style spawns AIOA suppresses."), this.width / 2, 49, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
