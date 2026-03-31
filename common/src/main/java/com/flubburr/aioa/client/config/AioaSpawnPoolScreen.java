package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaSpawnEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class AioaSpawnPoolScreen extends AioaScrollableScreen {
    private static final int COMPACT_PREVIEW_HEIGHT = 96;
    private static final int FULL_PREVIEW_HEIGHT = 124;

    private final Screen parent;
    private final AioaConfig editableConfig;
    private final List<Button> entryButtons = new ArrayList<>();
    private Button addButton;
    private Button editButton;
    private Button removeButton;
    private Button defaultsButton;
    private Button doneButton;
    private Button cancelButton;
    private int selectedIndex = -1;

    AioaSpawnPoolScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Day Spawn Pool"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 76, 68, 120);
        int contentBottom = AioaScreenUtil.adaptiveContentBottom(this.height, this.height - 68, 40, contentTop, 120);
        this.resetScrollLayout(660, contentTop, contentBottom);
        this.entryButtons.clear();
        int width = this.panelWidth - 40;
        int left = this.panelLeft + 20;
        int y = 0;

        for (String ignored : this.editableConfig.daySurfaceSpawns.spawnPoolEntries) {
            Button button = this.addScrollable(AioaScreenUtil.button(left, 0, width, "-", b -> this.selectEntry((Button) b)), y);
            button.visible = false;
            this.entryButtons.add(button);
        }

        this.addButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Add Mob To Pool", b ->
                this.minecraft.setScreen(new AioaEntityPickerScreen(this, "Add Day Spawn Mob", AioaScreenUtil.allEntityIds(), this::addEntryFor))), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.editButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Edit Selected Entry", b -> this.editSelected()), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.removeButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Remove Selected Entry", b -> this.removeSelected()), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.defaultsButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Reset Pool To Defaults", b -> {
            this.editableConfig.daySurfaceSpawns.spawnPoolEntries = new ArrayList<>(AioaConfig.createDefault().daySurfaceSpawns.spawnPoolEntries);
            this.selectedIndex = -1;
            this.minecraft.setScreen(new AioaSpawnPoolScreen(this.parent, this.editableConfig));
        }), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.doneButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Done", b -> this.minecraft.setScreen(this.parent)), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.cancelButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Cancel", b -> this.minecraft.setScreen(this.parent)), y);

        this.refreshEntries();
    }

    private void addEntryFor(ResourceLocation id) {
        this.editableConfig.daySurfaceSpawns.spawnPoolEntries.add(id + ";enabled=true;weight=10;chance=1.0;min=1;max=3");
        this.selectedIndex = this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size() - 1;
        this.minecraft.setScreen(new AioaSpawnPoolScreen(this.parent, this.editableConfig));
    }

    private void selectEntry(Button clicked) {
        int index = this.entryButtons.indexOf(clicked);
        if (index < 0 || index >= this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size()) {
            return;
        }
        this.selectedIndex = index;
        this.refreshEntries();
    }

    private void editSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size()) {
            return;
        }

        String rawEntry = this.editableConfig.daySurfaceSpawns.spawnPoolEntries.get(this.selectedIndex);
        Optional<AioaSpawnEntry> parsed = AioaSpawnEntry.parse(rawEntry, warning -> {
        });
        if (parsed.isEmpty()) {
            this.minecraft.setScreen(new AioaRawEntryEditorScreen(this, rawEntry, updated -> {
                this.editableConfig.daySurfaceSpawns.spawnPoolEntries.set(this.selectedIndex, updated);
                this.minecraft.setScreen(new AioaSpawnPoolScreen(this.parent, this.editableConfig));
            }));
            return;
        }

        this.minecraft.setScreen(new AioaSpawnEntryEditorScreen(this, parsed.get(), updated -> {
            this.editableConfig.daySurfaceSpawns.spawnPoolEntries.set(this.selectedIndex, updated.toConfigLine());
            this.minecraft.setScreen(new AioaSpawnPoolScreen(this.parent, this.editableConfig));
        }));
    }

    private void removeSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size()) {
            return;
        }
        this.editableConfig.daySurfaceSpawns.spawnPoolEntries.remove(this.selectedIndex);
        this.selectedIndex = -1;
        this.minecraft.setScreen(new AioaSpawnPoolScreen(this.parent, this.editableConfig));
    }

    private void refreshEntries() {
        List<String> entries = this.editableConfig.daySurfaceSpawns.spawnPoolEntries;
        int y = this.height < 260 ? 112 : 156;
        for (int i = 0; i < this.entryButtons.size(); i++) {
            Button button = this.entryButtons.get(i);
            if (i >= entries.size()) {
                this.setScrollableShown(button, false);
                continue;
            }

            boolean selected = i == this.selectedIndex;
            this.setScrollableRelativeY(button, y);
            this.setScrollableShown(button, true);
            button.active = true;
            button.setMessage(Component.literal((selected ? "> " : "") + AioaScreenUtil.clip(AioaScreenUtil.summarizeEntry(entries.get(i)), 46)));
            button.setTooltip(Tooltip.create(Component.literal(entries.get(i))));
            y += AioaScreenUtil.BUTTON_HEIGHT + 6;
        }

        this.setScrollableRelativeY(this.addButton, y + 6);
        this.setScrollableRelativeY(this.editButton, y + 38);
        this.setScrollableRelativeY(this.removeButton, y + 70);
        this.setScrollableRelativeY(this.defaultsButton, y + 102);
        this.setScrollableRelativeY(this.doneButton, y + 134);
        this.setScrollableRelativeY(this.cancelButton, y + 166);
        this.editButton.active = this.selectedIndex >= 0 && this.selectedIndex < entries.size();
        this.removeButton.active = this.editButton.active;
        this.finishScrollLayout(y + 198);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        AioaScreenUtil.drawPanel(guiGraphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal("Manage the mobs, weights, chances, and group sizes used for day surface spawns."), this.width / 2, 49, this.panelWidth - 72, AioaScreenUtil.TEXT_SUB);

        AioaScreenUtil.drawClippedContent(guiGraphics, this.panelLeft + 8, this.contentTop, this.panelLeft + this.panelWidth - 20, this.contentBottom, () -> {
            int previewTop = this.contentTop + 4 - this.scrollOffset;
            boolean compact = this.height < 260;
            List<String> entries = this.editableConfig.daySurfaceSpawns.spawnPoolEntries;
            if (this.selectedIndex >= 0 && this.selectedIndex < entries.size()) {
                Optional<AioaSpawnEntry> parsed = AioaSpawnEntry.parse(entries.get(this.selectedIndex), warning -> { });
                if (parsed.isPresent()) {
                    AioaSpawnEntry entry = parsed.get();
                    int previewHeight = compact ? COMPACT_PREVIEW_HEIGHT : FULL_PREVIEW_HEIGHT;
                    AioaScreenUtil.drawMobPreview(
                            guiGraphics,
                            this.font,
                            this.panelLeft + 20,
                            previewTop,
                            this.panelWidth - 40,
                            previewHeight,
                            entry.entityId(),
                            entry.enabled(),
                            List.of(
                                    Component.literal("Weight: " + entry.weight()),
                                    Component.literal("Chance: " + Math.round(entry.chance() * 100.0D) + "%"),
                                    Component.literal("Group size: " + entry.minGroupSize() + " - " + entry.maxGroupSize()),
                                    Component.literal(entry.enabled() ? "Status: this entry is active." : "Status: this entry is disabled.")
                            )
                    );
                }
            } else {
                AioaScreenUtil.drawInsetPanel(guiGraphics, this.panelLeft + 20, previewTop, this.panelLeft + this.panelWidth - 20, previewTop + 96, false);
                guiGraphics.drawCenteredString(this.font, Component.literal("Select a spawn entry to preview it here"), this.width / 2, previewTop + 40, AioaScreenUtil.TEXT_SUB);
            }
            AioaSpawnPoolScreen.super.render(guiGraphics, mouseX, mouseY, partialTick);
        });

        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
