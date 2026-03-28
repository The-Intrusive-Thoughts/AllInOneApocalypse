package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AioaEntityToggleScreen extends Screen {

    private final Screen parent;
    private final String description;
    private final List<ResourceLocation> allOptions;
    private final Set<String> selectedIds;
    private final java.util.function.Consumer<List<String>> saveConsumer;

    private final List<Button> optionButtons = new ArrayList<>();
    private final List<ResourceLocation> visibleOptions = new ArrayList<>();
    private EditBox searchBox;
    private int page;
    private List<ResourceLocation> filteredOptions = List.of();

    private AioaEntityToggleScreen(
            Screen parent,
            String title,
            String description,
            List<ResourceLocation> allOptions,
            Set<String> selectedIds,
            java.util.function.Consumer<List<String>> saveConsumer
    ) {
        super(Component.literal(title));
        this.parent = parent;
        this.description = description;
        this.allOptions = new ArrayList<>(allOptions);
        this.selectedIds = new LinkedHashSet<>(selectedIds);
        this.saveConsumer = saveConsumer;
    }

    static Screen forHostiles(Screen parent, AioaConfig editableConfig) {
        return new AioaEntityToggleScreen(
                parent,
                "Allowed Hostiles",
                "Search by mob name, then click to toggle it in the hostile allow-list.",
                AioaScreenUtil.hostileEntityIds(),
                new LinkedHashSet<>(editableConfig.hostileSpawnControl.whitelistEntityIds),
                values -> editableConfig.hostileSpawnControl.whitelistEntityIds = new ArrayList<>(values)
        );
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.searchBox = new EditBox(this.font, centerX - 170, 52, 340, 20, Component.literal("Search mobs"));
        this.searchBox.setResponder(value -> {
            this.page = 0;
            this.refreshList();
        });
        this.addRenderableWidget(this.searchBox);

        int y = 84;
        for (int i = 0; i < AioaScreenUtil.ROWS_PER_PAGE; i++) {
            int slot = i;
            this.visibleOptions.add(null);
            Button button = AioaScreenUtil.button(centerX - 190, y + (i * 22), 380, "-", b -> this.toggleSlot(slot));
            this.optionButtons.add(button);
            this.addRenderableWidget(button);
        }

        this.addRenderableWidget(AioaScreenUtil.button(centerX - 190, this.height - 58, 120, "Previous Page", b -> {
            if (this.page > 0) {
                this.page--;
                this.refreshList();
            }
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX - 60, this.height - 58, 120, "Clear All", b -> {
            this.selectedIds.clear();
            this.refreshList();
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 70, this.height - 58, 120, "Next Page", b -> {
            if ((this.page + 1) * AioaScreenUtil.ROWS_PER_PAGE < this.filteredOptions.size()) {
                this.page++;
                this.refreshList();
            }
        }));

        this.addRenderableWidget(AioaScreenUtil.button(centerX - 154, this.height - 30, 150, "Done", b -> {
            this.saveConsumer.accept(new ArrayList<>(this.selectedIds));
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(AioaScreenUtil.button(centerX + 4, this.height - 30, 150, "Cancel", b -> this.minecraft.setScreen(this.parent)));

        this.refreshList();
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void tick() {
        this.searchBox.tick();
    }

    private void refreshList() {
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        this.filteredOptions = this.allOptions.stream()
                .filter(id -> query.isBlank() || AioaScreenUtil.entityLine(id).toLowerCase(Locale.ROOT).contains(query))
                .toList();

        int start = this.page * AioaScreenUtil.ROWS_PER_PAGE;
        if (start >= this.filteredOptions.size() && this.page > 0) {
            this.page = Math.max(0, (this.filteredOptions.size() - 1) / AioaScreenUtil.ROWS_PER_PAGE);
            start = this.page * AioaScreenUtil.ROWS_PER_PAGE;
        }

        for (int i = 0; i < this.optionButtons.size(); i++) {
            Button button = this.optionButtons.get(i);
            int index = start + i;
            if (index >= this.filteredOptions.size()) {
                button.visible = false;
                this.visibleOptions.set(i, null);
                continue;
            }

            ResourceLocation id = this.filteredOptions.get(index);
            this.visibleOptions.set(i, id);
            String text = (this.selectedIds.contains(id.toString()) ? "[x] " : "[ ] ") + AioaScreenUtil.clip(AioaScreenUtil.entityLine(id), 54);
            button.visible = true;
            button.setMessage(Component.literal(text));
            button.active = true;
            button.setTooltip(Tooltip.create(Component.literal(AioaScreenUtil.entityLine(id))));
        }
    }

    private void toggleSlot(int slot) {
        if (slot < 0 || slot >= this.visibleOptions.size()) {
            return;
        }

        ResourceLocation id = this.visibleOptions.get(slot);
        if (id == null) {
            return;
        }

        String rawId = id.toString();
        if (this.selectedIds.contains(rawId)) {
            this.selectedIds.remove(rawId);
        } else {
            this.selectedIds.add(rawId);
        }
        this.refreshList();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.width / 2 - 206, 24, this.width / 2 + 206, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        guiGraphics.drawCenteredString(this.font, Component.literal(this.description), this.width / 2, 68, AioaScreenUtil.TEXT_SUB);
        guiGraphics.drawCenteredString(this.font, Component.literal("Selected: " + this.selectedIds.size() + " | Page " + (this.page + 1)), this.width / 2, this.height - 72, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
