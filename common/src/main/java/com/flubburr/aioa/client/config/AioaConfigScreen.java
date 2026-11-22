package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

import java.net.URI;

public final class AioaConfigScreen extends AioaScrollableScreen {

    private static final ResourceLocation LOGO = new ResourceLocation("aioa", "textures/gui/aioa-logo2.png");
    private static final int LOGO_TEXTURE_WIDTH = 1024;
    private static final int LOGO_TEXTURE_HEIGHT = 230;

    private final Screen parent;
    private AioaConfig editableConfig;
    private boolean canSave;
    private Button audioToggleButton;
    private Button replayTutorialButton;
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
        if (!this.editableConfig.clientUi.tutorialCompleted) {
            this.resetScrollLayout(480, 0, this.height);
            this.finishScrollLayout(0);
            buildTutorial();
            return;
        }
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 152, 56, 112);
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

        int rowWidth = singleColumn ? columnWidth : this.panelWidth - 44;
        this.addScrollable(AioaScreenUtil.toolButton(leftX, 0, rowWidth, "Behavior editor", BehaviorBoard.Part.SIGNAL, "F7", b ->
                this.transitionTo(AioaBehaviorEditorScreen.create(this, this.editableConfig))), y);
        y += step;

        this.addScrollable(AioaScreenUtil.toolButton(leftX, 0, rowWidth, "Spawn studio", BehaviorBoard.Part.EGG, "", b ->
                this.transitionTo(AioaSpawnStudioScreen.create(this))), y);
        y += step;

        this.addScrollable(AioaScreenUtil.toolButton(leftX, 0, rowWidth, "Apocalypse presets", BehaviorBoard.Part.BOOK, "", b ->
                this.transitionTo(new AioaPresetScreen(this, this.editableConfig))), y);
        y += step + 6;

        var docsButton = AioaScreenUtil.button(leftX, 0, columnWidth, Component.translatable("aioa.docs.button").getString(), b ->
                this.transitionTo(new AioaDocsScreen(this)));
        this.addScrollable(docsButton, y);
        this.addScrollable(AioaScreenUtil.button(rightX, 0, columnWidth, "Interface & Menu Audio", b ->
                this.transitionTo(new AioaUiSettingsScreen(this, this.editableConfig))), singleColumn ? y + step : y);
        y += singleColumn ? step * 2 : step + 12;

        this.addScrollable(AioaScreenUtil.button(leftX, 0, singleColumn ? columnWidth : this.panelWidth - 44,
                "Mob AI and targeting", b -> this.transitionTo(new AioaZombieAiScreen(this, this.editableConfig))), y);
        y += step;

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
        this.replayTutorialButton = this.addRenderableWidget(AioaScreenUtil.menuButton(10, this.height - 32, 94, "Replay tutorial", b -> {
            this.tutorialStep = 0;
            this.editableConfig.clientUi.tutorialCompleted = false;
            this.rebuildWidgets();
        }));

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
        if (!this.editableConfig.clientUi.tutorialCompleted) {
            drawTutorial(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            this.finishUiRender(guiGraphics);
            return;
        }
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

        float logoScale = logoWidth / (float) LOGO_TEXTURE_WIDTH;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(logoX, logoY, 0.0F);
        guiGraphics.pose().scale(logoScale, logoScale, 1.0F);
        guiGraphics.blit(LOGO, 0, 0, 0, 0, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
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
        this.replayTutorialButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.finishUiRender(guiGraphics);
    }

    private int tutorialWidth() { return Math.min(460, this.width - 32); }
    private int tutorialTop() { return (this.height - 174) / 2; }

    private void buildTutorial() {
        int left = (this.width - tutorialWidth()) / 2;
        int right = left + tutorialWidth();
        int y = tutorialTop() + 134;
        this.addRenderableWidget(AioaScreenUtil.menuButton(left + 12, y, 50, "Skip", b -> finishTutorial(false)));
        var back = this.addRenderableWidget(AioaScreenUtil.menuButton(right - 164, y, 56, "Back", b -> {
            this.tutorialStep--;
            this.rebuildWidgets();
        }));
        back.active = this.tutorialStep > 0;
        this.addRenderableWidget(AioaScreenUtil.button(right - 100, y, 84, this.tutorialStep == 3 ? "Open guide" : "Next", b -> {
            if (this.tutorialStep == 3) finishTutorial(true);
            else { this.tutorialStep++; this.rebuildWidgets(); }
        }));
    }

    private void drawTutorial(GuiGraphics g) {
        int width = tutorialWidth(), left = (this.width - width) / 2, top = tutorialTop();
        int right = left + width;
        String title = switch (this.tutorialStep) {
            case 1 -> "Wire a behavior";
            case 2 -> "Watch a zombie hunt";
            case 3 -> "Explore the field guide";
            default -> "Choose a starting preset";
        };
        String body = switch (this.tutorialStep) {
            case 1 -> "Press F7 in Creative. Follow a wire from an output pin to its input. Double-click a node to change its settings, or press Ctrl+K to search the library.";
            case 2 -> "Head outside in daylight on Normal or Hard. Switch to Survival and sprint past cover. Nearby idle zombies can investigate the last place they heard you.";
            case 3 -> "The guide covers graph controls, the node reference, spawn tools and multiplayer editing. You can reopen it from the configuration screen.";
            default -> "Open Apocalypse presets from the configuration screen. Horde checks for daytime surface spawns every four seconds and enables coordinated pursuit.";
        };
        BehaviorBoard.Part icon = switch (this.tutorialStep) {
            case 1 -> BehaviorBoard.Part.SIGNAL; case 2 -> BehaviorBoard.Part.TARGET;
            case 3 -> BehaviorBoard.Part.BOOK; default -> BehaviorBoard.Part.EGG;
        };
        g.fill(left, top, right, top + 174, 0xFF1A261F);
        g.fill(left, top, right, top + 30, 0xFF22382A);
        icon.draw(g, left + 14, top + 7);
        g.drawString(this.font, "Field guide", left + 37, top + 11, BehaviorBoard.BONE, false);
        String page = (tutorialStep + 1) + " / 4";
        g.drawString(this.font, page, right - this.font.width(page) - 16, top + 11, BehaviorBoard.PATINA, false);
        g.drawString(this.font, title, left + 16, top + 47, BehaviorBoard.BONE, false);
        int y = top + 65;
        for (var line : this.font.split(Component.literal(body), width - 32)) {
            g.drawString(this.font, line, left + 16, y, AioaScreenUtil.TEXT_SUB, false);
            y += 12;
        }
        g.fill(left, top + 126, right, top + 127, 0xFF35483A);
    }

    private void finishTutorial(boolean openDocs) {
        this.editableConfig.clientUi.tutorialCompleted = true;
        persistGuideState();
        this.rebuildWidgets();
        if (openDocs) this.transitionTo(new AioaDocsScreen(this));
    }

    private void persistGuideState() {
        AioaConfig persisted = AioaConfigManager.getConfigCopy();
        persisted.clientUi.tutorialCompleted = this.editableConfig.clientUi.tutorialCompleted;
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

}
