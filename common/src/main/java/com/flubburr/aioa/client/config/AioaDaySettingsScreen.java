package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AioaDaySettingsScreen extends Screen {

    private final Screen parent;
    private final AioaConfig editableConfig;

    private boolean enabled;
    private boolean overworldOnly;
    private boolean requireDaytime;
    private boolean requireClearSky;
    private boolean preventSunlightBurn;
    private boolean exportMobCatalog;
    private AioaConfig.ZombieVariantMode zombieVariantMode;

    private EditBox spawnIntervalTicks;
    private EditBox spawnAttemptsPerPlayer;
    private EditBox minSpawnDistance;
    private EditBox maxSpawnDistance;
    private EditBox maxNearbyManagedMobs;

    AioaDaySettingsScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Day Spawn Rules"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        AioaConfig.DaySurfaceSpawns config = this.editableConfig.daySurfaceSpawns;
        this.enabled = config.enabled;
        this.overworldOnly = config.overworldOnly;
        this.requireDaytime = config.requireDaytime;
        this.requireClearSky = config.requireClearSky;
        this.preventSunlightBurn = config.preventSunlightBurn;
        this.exportMobCatalog = config.exportMobCatalog;
        this.zombieVariantMode = config.zombieVariantMode;

        int centerX = this.width / 2;
        int width = 320;
        int y = 42;
        int step = 22;

        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Enable day surface spawns", this.enabled), b -> {
            this.enabled = !this.enabled;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Enable day surface spawns", this.enabled)));
        }));
        y += step;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Overworld only", this.overworldOnly), b -> {
            this.overworldOnly = !this.overworldOnly;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Overworld only", this.overworldOnly)));
        }));
        y += step;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Require daytime", this.requireDaytime), b -> {
            this.requireDaytime = !this.requireDaytime;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Require daytime", this.requireDaytime)));
        }));
        y += step;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Require open sky", this.requireClearSky), b -> {
            this.requireClearSky = !this.requireClearSky;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Require open sky", this.requireClearSky)));
        }));
        y += step;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Prevent sunlight burn", this.preventSunlightBurn), b -> {
            this.preventSunlightBurn = !this.preventSunlightBurn;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Prevent sunlight burn", this.preventSunlightBurn)));
        }));
        y += step;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.cycleLabel("Zombie age mode", this.zombieVariantMode), b -> {
            this.zombieVariantMode = AioaScreenUtil.next(this.zombieVariantMode, AioaConfig.ZombieVariantMode.values());
            b.setMessage(Component.literal(AioaScreenUtil.cycleLabel("Zombie age mode", this.zombieVariantMode)));
        }));
        y += step;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - width / 2, y, width, AioaScreenUtil.boolLabel("Export mob catalog file", this.exportMobCatalog), b -> {
            this.exportMobCatalog = !this.exportMobCatalog;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Export mob catalog file", this.exportMobCatalog)));
        }));

        int inputX = centerX + 54;
        int inputWidth = 106;
        int fieldY = y + 30;

        this.spawnIntervalTicks = AioaScreenUtil.numberBox(inputX, fieldY, inputWidth, config.spawnIntervalTicks);
        this.spawnAttemptsPerPlayer = AioaScreenUtil.numberBox(inputX, fieldY + step, inputWidth, config.spawnAttemptsPerPlayer);
        this.minSpawnDistance = AioaScreenUtil.numberBox(inputX, fieldY + (step * 2), inputWidth, config.minSpawnDistance);
        this.maxSpawnDistance = AioaScreenUtil.numberBox(inputX, fieldY + (step * 3), inputWidth, config.maxSpawnDistance);
        this.maxNearbyManagedMobs = AioaScreenUtil.numberBox(inputX, fieldY + (step * 4), inputWidth, config.maxNearbyManagedMobs);

        this.addRenderableWidget(this.spawnIntervalTicks);
        this.addRenderableWidget(this.spawnAttemptsPerPlayer);
        this.addRenderableWidget(this.minSpawnDistance);
        this.addRenderableWidget(this.maxSpawnDistance);
        this.addRenderableWidget(this.maxNearbyManagedMobs);

        int bottomY = this.height - 30;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 154, bottomY, 150, "Done", b -> {
            AioaConfig.DaySurfaceSpawns target = this.editableConfig.daySurfaceSpawns;
            target.enabled = this.enabled;
            target.overworldOnly = this.overworldOnly;
            target.requireDaytime = this.requireDaytime;
            target.requireClearSky = this.requireClearSky;
            target.preventSunlightBurn = this.preventSunlightBurn;
            target.zombieVariantMode = this.zombieVariantMode;
            target.removeBabyVariants = this.zombieVariantMode == AioaConfig.ZombieVariantMode.REGULAR_ONLY;
            target.exportMobCatalog = this.exportMobCatalog;
            target.spawnIntervalTicks = Math.max(20, AioaScreenUtil.readNumber(this.spawnIntervalTicks, target.spawnIntervalTicks));
            target.spawnAttemptsPerPlayer = AioaScreenUtil.clamp(AioaScreenUtil.readNumber(this.spawnAttemptsPerPlayer, target.spawnAttemptsPerPlayer), 1, 16);
            target.minSpawnDistance = Math.max(8, AioaScreenUtil.readNumber(this.minSpawnDistance, target.minSpawnDistance));
            target.maxSpawnDistance = Math.max(target.minSpawnDistance + 8, AioaScreenUtil.readNumber(this.maxSpawnDistance, target.maxSpawnDistance));
            target.maxNearbyManagedMobs = Math.max(1, AioaScreenUtil.readNumber(this.maxNearbyManagedMobs, target.maxNearbyManagedMobs));
            this.editableConfig.sanitize();
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 4, bottomY, 150, "Cancel", b -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void tick() {
        this.spawnIntervalTicks.tick();
        this.spawnAttemptsPerPlayer.tick();
        this.minSpawnDistance.tick();
        this.maxSpawnDistance.tick();
        this.maxNearbyManagedMobs.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 190, 24, this.width / 2 + 190, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        guiGraphics.drawCenteredString(this.font, Component.literal("Tune spawn pacing and choose regular, mixed, or baby-only zombies."), this.width / 2, 49, AioaScreenUtil.TEXT_SUB);

        int labelX = this.width / 2 - 160;
        int y = 226;
        guiGraphics.drawString(this.font, Component.literal("Spawn interval (ticks)"), labelX, y + 6, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, Component.literal("Spawn attempts per player"), labelX, y + 28, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, Component.literal("Minimum spawn distance"), labelX, y + 50, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, Component.literal("Maximum spawn distance"), labelX, y + 72, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, Component.literal("Max nearby managed mobs"), labelX, y + 94, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
