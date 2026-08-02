package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class AioaEntityToggleScreen extends AioaScrollableScreen {
    private static final int PREVIEW_TOP_OFFSET = 36;
    private static final int COMPACT_PREVIEW_HEIGHT = 112;
    private static final int FULL_PREVIEW_HEIGHT = 146;
    private static final int PREVIEW_BOTTOM_GAP = 12;

    private final Screen parent;
    private final String description;
    private final List<ResourceLocation> allOptions;
    private final Set<String> selectedIds;
    private final java.util.function.Consumer<List<String>> saveConsumer;
    private final boolean groupedByDimension;

    private final List<Button> optionButtons = new ArrayList<>();
    private final List<ResourceLocation> visibleButtonOptions = new ArrayList<>();
    private EditBox searchBox;
    private Button selectAllButton;
    private Button clearButton;
    private Button doneButton;
    private Button overworldHeader;
    private Button netherHeader;
    private Button endHeader;
    private Button moddedHeader;
    private ResourceLocation focusedOption;
    private String searchQuery = "";
    private List<ResourceLocation> filteredOptions = List.of();
    private boolean overworldExpanded;
    private boolean netherExpanded;
    private boolean endExpanded;
    private boolean moddedExpanded;

    private AioaEntityToggleScreen(
            Screen parent,
            String title,
            String description,
            List<ResourceLocation> allOptions,
            Set<String> selectedIds,
            java.util.function.Consumer<List<String>> saveConsumer,
            boolean groupedByDimension
    ) {
        super(Component.literal(title));
        this.parent = parent;
        this.description = description;
        this.allOptions = new ArrayList<>(allOptions);
        this.selectedIds = new LinkedHashSet<>(selectedIds);
        this.saveConsumer = saveConsumer;
        this.groupedByDimension = groupedByDimension;
    }

    static Screen forHostiles(Screen parent, AioaConfig editableConfig) {
        return new AioaEntityToggleScreen(
                parent,
                "Allowed Hostiles",
                "Choose which hostile mobs stay allowed after the hostile spawn rules run.",
                AioaScreenUtil.hostileEntityIds(),
                new LinkedHashSet<>(editableConfig.hostileSpawnControl.whitelistEntityIds),
                values -> editableConfig.hostileSpawnControl.whitelistEntityIds = new ArrayList<>(values),
                true
        );
    }

    static Screen forAllEntities(
            Screen parent,
            String title,
            String description,
            List<String> selectedIds,
            java.util.function.Consumer<List<String>> saveConsumer
    ) {
        return new AioaEntityToggleScreen(
                parent,
                title,
                description,
                AioaScreenUtil.allEntityIds(),
                new LinkedHashSet<>(selectedIds),
                saveConsumer,
                false
        );
    }

    @Override
    protected void init() {
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 76, 68, 120);
        int contentBottom = AioaScreenUtil.adaptiveContentBottom(this.height, this.height - 68, 40, contentTop, 120);
        this.resetScrollLayout(680, contentTop, contentBottom);
        this.optionButtons.clear();
        this.visibleButtonOptions.clear();
        int width = this.panelWidth - 40;
        int left = this.panelLeft + 20;
        int y = 0;

        this.searchBox = this.addScrollable(AioaScreenUtil.searchBox(left, 0, width, "Search mobs"), y);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(value -> {
            this.searchQuery = value;
            this.refreshList();
        });
        y += AioaScreenUtil.BUTTON_HEIGHT + 10;

        if (this.groupedByDimension) {
            this.overworldHeader = this.addScrollable(AioaScreenUtil.button(left, 0, width, AioaScreenUtil.sectionLabel("Overworld hostiles", this.overworldExpanded), b -> {
                this.overworldExpanded = !this.overworldExpanded;
                this.refreshList();
            }), y);
            this.netherHeader = this.addScrollable(AioaScreenUtil.button(left, 0, width, AioaScreenUtil.sectionLabel("Nether hostiles", this.netherExpanded), b -> {
                this.netherExpanded = !this.netherExpanded;
                this.refreshList();
            }), y);
            this.endHeader = this.addScrollable(AioaScreenUtil.button(left, 0, width, AioaScreenUtil.sectionLabel("End hostiles", this.endExpanded), b -> {
                this.endExpanded = !this.endExpanded;
                this.refreshList();
            }), y);
            this.moddedHeader = this.addScrollable(AioaScreenUtil.button(left, 0, width, AioaScreenUtil.sectionLabel("Modded hostiles", this.moddedExpanded), b -> {
                this.moddedExpanded = !this.moddedExpanded;
                this.refreshList();
            }), y);
        }

        for (ResourceLocation ignored : this.allOptions) {
            Button button = this.addScrollable(AioaScreenUtil.button(left, 0, width, "-", b -> this.toggleButton((Button) b)), y);
            button.visible = false;
            this.optionButtons.add(button);
            this.visibleButtonOptions.add(null);
        }

        this.selectAllButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Select All Visible", b -> {
            for (ResourceLocation option : this.visibleButtonOptions) {
                if (option != null) {
                    this.selectedIds.add(option.toString());
                }
            }
            this.refreshList();
        }), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.clearButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Clear Visible", b -> {
            for (ResourceLocation option : this.visibleButtonOptions) {
                if (option != null) {
                    this.selectedIds.remove(option.toString());
                }
            }
            this.refreshList();
        }), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.doneButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Done", b -> {
            this.saveConsumer.accept(new ArrayList<>(this.selectedIds));
            this.closeToParent();
        }), y);

        this.refreshList();
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void tick() {
    }

    private void refreshList() {
        String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        this.filteredOptions = this.allOptions.stream()
                .filter(id -> query.isBlank() || AioaScreenUtil.entityLine(id).toLowerCase(Locale.ROOT).contains(query))
                .toList();

        if (this.focusedOption == null || !this.filteredOptions.contains(this.focusedOption)) {
            this.focusedOption = this.filteredOptions.isEmpty() ? null : this.filteredOptions.get(0);
        }

        int y = this.previewReservedHeight();
        int slot = 0;
        if (this.groupedByDimension) {
            Map<String, List<ResourceLocation>> groups = new HashMap<>();
            groups.put("Overworld", new ArrayList<>());
            groups.put("Nether", new ArrayList<>());
            groups.put("End", new ArrayList<>());
            groups.put("Modded", new ArrayList<>());
            for (ResourceLocation id : this.filteredOptions) {
                groups.computeIfAbsent(AioaScreenUtil.dimensionCategory(id), key -> new ArrayList<>()).add(id);
            }
            y = this.layoutGroup(this.overworldHeader, "Overworld hostiles", this.overworldExpanded, groups.get("Overworld"), y, slot);
            slot += this.overworldExpanded ? groups.get("Overworld").size() : 0;
            y = this.layoutGroup(this.netherHeader, "Nether hostiles", this.netherExpanded, groups.get("Nether"), y, slot);
            slot += this.netherExpanded ? groups.get("Nether").size() : 0;
            y = this.layoutGroup(this.endHeader, "End hostiles", this.endExpanded, groups.get("End"), y, slot);
            slot += this.endExpanded ? groups.get("End").size() : 0;
            y = this.layoutGroup(this.moddedHeader, "Modded hostiles", this.moddedExpanded, groups.get("Modded"), y, slot);
            slot += this.moddedExpanded ? groups.get("Modded").size() : 0;
        } else {
            for (ResourceLocation id : this.filteredOptions) {
                y = this.layoutOptionButton(this.optionButtons.get(slot), slot, id, y);
                slot++;
            }
        }

        for (int i = slot; i < this.optionButtons.size(); i++) {
            this.visibleButtonOptions.set(i, null);
            this.setScrollableShown(this.optionButtons.get(i), false);
        }

        this.setScrollableRelativeY(this.selectAllButton, y + 6);
        this.setScrollableRelativeY(this.clearButton, y + 38);
        this.setScrollableRelativeY(this.doneButton, y + 70);
        boolean hasVisibleOptions = this.visibleButtonOptions.stream().anyMatch(option -> option != null);
        this.selectAllButton.active = hasVisibleOptions;
        this.clearButton.active = hasVisibleOptions;
        this.finishScrollLayout(y + 102);
    }

    private int layoutGroup(Button header, String label, boolean expanded, List<ResourceLocation> entries, int y, int slotStart) {
        if (header == null) {
            return y;
        }
        this.setScrollableRelativeY(header, y);
        this.setScrollableShown(header, true);
        header.setMessage(Component.literal(AioaScreenUtil.sectionLabel(label + " (" + entries.size() + ")", expanded)));
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        if (!expanded) {
            return y;
        }
        int slot = slotStart;
        for (ResourceLocation id : entries) {
            y = this.layoutOptionButton(this.optionButtons.get(slot), slot, id, y);
            slot++;
        }
        return y + 4;
    }

    private int layoutOptionButton(Button button, int slot, ResourceLocation id, int y) {
        boolean selected = this.selectedIds.contains(id.toString());
        boolean focused = id.equals(this.focusedOption);
        this.visibleButtonOptions.set(slot, id);
        this.setScrollableRelativeY(button, y);
        this.setScrollableShown(button, true);
        button.active = true;
        button.setMessage(Component.literal((selected ? "Included: " : "Available: ") + AioaScreenUtil.clip(AioaScreenUtil.entityDisplayName(id), 22)));
        button.setTooltip(Tooltip.create(Component.literal(AioaScreenUtil.entityLine(id) + " | " + AioaScreenUtil.dimensionCategory(id))));
        if (focused) {
            button.setTooltip(Tooltip.create(Component.literal(AioaScreenUtil.entityLine(id) + " | focused")));
        }
        return y + AioaScreenUtil.BUTTON_HEIGHT + 10;
    }

    private void toggleButton(Button clicked) {
        int index = this.optionButtons.indexOf(clicked);
        if (index < 0 || index >= this.visibleButtonOptions.size()) {
            return;
        }

        ResourceLocation id = this.visibleButtonOptions.get(index);
        if (id == null) {
            return;
        }
        this.focusedOption = id;
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
        this.beginUiRender(guiGraphics);
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        AioaScreenUtil.drawPanel(guiGraphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal(this.description), this.width / 2, 49, this.panelWidth - 72, AioaScreenUtil.TEXT_SUB);

        AioaScreenUtil.drawClippedContent(guiGraphics, this.panelLeft + 8, this.contentTop, this.panelLeft + this.panelWidth - 20, this.contentBottom, () -> {
            if (this.focusedOption != null) {
                int previewTop = this.contentTop + PREVIEW_TOP_OFFSET - this.scrollOffset;
                boolean selected = this.selectedIds.contains(this.focusedOption.toString());
                AioaScreenUtil.drawMobPreview(
                        guiGraphics,
                        this.font,
                        this.panelLeft + 20,
                        previewTop,
                        this.panelWidth - 40,
                        this.previewHeight(),
                        this.focusedOption,
                        selected,
                        this.previewDetailLines(selected)
                );
            }
            AioaEntityToggleScreen.super.render(guiGraphics, mouseX, mouseY, partialTick);
        });

        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
        this.finishUiRender(guiGraphics);
    }

    @Override
    public void onClose() {
        this.closeToParent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.closeToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int previewHeight() {
        return this.height < 260 ? COMPACT_PREVIEW_HEIGHT : FULL_PREVIEW_HEIGHT;
    }

    private int previewReservedHeight() {
        return PREVIEW_TOP_OFFSET + this.previewHeight() + PREVIEW_BOTTOM_GAP;
    }

    private List<Component> previewDetailLines(boolean selected) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Selected: " + this.selectedIds.size()));
        lines.add(Component.literal("Type: " + AioaScreenUtil.categoryLabel(this.focusedOption)));
        lines.add(Component.literal("ID: " + this.focusedOption));
        lines.add(Component.literal(selected ? "Status: included in the hostile allow-list" : "Status: currently excluded"));
        return lines;
    }

    private void closeToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
