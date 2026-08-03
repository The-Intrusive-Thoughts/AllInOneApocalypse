package com.flubburr.aioa.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class AioaPlayerPickerScreen extends AioaAnimatedScreen {
    private final Screen parent;
    private final Consumer<String> onSelected;
    private final List<String> players = new ArrayList<>();
    private int page;

    AioaPlayerPickerScreen(Screen parent, Consumer<String> onSelected) {
        super(Component.literal("Choose Player"));
        this.parent = parent;
        this.onSelected = onSelected;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.players.clear();
        if (this.minecraft != null && this.minecraft.level != null) {
            this.minecraft.level.players().stream().map(player -> player.getGameProfile().name())
                    .sorted(String.CASE_INSENSITIVE_ORDER).forEach(this.players::add);
        }
        int width = Math.min(440, this.width - 32);
        int left = (this.width - width) / 2;
        int top = Math.max(48, (this.height - 360) / 2);
        int start = this.page * 8;
        for (int i = start; i < Math.min(this.players.size(), start + 8); i++) {
            String name = this.players.get(i);
            this.addRenderableWidget(AioaScreenUtil.button(left + 18, top + 44 + (i - start) * 30, width - 36,
                    name, button -> { this.onSelected.accept(name); this.transitionTo(this.parent); }));
        }
        this.addRenderableWidget(AioaScreenUtil.button(left + 18, top + 292, 92, "< Previous", button -> { this.page = Math.max(0, this.page - 1); this.init(); }));
        this.addRenderableWidget(AioaScreenUtil.button(left + 116, top + 292, 92, "Next >", button -> { if ((this.page + 1) * 8 < this.players.size()) this.page++; this.init(); }));
        this.addRenderableWidget(AioaScreenUtil.button(left + width - 110, top + 292, 92, "Done", button -> this.onClose()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(graphics);
        AioaScreenUtil.drawScreenBackground(graphics, this.width, this.height);
        int width = Math.min(440, this.width - 32);
        int left = (this.width - width) / 2;
        int top = Math.max(48, (this.height - 360) / 2);
        AioaScreenUtil.drawPanel(graphics, left, top, left + width, top + 332);
        graphics.drawString(this.font, "ONLINE PLAYER TARGET", left + 18, top + 16, AioaScreenUtil.TEXT_MAIN);
        if (this.players.isEmpty()) graphics.drawCenteredString(this.font, "No players are visible in this world.", this.width / 2, top + 74, AioaScreenUtil.TEXT_SUB);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.finishUiRender(graphics);
    }

    @Override
    public void onClose() { this.transitionTo(this.parent); }
}
