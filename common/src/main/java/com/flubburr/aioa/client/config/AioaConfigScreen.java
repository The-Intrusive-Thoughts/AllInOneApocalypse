package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.Util;

import java.net.URI;

public final class AioaConfigScreen extends AioaScrollableScreen {

    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath("aioa", "textures/gui/aioa-logo2.png");
    private static final int LOGO_TEXTURE_WIDTH = 1024;
    private static final int LOGO_TEXTURE_HEIGHT = 230;

    private final Screen parent;
    private AioaConfig editableConfig;
    private boolean canSave;
    private Button docsButton;
    private Button tutorialSkipButton;
    private Button tutorialBackButton;
    private Button tutorialNextButton;
    private Button tutorialCloseButton;
    private Button audioToggleButton;
    private int tutorialStep;

    private AioaConfigScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("AIOA Configuration"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    public static Screen create(Screen parent) {
        return new AioaConfigScreen(parent, AioaConfigManager.getConfigCopy());
    }

    @Override
    protected void init() {
        this.canSave = this.canEditServerAffectingConfig();
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

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44, "Quick Presets", b ->
                this.transitionTo(new AioaPresetScreen(this, this.editableConfig))), y);
        y += step + 6;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44, "Behavior Graph Studio   [F7]", b ->
                this.transitionTo(AioaBehaviorEditorScreen.create(this, this.editableConfig))), y);
        y += step + 6;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44, "Open Creative Spawn Studio", b ->
                this.transitionTo(AioaSpawnStudioScreen.create(this))), y);
        y += step + 6;

        this.docsButton = AioaScreenUtil.button(leftX, 0, columnWidth, Component.translatable("aioa.docs.button").getString(), b ->
                this.transitionTo(new AioaDocsScreen(this)));
        this.addScrollable(this.docsButton, y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Interface & Menu Audio", b ->
                this.transitionTo(new AioaUiSettingsScreen(this, this.editableConfig))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step + 12;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, columnWidth, "Hostile Spawn Rules", b ->
                this.transitionTo(new AioaHostileSettingsScreen(this, this.editableConfig))), y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Day Spawn Rules", b ->
                this.transitionTo(new AioaDaySettingsScreen(this, this.editableConfig))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, columnWidth, "Behavior Engine Safety", b ->
                this.transitionTo(new AioaBehaviorEngineSettingsScreen(this, this.editableConfig))), y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Choose Allowed Hostiles", b ->
                this.transitionTo(AioaEntityToggleScreen.forHostiles(this, this.editableConfig))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, columnWidth, "Edit Day Spawn Pool", b ->
                this.transitionTo(new AioaSpawnPoolScreen(this, this.editableConfig))), y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Allowed Biomes", b ->
                this.transitionTo(AioaBiomeToggleScreen.create(this, this.editableConfig))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step + 14;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44,
                "Community Discord", b -> openExternal("https://discord.gg/uHqJNDewnU")), y);
        y += step;
        this.addScrollable(AioaScreenUtil.button(leftX, 0, columnWidth, "Credits: DarkFox Studios", b ->
                openExternal("https://www.youtube.com/@DarkFoxStudiosOF")), y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Credits: flubburr", b ->
                openExternal("https://www.youtube.com/@flubburr")), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step + 10;

        int actionWidth = singleColumn ? columnWidth : (this.panelWidth - 58) / 2;
        this.addScrollable(AioaScreenUtil.button(singleColumn ? leftX : centerX - actionWidth - (gap / 2), 0, actionWidth, "Reset to Defaults", b ->
                this.transitionTo(new AioaConfigScreen(this.parent, AioaConfig.createDefault()))), y);
        this.addScrollable(AioaScreenUtil.button(singleColumn ? leftX : centerX + (gap / 2), 0, actionWidth, "Reload Saved Config", b ->
                this.transitionTo(new AioaConfigScreen(this.parent, AioaConfigManager.getConfigCopy()))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step + 10;

        var doneButton = this.addScrollable(AioaScreenUtil.button(centerX - 90, 0, 180, "Done", b -> {
            this.editableConfig = this.editableConfig.sanitize();
            AioaConfigManager.save(this.editableConfig);
            this.onClose();
        }), y);
        doneButton.active = this.canSave;
        this.finishScrollLayout(y + AioaScreenUtil.BUTTON_HEIGHT);

        this.audioToggleButton = this.addRenderableWidget(AioaScreenUtil.button(this.width - 158, this.height - 32, 148,
                this.editableConfig.clientUi.audioMuted ? "Audio: OFF" : "Audio: ON", b -> toggleAudio()));

        if (!this.editableConfig.clientUi.tutorialCompleted) {
            this.tutorialSkipButton = this.addRenderableWidget(AioaScreenUtil.button(this.width / 2 - 174, this.height - 66, 108, "Skip tutorial", b -> finishTutorial(false)));
            this.tutorialBackButton = this.addRenderableWidget(AioaScreenUtil.button(this.width / 2 - 54, this.height - 66, 108, "Back", b -> {
                this.tutorialStep = Math.max(0, this.tutorialStep - 1); this.rebuildWidgets();
            }));
            this.tutorialBackButton.active = this.tutorialStep > 0;
            this.tutorialNextButton = this.addRenderableWidget(AioaScreenUtil.button(this.width / 2 + 66, this.height - 66, 108,
                    this.tutorialStep == 3 ? "Open docs" : "Next", b -> {
                if (this.tutorialStep >= 3) finishTutorial(true);
                else { this.tutorialStep++; this.rebuildWidgets(); }
            }));
            int boxWidth = Math.min(520, this.width - 32);
            this.tutorialCloseButton = this.addRenderableWidget(AioaScreenUtil.button(this.width / 2 + boxWidth / 2 - 30,
                    this.height - 127, 24, "x", b -> finishTutorial(false)));
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.transitionTo(this.parent);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(guiGraphics);
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
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
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(logoX, logoY);
        guiGraphics.pose().scale(logoScale, logoScale);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0.0F, 0.0F, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
        guiGraphics.pose().popMatrix();
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
        if (!this.canSave) {
            AioaScreenUtil.drawWrappedCenteredText(
                    guiGraphics,
                    this.font,
                    Component.literal("Connected to a multiplayer or dedicated server. Saving apocalypse gameplay settings is disabled in this session."),
                    this.width / 2,
                    Math.max(titleY + (compactHeader ? 14 : 34), this.contentTop - 34),
                    this.panelWidth - 72,
                    0xFFE7C76A
            );
        }
        AioaScreenUtil.drawClippedContent(guiGraphics, this.panelLeft + 8, this.contentTop, this.panelLeft + this.panelWidth - 20, this.contentBottom,
                () -> AioaConfigScreen.super.render(guiGraphics, mouseX, mouseY, partialTick));
        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
        this.audioToggleButton.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.editableConfig.clientUi.showDocsHint) {
            int hintWidth = Math.min(390, this.width - 30);
            int hintX = this.width - hintWidth - 14;
            int hintY = 12;
            guiGraphics.fill(hintX, hintY, hintX + hintWidth, hintY + 52, 0xF21A2B21);
            guiGraphics.drawString(this.font, "NEW: INTEGRATED HELP & SHOWCASES", hintX + 10, hintY + 9, 0xFF78E5A5);
            guiGraphics.drawString(this.font, "x", hintX + hintWidth - 14, hintY + 8, 0xFFFF9A9A);
            AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font,
                    Component.literal("Open Documentation & Creator Guide for controls, every node, live mob examples, spawning, and multiplayer safety."),
                    hintX + hintWidth / 2, hintY + 23, hintWidth - 18, AioaScreenUtil.TEXT_SUB);
            if (this.docsButton != null) drawDocsArrow(guiGraphics, hintX + 18, hintY + 52,
                    this.docsButton.getX() + this.docsButton.getWidth() / 2, this.docsButton.getY());
        }
        if (!this.editableConfig.clientUi.tutorialCompleted) {
            drawTutorial(guiGraphics);
            this.tutorialSkipButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.tutorialBackButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.tutorialNextButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.tutorialCloseButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        this.finishUiRender(guiGraphics);
    }

    private void drawTutorial(GuiGraphics graphics) {
        int boxWidth = Math.min(520, this.width - 32);
        int left = this.width / 2 - boxWidth / 2;
        int top = this.height - 158;
        graphics.fill(0, 0, this.width, this.height, 0xC8000000);
        graphics.fill(left - 2, top - 2, left + boxWidth + 2, this.height - 26, 0xFF62E99C);
        graphics.fill(left, top, left + boxWidth, this.height - 28, 0xFF101713);
        String title = switch (this.tutorialStep) {
            case 1 -> "2 / 4  BUILD BEHAVIOR WITH NODES";
            case 2 -> "3 / 4  PREVIEW, SPAWN, AND SELECT";
            case 3 -> "4 / 4  DOCUMENTATION & SETTINGS";
            default -> "1 / 4  START WITH A QUICK PRESET";
        };
        String body = switch (this.tutorialStep) {
            case 1 -> "Open Graph Studio or press F7 in game. Its palette, inspector, parameters, viewport, and guide are draggable windows.";
            case 2 -> "Use the real 3D viewport, Spawn Studio, or leave the UI and right-click a mob to bind a graph safely.";
            case 3 -> "Docs explains every node without code. Use the small Audio button at bottom-right for instant mute, or Interface settings for exact volumes.";
            default -> "Balanced, Cinematic, and Horde presets give a safe base before you customize individual systems.";
        };
        graphics.drawString(this.font, title, left + 12, top + 9, 0xFF78E5A5);
        AioaScreenUtil.drawWrappedCenteredText(graphics, this.font, Component.literal(body), left + boxWidth / 2, top + 30,
                boxWidth - 36, 0xFFFFFFFF);
    }

    private static void drawDocsArrow(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        int midY = Math.max(y1 + 8, y2 - 12);
        graphics.fill(x1, y1, x1 + 2, midY, 0xFF78E5A5);
        graphics.fill(Math.min(x1, x2), midY, Math.max(x1, x2) + 2, midY + 2, 0xFF78E5A5);
        graphics.fill(x2 - 4, y2 - 6, x2 + 5, y2 - 4, 0xFF78E5A5);
        graphics.fill(x2 - 2, y2 - 4, x2 + 3, y2, 0xFF78E5A5);
    }

    private void finishTutorial(boolean openDocs) {
        this.editableConfig.clientUi.tutorialCompleted = true;
        persistGuideState();
        if (openDocs) this.transitionTo(new AioaDocsScreen(this));
        else this.rebuildWidgets();
    }

    private void persistGuideState() {
        AioaConfig persisted = AioaConfigManager.getConfigCopy();
        persisted.clientUi.tutorialCompleted = this.editableConfig.clientUi.tutorialCompleted;
        persisted.clientUi.showDocsHint = this.editableConfig.clientUi.showDocsHint;
        AioaConfigManager.save(persisted);
    }

    private void toggleAudio() {
        this.editableConfig.clientUi.audioMuted = !this.editableConfig.clientUi.audioMuted;
        AioaConfig persisted = AioaConfigManager.getConfigCopy();
        persisted.clientUi.audioMuted = this.editableConfig.clientUi.audioMuted;
        AioaConfigManager.save(persisted);
        this.audioToggleButton.setMessage(Component.literal(this.editableConfig.clientUi.audioMuted ? "Audio: OFF" : "Audio: ON"));
    }

    private static void openExternal(String url) {
        Util.getPlatform().openUri(URI.create(url));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && this.audioToggleButton != null && this.audioToggleButton.isMouseOver(mouseX, mouseY)) {
            toggleAudio();
            return true;
        }
        if (!this.editableConfig.clientUi.tutorialCompleted) {
            int boxWidth = Math.min(520, this.width - 32);
            boolean footerControls = mouseY >= this.height - 72 && mouseY <= this.height - 34
                    && mouseX >= this.width / 2 - 184 && mouseX <= this.width / 2 + 184;
            boolean closeControl = mouseX >= this.width / 2 + boxWidth / 2 - 34
                    && mouseX <= this.width / 2 + boxWidth / 2
                    && mouseY >= this.height - 132 && mouseY <= this.height - 96;
            if (button == 0 && closeControl) { finishTutorial(false); return true; }
            if (button == 0 && footerControls) {
                if (this.tutorialSkipButton.isMouseOver(mouseX, mouseY)) { finishTutorial(false); return true; }
                if (this.tutorialBackButton.isMouseOver(mouseX, mouseY) && this.tutorialBackButton.active) {
                    this.tutorialStep = Math.max(0, this.tutorialStep - 1); this.rebuildWidgets(); return true;
                }
                if (this.tutorialNextButton.isMouseOver(mouseX, mouseY)) {
                    if (this.tutorialStep >= 3) finishTutorial(true);
                    else { this.tutorialStep++; this.rebuildWidgets(); }
                    return true;
                }
            }
            if (!footerControls && !closeControl) return true;
        }
        if (button == 0 && this.editableConfig.clientUi.showDocsHint) {
            int hintWidth = Math.min(390, this.width - 30);
            int hintX = this.width - hintWidth - 14;
            if (mouseX >= hintX + hintWidth - 24 && mouseX <= hintX + hintWidth
                    && mouseY >= 12 && mouseY <= 36) {
                this.editableConfig.clientUi.showDocsHint = false;
                persistGuideState();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
