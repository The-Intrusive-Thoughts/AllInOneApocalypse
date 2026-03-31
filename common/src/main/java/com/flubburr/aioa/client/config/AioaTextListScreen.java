package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class AioaTextListScreen extends Screen {

    private final Screen parent;
    private final String description;
    private final List<String> initialValues;
    private final Consumer<List<String>> saveConsumer;
    private MultiLineEditBox editor;

    private AioaTextListScreen(Screen parent, String title, String description, List<String> initialValues, Consumer<List<String>> saveConsumer) {
        super(Component.literal(title));
        this.parent = parent;
        this.description = description;
        this.initialValues = new ArrayList<>(initialValues);
        this.saveConsumer = saveConsumer;
    }

    static Screen forBiomes(Screen parent, AioaConfig editableConfig) {
        return new AioaTextListScreen(
                parent,
                "Allowed Biomes",
                "Use one biome id per line. Leave this empty to allow every biome.",
                editableConfig.daySurfaceSpawns.allowedBiomeIds,
                values -> editableConfig.daySurfaceSpawns.allowedBiomeIds = new ArrayList<>(values)
        );
    }

    @Override
    protected void init() {
        this.editor = new MultiLineEditBox(
                this.font,
                this.width / 2 - 180,
                62,
                360,
                Math.max(100, this.height - 116),
                Component.literal("Entries"),
                Component.literal("One value per line")
        );
        this.editor.setCharacterLimit(32767);
        this.editor.setValue(String.join("\n", this.initialValues));
        this.addRenderableWidget(this.editor);

        this.addRenderableWidget(AioaScreenUtil.button(this.width / 2 - 154, this.height - 30, 150, "Done", b -> {
            List<String> values = this.editor.getValue().lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
            this.saveConsumer.accept(new ArrayList<>(values));
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(AioaScreenUtil.button(this.width / 2 + 4, this.height - 30, 150, "Cancel", b -> this.minecraft.setScreen(this.parent)));
        this.setInitialFocus(this.editor);
    }

    @Override
    public void tick() {
        this.editor.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 196, 24, this.width / 2 + 196, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal(this.description), this.width / 2, 49, 340, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
