package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AioaPresetScreen extends Screen {
    private final Screen parent;
    private final AioaConfig editableConfig;
    private String status = "Choose a starting point; every setting remains editable.";

    AioaPresetScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Quick Presets"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        int panelWidth = AioaScreenUtil.panelWidth(this.width, 520);
        int x = (this.width - panelWidth) / 2 + 22;
        int width = panelWidth - 44;
        int y = Math.max(86, (this.height - 244) / 2 + 54);
        this.addRenderableWidget(AioaScreenUtil.button(x, y, width, "Balanced Survival", button -> apply(AioaConfig.Preset.BALANCED, "Balanced survival preset applied.")));
        y += 34;
        this.addRenderableWidget(AioaScreenUtil.button(x, y, width, "Cinematic / Manual Spawning", button -> apply(AioaConfig.Preset.CINEMATIC, "Cinematic preset applied; automatic spawns are off.")));
        y += 34;
        this.addRenderableWidget(AioaScreenUtil.button(x, y, width, "High-Intensity Horde", button -> apply(AioaConfig.Preset.HORDE, "Horde preset applied; check performance before recording.")));
        y += 44;
        this.addRenderableWidget(AioaScreenUtil.button(x, y, width, "Done", button -> this.onClose()));
    }

    private void apply(AioaConfig.Preset preset, String message) {
        this.editableConfig.applyPreset(preset);
        this.status = message;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        int panelWidth = AioaScreenUtil.panelWidth(this.width, 520);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(24, (this.height - 244) / 2);
        AioaScreenUtil.drawPanel(guiGraphics, left, top, left + panelWidth, Math.min(this.height - 24, top + 244));
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 14, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal(this.status), this.width / 2, top + 31, panelWidth - 52, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
