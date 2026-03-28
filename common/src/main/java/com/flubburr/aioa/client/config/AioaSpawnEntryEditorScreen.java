package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaSpawnEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

final class AioaSpawnEntryEditorScreen extends Screen {

    private final Screen parent;
    private final ResourceLocation entityId;
    private final Consumer<AioaSpawnEntry> saveConsumer;

    private boolean enabled;
    private int cachedWeight;
    private double cachedChance;
    private int cachedMin;
    private int cachedMax;
    private EditBox weightBox;
    private EditBox chanceBox;
    private EditBox minBox;
    private EditBox maxBox;

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
        int centerX = this.width / 2;
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 170, 68, 340, AioaScreenUtil.boolLabel("Entry enabled", this.enabled), b -> {
            this.enabled = !this.enabled;
            b.setMessage(Component.literal(AioaScreenUtil.boolLabel("Entry enabled", this.enabled)));
        }));

        int inputX = centerX + 28;
        this.weightBox = AioaScreenUtil.numberBox(inputX, 110, 120, this.cachedWeight);
        this.chanceBox = AioaScreenUtil.decimalBox(inputX, 134, 120, this.cachedChance);
        this.minBox = AioaScreenUtil.numberBox(inputX, 158, 120, this.cachedMin);
        this.maxBox = AioaScreenUtil.numberBox(inputX, 182, 120, this.cachedMax);
        this.addRenderableWidget(this.weightBox);
        this.addRenderableWidget(this.chanceBox);
        this.addRenderableWidget(this.minBox);
        this.addRenderableWidget(this.maxBox);

        this.addRenderableWidget(AioaScreenUtil.button(centerX - 154, this.height - 30, 150, "Done", b -> {
            int weight = Math.max(1, AioaScreenUtil.readNumber(this.weightBox, this.cachedWeight));
            double chance = AioaScreenUtil.clampChance(AioaScreenUtil.readDecimal(this.chanceBox, this.cachedChance));
            int min = Math.max(1, AioaScreenUtil.readNumber(this.minBox, this.cachedMin));
            int max = Math.max(min, AioaScreenUtil.readNumber(this.maxBox, this.cachedMax));
            this.saveConsumer.accept(new AioaSpawnEntry(
                    this.entityId + ";enabled=" + this.enabled + ";weight=" + weight + ";chance=" + chance + ";min=" + min + ";max=" + max,
                    this.entityId,
                    this.enabled,
                    weight,
                    chance,
                    min,
                    max
            ));
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 4, this.height - 30, 150, "Cancel", b -> this.minecraft.setScreen(this.parent)));
        this.setInitialFocus(this.weightBox);
    }

    @Override
    public void tick() {
        this.weightBox.tick();
        this.chanceBox.tick();
        this.minBox.tick();
        this.maxBox.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 196, 24, this.width / 2 + 196, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        guiGraphics.drawCenteredString(this.font, Component.literal(AioaScreenUtil.entityLine(this.entityId)), this.width / 2, 49, AioaScreenUtil.TEXT_SUB);
        int labelX = this.width / 2 - 148;
        guiGraphics.drawString(this.font, Component.literal("Weight"), labelX, 116, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, Component.literal("Chance (0.0 - 1.0)"), labelX, 140, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, Component.literal("Min group size"), labelX, 164, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawString(this.font, Component.literal("Max group size"), labelX, 188, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
