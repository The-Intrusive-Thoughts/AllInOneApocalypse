package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class AioaDaySettingsScreen extends AioaScrollableScreen {
    private static final int HEADER_TO_SLIDER_GAP = 12;
    private static final int SLIDER_STACK_SPACING = 42;
    private static final int SLIDER_STACK_END_SPACING = 52;

    private final Screen parent;
    private final AioaConfig editableConfig;

    private boolean enabled;
    private boolean overworldOnly;
    private boolean requireDaytime;
    private boolean requireClearSky;
    private boolean preventSunlightBurn;
    private boolean exportMobCatalog;
    private AioaConfig.ZombieVariantMode zombieVariantMode;
    private int spawnIntervalTicksValue;
    private int spawnAttemptsPerPlayerValue;
    private int minSpawnDistanceValue;
    private int maxSpawnDistanceValue;
    private int maxNearbyManagedMobsValue;
    private boolean rulesExpanded = false;
    private boolean tuningExpanded = false;
    private final List<Button> ruleButtons = new ArrayList<>();
    private Button rulesHeader;
    private Button tuningHeader;
    private AioaScreenUtil.AioaSlider spawnIntervalTicks;
    private AioaScreenUtil.AioaSlider spawnAttemptsPerPlayer;
    private AioaScreenUtil.AioaSlider minSpawnDistance;
    private AioaScreenUtil.AioaSlider maxSpawnDistance;
    private AioaScreenUtil.AioaSlider maxNearbyManagedMobs;

    AioaDaySettingsScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Day Spawn Rules"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 76, 68, 110);
        int contentBottom = AioaScreenUtil.adaptiveContentBottom(this.height, this.height - 68, 40, contentTop, 110);
        this.resetScrollLayout(640, contentTop, contentBottom);
        AioaConfig.DaySurfaceSpawns config = this.editableConfig.daySurfaceSpawns;
        this.enabled = config.enabled;
        this.overworldOnly = config.overworldOnly;
        this.requireDaytime = config.requireDaytime;
        this.requireClearSky = config.requireClearSky;
        this.preventSunlightBurn = config.preventSunlightBurn;
        this.exportMobCatalog = config.exportMobCatalog;
        this.zombieVariantMode = config.zombieVariantMode;
        this.spawnIntervalTicksValue = config.spawnIntervalTicks;
        this.spawnAttemptsPerPlayerValue = config.spawnAttemptsPerPlayer;
        this.minSpawnDistanceValue = config.minSpawnDistance;
        this.maxSpawnDistanceValue = config.maxSpawnDistance;
        this.maxNearbyManagedMobsValue = config.maxNearbyManagedMobs;

        int centerX = this.width / 2;
        int width = this.panelWidth - 40;
        int y = 0;
        int step = AioaScreenUtil.BUTTON_HEIGHT + 6;

        this.ruleButtons.clear();
        this.rulesHeader = this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.sectionLabel("Rules and filters", this.rulesExpanded), b -> {
            this.rulesExpanded = !this.rulesExpanded;
            this.init();
        }), y);
        y += step;
        this.ruleButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Enable day surface spawns", this.enabled), b -> {
            this.enabled = !this.enabled;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Enable day surface spawns", this.enabled)));
        }), y));
        if (this.rulesExpanded) {
            y += step;
        }
        this.ruleButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Overworld only", this.overworldOnly), b -> {
            this.overworldOnly = !this.overworldOnly;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Overworld only", this.overworldOnly)));
        }), y));
        if (this.rulesExpanded) {
            y += step;
        }
        this.ruleButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Require daytime", this.requireDaytime), b -> {
            this.requireDaytime = !this.requireDaytime;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Require daytime", this.requireDaytime)));
        }), y));
        if (this.rulesExpanded) {
            y += step;
        }
        this.ruleButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Require open sky", this.requireClearSky), b -> {
            this.requireClearSky = !this.requireClearSky;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Require open sky", this.requireClearSky)));
        }), y));
        if (this.rulesExpanded) {
            y += step;
        }
        this.ruleButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Prevent sunlight burn", this.preventSunlightBurn), b -> {
            this.preventSunlightBurn = !this.preventSunlightBurn;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Prevent sunlight burn", this.preventSunlightBurn)));
        }), y));
        if (this.rulesExpanded) {
            y += step;
        }
        this.ruleButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.cycleLabel("Zombie age mode", this.zombieVariantMode), b -> {
            this.zombieVariantMode = AioaScreenUtil.next(this.zombieVariantMode, AioaConfig.ZombieVariantMode.values());
            b.setMessage(Component.literal(AioaScreenUtil.cycleLabel("Zombie age mode", this.zombieVariantMode)));
        }), y));
        if (this.rulesExpanded) {
            y += step;
        }
        this.ruleButtons.add(this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Export mob catalog file", this.exportMobCatalog), b -> {
            this.exportMobCatalog = !this.exportMobCatalog;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Export mob catalog file", this.exportMobCatalog)));
        }), y));
        if (this.rulesExpanded) {
            y += step;
        }

        y += 6;
        this.tuningHeader = this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.sectionLabel("Spawn pacing sliders", this.tuningExpanded), b -> {
            this.tuningExpanded = !this.tuningExpanded;
            this.init();
        }), y);
        y += step;
        if (this.tuningExpanded) {
            y += HEADER_TO_SLIDER_GAP;
        }

        int sliderWidth = width - 28;
        int sliderX = centerX - (sliderWidth / 2);
        this.spawnIntervalTicks = this.addScrollable(AioaScreenUtil.intSlider(sliderX, 0, sliderWidth, "Spawn interval", 20, 24000, 20, this.spawnIntervalTicksValue, value -> this.spawnIntervalTicksValue = value), y);
        y += this.tuningExpanded ? SLIDER_STACK_SPACING : 0;
        this.spawnAttemptsPerPlayer = this.addScrollable(AioaScreenUtil.intSlider(sliderX, 0, sliderWidth, "Attempts per player", 1, 16, 1, this.spawnAttemptsPerPlayerValue, value -> this.spawnAttemptsPerPlayerValue = value), y);
        y += this.tuningExpanded ? SLIDER_STACK_SPACING : 0;
        this.minSpawnDistance = this.addScrollable(AioaScreenUtil.intSlider(sliderX, 0, sliderWidth, "Minimum distance", 8, 128, 1, this.minSpawnDistanceValue, value -> {
            this.minSpawnDistanceValue = value;
            if (this.maxSpawnDistanceValue < value && this.maxSpawnDistance != null) {
                this.maxSpawnDistance.setSliderValue(value + 8);
            }
        }), y);
        y += this.tuningExpanded ? SLIDER_STACK_SPACING : 0;
        this.maxSpawnDistance = this.addScrollable(AioaScreenUtil.intSlider(sliderX, 0, sliderWidth, "Maximum distance", 16, 256, 1, this.maxSpawnDistanceValue, value -> this.maxSpawnDistanceValue = value), y);
        y += this.tuningExpanded ? SLIDER_STACK_SPACING : 0;
        this.maxNearbyManagedMobs = this.addScrollable(AioaScreenUtil.intSlider(sliderX, 0, sliderWidth, "Nearby mob cap", 1, 256, 1, this.maxNearbyManagedMobsValue, value -> this.maxNearbyManagedMobsValue = value), y);
        y += this.tuningExpanded ? SLIDER_STACK_END_SPACING : 0;

        this.refreshVisibility();

        this.addScrollable(AioaScreenUtil.button(centerX - 172, 0, 164, "Done", b -> {
            AioaConfig.DaySurfaceSpawns target = this.editableConfig.daySurfaceSpawns;
            target.enabled = this.enabled;
            target.overworldOnly = this.overworldOnly;
            target.requireDaytime = this.requireDaytime;
            target.requireClearSky = this.requireClearSky;
            target.preventSunlightBurn = this.preventSunlightBurn;
            target.zombieVariantMode = this.zombieVariantMode;
            target.removeBabyVariants = this.zombieVariantMode == AioaConfig.ZombieVariantMode.REGULAR_ONLY;
            target.exportMobCatalog = this.exportMobCatalog;
            target.spawnIntervalTicks = Math.max(20, this.spawnIntervalTicksValue);
            target.spawnAttemptsPerPlayer = AioaScreenUtil.clamp(this.spawnAttemptsPerPlayerValue, 1, 16);
            target.minSpawnDistance = Math.max(8, this.minSpawnDistanceValue);
            target.maxSpawnDistance = Math.max(target.minSpawnDistance + 8, this.maxSpawnDistanceValue);
            target.maxNearbyManagedMobs = Math.max(1, this.maxNearbyManagedMobsValue);
            this.editableConfig.sanitize();
            this.minecraft.setScreen(this.parent);
        }), y);
        this.addScrollable(AioaScreenUtil.button(centerX + 8, 0, 164, "Cancel", b -> this.minecraft.setScreen(this.parent)), y);
        this.finishScrollLayout(y + AioaScreenUtil.BUTTON_HEIGHT);
    }

    private void refreshVisibility() {
        this.rulesHeader.setMessage(Component.literal(AioaScreenUtil.sectionLabel("Rules and filters", this.rulesExpanded)));
        for (Button button : this.ruleButtons) {
            this.setScrollableShown(button, this.rulesExpanded);
        }
        this.tuningHeader.setMessage(Component.literal(AioaScreenUtil.sectionLabel("Spawn pacing sliders", this.tuningExpanded)));
        this.setScrollableShown(this.spawnIntervalTicks, this.tuningExpanded);
        this.setScrollableShown(this.spawnAttemptsPerPlayer, this.tuningExpanded);
        this.setScrollableShown(this.minSpawnDistance, this.tuningExpanded);
        this.setScrollableShown(this.maxSpawnDistance, this.tuningExpanded);
        this.setScrollableShown(this.maxNearbyManagedMobs, this.tuningExpanded);
        this.updateScrollLayout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        AioaScreenUtil.drawPanel(guiGraphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal("Set when day surface spawns can happen and how often they try to appear."), this.width / 2, 49, this.panelWidth - 72, AioaScreenUtil.TEXT_SUB);
        AioaScreenUtil.drawClippedContent(guiGraphics, this.panelLeft + 8, this.contentTop, this.panelLeft + this.panelWidth - 20, this.contentBottom,
                () -> AioaDaySettingsScreen.super.render(guiGraphics, mouseX, mouseY, partialTick));
        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
