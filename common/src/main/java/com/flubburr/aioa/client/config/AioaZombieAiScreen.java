package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AioaZombieAiScreen extends Screen {

    private final Screen parent;
    private final AioaConfig editableConfig;
    private AioaConfig.ZombieTargetMode zombieTargetMode;
    private boolean zombiesCanClimbWalls;
    private boolean refinedZombieAi;

    AioaZombieAiScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Zombie AI and Targeting"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        AioaConfig.DaySurfaceSpawns config = this.editableConfig.daySurfaceSpawns;
        this.zombieTargetMode = config.zombieTargetMode;
        this.zombiesCanClimbWalls = config.zombiesCanClimbWalls;
        this.refinedZombieAi = config.refinedZombieAi;

        int centerX = this.width / 2;
        int width = 340;
        int y = 60;

        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.cycleLabel("Zombie target mode", this.zombieTargetMode), b -> {
            this.zombieTargetMode = AioaScreenUtil.next(this.zombieTargetMode, AioaConfig.ZombieTargetMode.values());
            b.setMessage(Component.literal(AioaScreenUtil.cycleLabel("Zombie target mode", this.zombieTargetMode)));
        }));
        y += 28;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Wall climbing", this.zombiesCanClimbWalls), b -> {
            this.zombiesCanClimbWalls = !this.zombiesCanClimbWalls;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Wall climbing", this.zombiesCanClimbWalls)));
        }));
        y += 28;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Refined chase AI", this.refinedZombieAi), b -> {
            this.refinedZombieAi = !this.refinedZombieAi;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Refined chase AI", this.refinedZombieAi)));
        }));

        int bottomY = this.height - 30;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 154, bottomY, 150, "Done", b -> {
            this.editableConfig.daySurfaceSpawns.zombieTargetMode = this.zombieTargetMode;
            this.editableConfig.daySurfaceSpawns.zombiesCanClimbWalls = this.zombiesCanClimbWalls;
            this.editableConfig.daySurfaceSpawns.refinedZombieAi = this.refinedZombieAi;
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 4, bottomY, 150, "Cancel", b -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 200, 24, this.width / 2 + 200, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        guiGraphics.drawCenteredString(this.font, Component.literal("Switch zombie targets, enable wall climbing, and sharpen their pursuit behavior."), this.width / 2, 49, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawCenteredString(this.font, Component.literal("Modes: vanilla, players, animals, other mobs, or everything non-zombie."), this.width / 2, 135, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
