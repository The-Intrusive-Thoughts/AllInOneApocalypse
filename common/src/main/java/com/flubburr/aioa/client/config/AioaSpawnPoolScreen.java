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

final class AioaSpawnPoolScreen extends Screen {

    private final Screen parent;
    private final AioaConfig editableConfig;
    private final List<Button> entryButtons = new ArrayList<>();
    private int page;
    private int selectedIndex = -1;

    AioaSpawnPoolScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Day Spawn Pool"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 58;
        for (int i = 0; i < AioaScreenUtil.ROWS_PER_PAGE; i++) {
            int localIndex = i;
            Button button = AioaScreenUtil.button(centerX - 200, y + (i * 22), 400, "-", b -> this.selectEntry((this.page * AioaScreenUtil.ROWS_PER_PAGE) + localIndex));
            this.entryButtons.add(button);
            this.addRenderableWidget(button);
        }

        this.addRenderableWidget(AioaScreenUtil.button(centerX - 200, this.height - 82, 126, "Add Mob", b ->
                this.minecraft.setScreen(new AioaEntityPickerScreen(this, "Add Day Spawn Mob", AioaScreenUtil.allEntityIds(), this::addEntryFor))));
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 66, this.height - 82, 126, "Edit Entry", b -> this.editSelected()));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 68, this.height - 82, 126, "Remove Entry", b -> this.removeSelected()));

        this.addRenderableWidget(AioaScreenUtil.button(centerX - 200, this.height - 58, 126, "Previous Page", b -> {
            if (this.page > 0) {
                this.page--;
                this.refreshEntries();
            }
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 66, this.height - 58, 126, "Next Page", b -> {
            if ((this.page + 1) * AioaScreenUtil.ROWS_PER_PAGE < this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size()) {
                this.page++;
                this.refreshEntries();
            }
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 68, this.height - 58, 126, "Copy Defaults", b -> {
            this.editableConfig.daySurfaceSpawns.spawnPoolEntries = new ArrayList<>(AioaConfig.createDefault().daySurfaceSpawns.spawnPoolEntries);
            this.selectedIndex = -1;
            this.page = 0;
            this.refreshEntries();
        }));

        this.addRenderableWidget(AioaScreenUtil.button(centerX - 154, this.height - 30, 150, "Done", b -> this.minecraft.setScreen(this.parent)));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 4, this.height - 30, 150, "Cancel", b -> this.minecraft.setScreen(this.parent)));
        this.refreshEntries();
    }

    private void addEntryFor(ResourceLocation id) {
        this.editableConfig.daySurfaceSpawns.spawnPoolEntries.add(id + ";enabled=true;weight=10;chance=1.0;min=1;max=3");
        this.selectedIndex = this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size() - 1;
        this.page = this.selectedIndex / AioaScreenUtil.ROWS_PER_PAGE;
        this.refreshEntries();
    }

    private void selectEntry(int index) {
        this.selectedIndex = index >= 0 && index < this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size() ? index : -1;
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
                this.refreshEntries();
            }));
            return;
        }

        this.minecraft.setScreen(new AioaSpawnEntryEditorScreen(this, parsed.get(), updated -> {
            this.editableConfig.daySurfaceSpawns.spawnPoolEntries.set(this.selectedIndex, updated.toConfigLine());
            this.refreshEntries();
        }));
    }

    private void removeSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size()) {
            return;
        }
        this.editableConfig.daySurfaceSpawns.spawnPoolEntries.remove(this.selectedIndex);
        this.selectedIndex = -1;
        this.refreshEntries();
    }

    private void refreshEntries() {
        List<String> entries = this.editableConfig.daySurfaceSpawns.spawnPoolEntries;
        int start = this.page * AioaScreenUtil.ROWS_PER_PAGE;
        if (start >= entries.size() && this.page > 0) {
            this.page = Math.max(0, (entries.size() - 1) / AioaScreenUtil.ROWS_PER_PAGE);
            start = this.page * AioaScreenUtil.ROWS_PER_PAGE;
        }

        for (int i = 0; i < this.entryButtons.size(); i++) {
            Button button = this.entryButtons.get(i);
            int index = start + i;
            if (index >= entries.size()) {
                button.visible = false;
                continue;
            }

            String label = (index == this.selectedIndex ? "> " : "  ") + AioaScreenUtil.clip(AioaScreenUtil.summarizeEntry(entries.get(index)), 60);
            button.visible = true;
            button.setMessage(Component.literal(label));
            button.active = true;
            button.setTooltip(Tooltip.create(Component.literal(entries.get(index))));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 216, 24, this.width / 2 + 216, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        guiGraphics.drawCenteredString(this.font, Component.literal("Pick mobs by name, then edit weight, chance, and group sizes without hand-writing ids."), this.width / 2, 49, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawCenteredString(this.font, Component.literal("Entries: " + this.editableConfig.daySurfaceSpawns.spawnPoolEntries.size() + " | Page " + (this.page + 1)), this.width / 2, this.height - 96, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
