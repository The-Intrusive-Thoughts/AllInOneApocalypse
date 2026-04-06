package com.flubburr.aioa.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class AioaRawEntryEditorScreen extends Screen {

    private final Screen parent;
    private final Consumer<String> saveConsumer;
    private final String initialValue;
    private EditBox rawEntryBox;

    AioaRawEntryEditorScreen(Screen parent, String initialValue, Consumer<String> saveConsumer) {
        super(Component.literal("Edit Raw Entry"));
        this.parent = parent;
        this.initialValue = initialValue;
        this.saveConsumer = saveConsumer;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.rawEntryBox = new EditBox(this.font, centerX - 180, 92, 360, 20, Component.literal("raw entry"));
        this.rawEntryBox.setMaxLength(1024);
        this.rawEntryBox.setValue(this.initialValue);
        this.addRenderableWidget(this.rawEntryBox);
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 154, this.height - 30, 150, "Done", b -> {
            this.saveConsumer.accept(this.rawEntryBox.getValue().trim());
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 4, this.height - 30, 150, "Cancel", b -> this.minecraft.setScreen(this.parent)));
        this.setInitialFocus(this.rawEntryBox);
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AioaScreenUtil.drawBackdrop(guiGraphics, this.width, this.height);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 196, 24, this.width / 2 + 196, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal("This entry could not be parsed, so it is exposed as raw text."), this.width / 2, 58, 340, AioaScreenUtil.TEXT_SUB);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal("Expected: entity_id;enabled=true;weight=10;chance=1.0;min=1;max=3"), this.width / 2, 80, 340, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
