package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaSpawnEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

final class AioaSpawnEntryEditorScreen extends AioaScrollableScreen {
    private static final int HEADER_TO_SLIDER_GAP = 12;
    private static final int SLIDER_STACK_SPACING = 42;
    private static final int SLIDER_STACK_END_SPACING = 52;

    private final Screen parent;
    private final ResourceLocation entityId;
    private final Consumer<AioaSpawnEntry> saveConsumer;

    private boolean enabled;
    private int cachedWeight;
    private double cachedChance;
    private int cachedMin;
    private int cachedMax;
    private boolean spawnRateExpanded = false;
    private boolean groupSizeExpanded = false;
    private ButtonLikeHeaders headers;
    private AioaScreenUtil.AioaSlider weightSlider;
    private AioaScreenUtil.AioaSlider chanceSlider;
    private AioaScreenUtil.AioaSlider minSlider;
    private AioaScreenUtil.AioaSlider maxSlider;

    AioaSpawnEntryEditorScreen(Screen parent, AioaSpawnEntry entry, Consumer<AioaSpawnEntry> saveConsumer) {
        super(Component.literal("Edit Spawn Entry"));
        this.parent = parent;
        this.entityId = entry.entityId();
        this.enabled = entry.enabled();
        this.saveConsumer = saveConsumer;
        this.cachedWeight = entry.weight();
        this.cachedChance = entry.chance();
        this.cachedMin = entry.minGroupSize();
        this.cachedMax = entry.maxGroupSize();
    }

    @Override
    protected void init() {
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 76, 68, 110);
        int contentBottom = AioaScreenUtil.adaptiveContentBottom(this.height, this.height - 68, 40, contentTop, 110);
        this.resetScrollLayout(460, contentTop, contentBottom);
        int centerX = this.width / 2;
        int width = this.panelWidth - 40;
        int sliderWidth = width - 28;
        int sliderX = centerX - (sliderWidth / 2);
        int y = 0;

