package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AioaUiSettingsScreen extends AioaScrollableScreen {
    private final Screen parent;
    private final AioaConfig editableConfig;
    private int scale;
    private double menuVolume;
    private double uiVolume;

    AioaUiSettingsScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Interface & Menu Audio"));
        this.parent = parent;
        this.editableConfig = editableConfig;
        this.scale = editableConfig.clientUi.editorScalePercent;
        this.menuVolume = editableConfig.clientUi.menuSfxVolume;
        this.uiVolume = editableConfig.clientUi.uiSoundVolume;
    }

    @Override
    protected void init() {
        this.resetScrollLayout(540, 86, Math.max(150, this.height - 70));
        int x = this.panelLeft + 24;
        int width = this.panelWidth - 48;
        int y = 0;
        int step = AioaScreenUtil.BUTTON_HEIGHT + 12;
        this.addScrollable(AioaScreenUtil.intSlider(x, 0, width, "Editor scale", 60, 140, 5, this.scale,
                value -> this.scale = value), y); y += step;
        this.addScrollable(AioaScreenUtil.decimalSlider(x, 0, width, "Menu mob / ambience volume", 0.0D, 1.0D, 0.05D,
                this.menuVolume, value -> "Menu mob / ambience: " + Math.round(value * 100.0D) + "%",
                value -> this.menuVolume = value), y); y += step;
        this.addScrollable(AioaScreenUtil.decimalSlider(x, 0, width, "UI sound volume", 0.0D, 1.0D, 0.05D,
                this.uiVolume, value -> "UI sounds: " + Math.round(value * 100.0D) + "%",
                value -> this.uiVolume = value), y); y += step + 10;
        this.addScrollable(AioaScreenUtil.button(this.width / 2 - 90, 0, 180, "Done", button -> done()), y);
        this.finishScrollLayout(y + AioaScreenUtil.BUTTON_HEIGHT);
    }

    private void done() {
        this.editableConfig.clientUi.editorScalePercent = this.scale;
        this.editableConfig.clientUi.menuSfxVolume = this.menuVolume;
        this.editableConfig.clientUi.uiSoundVolume = this.uiVolume;
        this.transitionTo(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(graphics);
        AioaScreenUtil.drawScreenBackground(graphics, this.width, this.height);
        AioaScreenUtil.drawPanel(graphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 36, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(graphics, this.font,
                Component.literal("Graph Studio stays compact independently of Minecraft's GUI scale. Audio changes preview live."),
                this.width / 2, 54, this.panelWidth - 60, AioaScreenUtil.TEXT_SUB);
        AioaScreenUtil.drawClippedContent(graphics, this.panelLeft + 8, 86, this.panelLeft + this.panelWidth - 20,
                Math.max(150, this.height - 70), () -> AioaUiSettingsScreen.super.render(graphics, mouseX, mouseY, partialTick));
        this.finishUiRender(graphics);
    }

    @Override
    public void onClose() {
        done();
    }
}
