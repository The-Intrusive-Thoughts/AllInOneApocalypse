package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class AioaConfigScreen extends AioaScrollableScreen {

    private static final ResourceLocation LOGO = logoTexture();
    private static final int LOGO_TEXTURE_WIDTH = 1024;
    private static final int LOGO_TEXTURE_HEIGHT = 230;

    private final Screen parent;
    private AioaConfig editableConfig;
    private static boolean welcomeShown;
    private boolean showWelcome = !welcomeShown;

    private AioaConfigScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("AIOA Configuration"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    public static Screen create(Screen parent) {
        return new AioaConfigScreen(parent, AioaConfigManager.getConfigCopy());
    }

    private static ResourceLocation logoTexture() {
        return ResourceLocation.fromNamespaceAndPath("aioa", "textures/gui/aioa-logo2.png");
    }

    @Override
    protected void init() {
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 238, 68, 120);
        int contentBottom = AioaScreenUtil.adaptiveContentBottom(this.height, this.height - 68, 40, contentTop, 120);
        this.resetScrollLayout(760, contentTop, contentBottom);
        int centerX = this.width / 2;
        int gap = 14;
        boolean singleColumn = this.panelWidth < 620;
        int columnWidth = singleColumn ? this.panelWidth - 44 : (this.panelWidth - 58) / 2;
        int leftX = this.panelLeft + 22;
        int rightX = singleColumn ? leftX : leftX + columnWidth + gap;
        int y = 0;
        int step = AioaScreenUtil.BUTTON_HEIGHT + 10;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44, Component.translatable("aioa.docs.button").getString(), b -> {
            welcomeShown = true;
            this.showWelcome = false;
            this.minecraft.setScreen(new AioaDocsScreen(this));
        }), y);
        y += step + 6;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, columnWidth, "Hostile Spawn Rules", b ->
                this.minecraft.setScreen(new AioaHostileSettingsScreen(this, this.editableConfig))), y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Day Spawn Rules", b ->
                this.minecraft.setScreen(new AioaDaySettingsScreen(this, this.editableConfig))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, columnWidth, "Behavior Graph Studio", b ->
                this.minecraft.setScreen(AioaBehaviorEditorScreen.create(this, this.editableConfig))), y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Choose Allowed Hostiles", b ->
                this.minecraft.setScreen(AioaEntityToggleScreen.forHostiles(this, this.editableConfig))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44, "Behavior Engine Limits & Safety", b ->
                this.minecraft.setScreen(new AioaBehaviorEngineSettingsScreen(this, this.editableConfig))), y);
        y += step;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, columnWidth, "Edit Day Spawn Pool", b ->
                this.minecraft.setScreen(new AioaSpawnPoolScreen(this, this.editableConfig))), y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Allowed Biomes", b ->
                this.minecraft.setScreen(AioaBiomeToggleScreen.create(this, this.editableConfig))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step + 14;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44, "Open Creative Spawn Studio", b ->
                this.minecraft.setScreen(AioaSpawnStudioScreen.create(this))), y);
        y += step + 6;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44, "Quick Presets", b ->
                this.minecraft.setScreen(new AioaPresetScreen(this, this.editableConfig))), y);
        y += step + 6;

        int actionWidth = singleColumn ? columnWidth : (this.panelWidth - 58) / 2;
        this.addScrollable(AioaScreenUtil.button(singleColumn ? leftX : centerX - actionWidth - (gap / 2), 0, actionWidth, "Reset to Defaults", b ->
                this.minecraft.setScreen(new AioaConfigScreen(this.parent, AioaConfig.createDefault()))), y);
        this.addScrollable(AioaScreenUtil.button(singleColumn ? leftX : centerX + (gap / 2), 0, actionWidth, "Reload Saved Config", b ->
                this.minecraft.setScreen(new AioaConfigScreen(this.parent, AioaConfigManager.getConfigCopy()))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step + 10;

        this.addScrollable(AioaScreenUtil.button(centerX - 172, 0, 164, "Save", b -> {
            this.editableConfig = this.editableConfig.sanitize();
            AioaConfigManager.save(this.editableConfig);
            this.onClose();
        }), y);
        this.addScrollable(AioaScreenUtil.button(centerX + 8, 0, 164, "Cancel", b -> this.onClose()), y);
        this.finishScrollLayout(y + AioaScreenUtil.BUTTON_HEIGHT);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AioaScreenUtil.pushUiLayer(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        int headerBottom = this.contentTop - 12;
        boolean compactHeader = this.height < 300;
        int reservedTextHeight = compactHeader ? 22 : 42;
        int maxLogoWidth = Math.min(this.panelWidth - 64, 560);
        int maxLogoHeight = Math.max(20, Math.min(118, headerBottom - 38 - reservedTextHeight));
        int logoWidth = maxLogoWidth;
        int logoHeight = Math.max(1, (logoWidth * LOGO_TEXTURE_HEIGHT) / LOGO_TEXTURE_WIDTH);
        if (logoHeight > maxLogoHeight) {
            logoHeight = maxLogoHeight;
            logoWidth = Math.max(1, (logoHeight * LOGO_TEXTURE_WIDTH) / LOGO_TEXTURE_HEIGHT);
        }
        int logoX = this.width / 2 - (logoWidth / 2);
        int logoY = 38;
        AioaScreenUtil.drawInsetPanel(guiGraphics, logoX - 10, logoY - 8, logoX + logoWidth + 10, logoY + logoHeight + 8, false);
        float logoScale = logoWidth / (float) LOGO_TEXTURE_WIDTH;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(logoX, logoY, 0.0F);
        guiGraphics.pose().scale(logoScale, logoScale, 1.0F);
        guiGraphics.blit(RenderType::guiTextured, LOGO, 0, 0, 0.0F, 0.0F, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
        guiGraphics.pose().popPose();
        int titleY = Math.min(logoY + logoHeight + 10, headerBottom - (compactHeader ? 12 : 26));
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, AioaScreenUtil.TEXT_MAIN);
        if (!compactHeader) {
            AioaScreenUtil.drawWrappedCenteredText(
                    guiGraphics,
                    this.font,
                    Component.literal("Configure day spawns, hostile filters, biome limits, and zombie behavior."),
                    this.width / 2,
                    Math.min(titleY + 16, headerBottom - 20),
                    this.panelWidth - 72,
                    AioaScreenUtil.TEXT_SUB
            );
        }
        AioaConfigScreen.super.render(guiGraphics, mouseX, mouseY, partialTick);
        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
        if (this.showWelcome) {
            int hintWidth = Math.min(390, this.width - 30);
            int hintX = this.width - hintWidth - 14;
            int hintY = 12;
            guiGraphics.fill(hintX, hintY, hintX + hintWidth, hintY + 52, 0xF21A2B21);
            guiGraphics.drawString(this.font, "NEW: INTEGRATED HELP & SHOWCASES", hintX + 10, hintY + 9, 0xFF78E5A5);
            AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font,
                    Component.literal("Open Documentation & Creator Guide for controls, every node, live mob examples, spawning, and multiplayer safety."),
                    hintX + hintWidth / 2, hintY + 23, hintWidth - 18, AioaScreenUtil.TEXT_SUB);
        }
        AioaScreenUtil.popUiLayer(guiGraphics);
    }
}