        this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.boolLabel("Entry enabled", this.enabled), b -> {
            this.enabled = !this.enabled;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Entry enabled", this.enabled)));
        }), y);
        y += 32;
        ButtonLikeHeaders localHeaders = new ButtonLikeHeaders();
        this.headers = localHeaders;
        localHeaders.spawnRate = this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.sectionLabel("Spawn rate", this.spawnRateExpanded), b -> {
            this.spawnRateExpanded = !this.spawnRateExpanded;
            this.init();
        }), y);
        y += 32;
        if (this.spawnRateExpanded) {
            y += HEADER_TO_SLIDER_GAP;
        }
        this.weightSlider = this.addScrollable(AioaScreenUtil.intSlider(sliderX, 0, sliderWidth, "Weight", 1, 30, 1, this.cachedWeight, value -> this.cachedWeight = value), y);
        if (this.spawnRateExpanded) {
            y += SLIDER_STACK_SPACING;
        }
        this.chanceSlider = this.addScrollable(AioaScreenUtil.decimalSlider(sliderX, 0, sliderWidth, "Chance", 0.0D, 1.0D, 0.05D, this.cachedChance,
                value -> "Chance: " + Math.round(value * 100.0D) + "%",
                value -> this.cachedChance = value), y);
        if (this.spawnRateExpanded) {
            y += SLIDER_STACK_END_SPACING;
        }
        localHeaders.groupSize = this.addScrollable(AioaScreenUtil.button(centerX - width / 2, 0, width, AioaScreenUtil.sectionLabel("Group size", this.groupSizeExpanded), b -> {
            this.groupSizeExpanded = !this.groupSizeExpanded;
            this.init();
        }), y);
        y += 32;
        if (this.groupSizeExpanded) {
            y += HEADER_TO_SLIDER_GAP;
        }
        this.minSlider = this.addScrollable(AioaScreenUtil.intSlider(sliderX, 0, sliderWidth, "Min group", 1, 10, 1, this.cachedMin, value -> {
            this.cachedMin = value;
            if (this.cachedMax < value && this.maxSlider != null) {
                this.maxSlider.setSliderValue(value);
            }
        }), y);
        if (this.groupSizeExpanded) {
            y += SLIDER_STACK_SPACING;
        }
        this.maxSlider = this.addScrollable(AioaScreenUtil.intSlider(sliderX, 0, sliderWidth, "Max group", 1, 12, 1, this.cachedMax, value -> {
            if (value < this.cachedMin) {
                this.maxSlider.setSliderValue(this.cachedMin);
                return;
            }
            this.cachedMax = value;
        }), y);
        if (this.groupSizeExpanded) {
            y += SLIDER_STACK_END_SPACING;
        }
        this.refreshSectionVisibility();

        this.addScrollable(AioaScreenUtil.button(centerX - 172, 0, 164, "Done", b -> {
            int weight = Math.max(1, this.cachedWeight);
            double chance = AioaScreenUtil.clampChance(this.cachedChance);
            int min = Math.max(1, this.cachedMin);
            int max = Math.max(min, this.cachedMax);
            this.saveConsumer.accept(new AioaSpawnEntry(
                    this.entityId + ";enabled=" + this.enabled + ";weight=" + weight + ";chance=" + chance + ";min=" + min + ";max=" + max,
                    this.entityId,
                    this.enabled,
                    weight,
                    chance,
                    min,
                    max
            ));
            this.transitionTo(this.parent);
        }), y);
        this.addScrollable(AioaScreenUtil.button(centerX + 8, 0, 164, "Cancel", b -> this.transitionTo(this.parent)), y);
        this.finishScrollLayout(y + AioaScreenUtil.BUTTON_HEIGHT + 110);
        this.setInitialFocus(this.weightSlider);
    }

    private void refreshSectionVisibility() {
        if (this.headers != null) {
            this.headers.spawnRate.setMessage(Component.literal(AioaScreenUtil.sectionLabel("Spawn rate", this.spawnRateExpanded)));
            this.headers.groupSize.setMessage(Component.literal(AioaScreenUtil.sectionLabel("Group size", this.groupSizeExpanded)));
        }
        this.setScrollableShown(this.weightSlider, this.spawnRateExpanded);
        this.setScrollableShown(this.chanceSlider, this.spawnRateExpanded);
        this.setScrollableShown(this.minSlider, this.groupSizeExpanded);
        this.setScrollableShown(this.maxSlider, this.groupSizeExpanded);
        this.updateScrollLayout();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(guiGraphics);
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        AioaScreenUtil.drawPanel(guiGraphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal(AioaScreenUtil.entityLine(this.entityId)), this.width / 2, 49, this.panelWidth - 48, AioaScreenUtil.TEXT_SUB);
        AioaScreenUtil.drawClippedContent(guiGraphics, this.panelLeft + 8, this.contentTop, this.panelLeft + this.panelWidth - 20, this.contentBottom,
                () -> AioaSpawnEntryEditorScreen.super.render(guiGraphics, mouseX, mouseY, partialTick));
        if (this.height >= 280) {
            AioaScreenUtil.drawMobPreview(
                    guiGraphics,
                    this.font,
                    this.width / 2 - 110,
                    this.height - 166,
                    220,
                    118,
                    this.entityId,
                    this.enabled,
                    List.of(
                            Component.literal("Weight: " + this.cachedWeight),
                            Component.literal("Chance: " + Math.round(this.cachedChance * 100.0D) + "%"),
                            Component.literal("Group: " + this.cachedMin + " - " + this.cachedMax),
                            Component.literal(this.enabled ? "Status: entry enabled" : "Status: entry disabled")
                    )
            );
        }
        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
        this.finishUiRender(guiGraphics);
    }

    @Override
    public void onClose() {
        this.transitionTo(this.parent);
    }

    private static final class ButtonLikeHeaders {
        private net.minecraft.client.gui.components.Button spawnRate;
        private net.minecraft.client.gui.components.Button groupSize;
    }
}
