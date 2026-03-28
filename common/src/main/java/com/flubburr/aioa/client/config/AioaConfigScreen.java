package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AioaConfigScreen extends Screen {

    private final Screen parent;
    private AioaConfig editableConfig;

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
        int centerX = this.width / 2;
        int columnWidth = 220;
        int gap = 12;
        int leftX = centerX - columnWidth - (gap / 2);
        int rightX = centerX + (gap / 2);
        int y = 46;
        int step = 24;

        this.addRenderableWidget(AioaScreenUtil.button(leftX, y, columnWidth, "Hostile Spawn Rules", b ->
                this.minecraft.setScreen(new AioaHostileSettingsScreen(this, this.editableConfig))));
        this.addRenderableWidget(AioaScreenUtil.button(rightX, y, columnWidth, "Day Spawn Rules", b ->
                this.minecraft.setScreen(new AioaDaySettingsScreen(this, this.editableConfig))));
        y += step;

        this.addRenderableWidget(AioaScreenUtil.button(leftX, y, columnWidth, "Zombie AI and Targeting", b ->
                this.minecraft.setScreen(new AioaZombieAiScreen(this, this.editableConfig))));
        this.addRenderableWidget(AioaScreenUtil.button(rightX, y, columnWidth, "Choose Allowed Hostiles", b ->
                this.minecraft.setScreen(AioaEntityToggleScreen.forHostiles(this, this.editableConfig))));
        y += step;

        this.addRenderableWidget(AioaScreenUtil.button(leftX, y, columnWidth, "Edit Day Spawn Pool", b ->
                this.minecraft.setScreen(new AioaSpawnPoolScreen(this, this.editableConfig))));
        this.addRenderableWidget(AioaScreenUtil.button(rightX, y, columnWidth, "Allowed Biomes", b ->
                this.minecraft.setScreen(AioaTextListScreen.forBiomes(this, this.editableConfig))));
        y += step + 8;

        this.addRenderableWidget(AioaScreenUtil.button(centerX - 226, y, 220, "Reset to Defaults", b ->
                this.minecraft.setScreen(new AioaConfigScreen(this.parent, AioaConfig.createDefault()))));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 6, y, 220, "Reload Saved Config", b ->
                this.minecraft.setScreen(new AioaConfigScreen(this.parent, AioaConfigManager.getConfigCopy()))));

        int bottomY = this.height - 30;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 154, bottomY, 150, "Save", b -> {
            this.editableConfig = this.editableConfig.sanitize();
            AioaConfigManager.save(this.editableConfig);
            this.onClose();
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 4, bottomY, 150, "Cancel", b -> this.onClose()));
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 238, 24, this.width / 2 + 238, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 32, AioaScreenUtil.TEXT_MAIN);
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("A cleaner apocalypse setup screen with named mob pickers and zombie AI controls."),
                this.width / 2,
                48,
                AioaScreenUtil.TEXT_SUB
        );
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
