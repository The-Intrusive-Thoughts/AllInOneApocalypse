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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class AioaBiomeToggleScreen extends AioaScrollableScreen {

    private final Screen parent;
    private final AioaConfig editableConfig;
    private final Set<String> selectedBiomeIds;
    private final List<ResourceLocation> allBiomes;

    private final List<Button> optionButtons = new ArrayList<>();
    private final List<ResourceLocation> visibleButtonBiomes = new ArrayList<>();
    private EditBox searchBox;
    private Button selectAllButton;
    private Button clearButton;
    private Button doneButton;
    private Button cancelButton;
    private Button overworldHeader;
    private Button netherHeader;
    private Button endHeader;
    private Button moddedHeader;
    private ResourceLocation focusedBiome;
    private String searchQuery = "";
    private List<ResourceLocation> filteredBiomes = List.of();
    private boolean overworldExpanded;
    private boolean netherExpanded;
    private boolean endExpanded;
    private boolean moddedExpanded;

    private AioaBiomeToggleScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("Allowed Biomes"));
        this.parent = parent;
        this.editableConfig = editableConfig;
        this.selectedBiomeIds = new LinkedHashSet<>(editableConfig.daySurfaceSpawns.allowedBiomeIds);
        this.allBiomes = AioaScreenUtil.allBiomeIds();
    }

    static Screen create(Screen parent, AioaConfig editableConfig) {
        return new AioaBiomeToggleScreen(parent, editableConfig);
    }

    @Override
    protected void init() {
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 76, 68, 120);
        int contentBottom = AioaScreenUtil.adaptiveContentBottom(this.height, this.height - 68, 40, contentTop, 120);
        this.resetScrollLayout(680, contentTop, contentBottom);
        this.optionButtons.clear();
        this.visibleButtonBiomes.clear();
        int width = this.panelWidth - 40;
        int left = this.panelLeft + 20;
        int y = 0;

        this.searchBox = this.addScrollable(AioaScreenUtil.searchBox(left, 0, width, "Search biomes"), y);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(value -> {
            this.searchQuery = value;
            this.refreshList();
        });
        y += AioaScreenUtil.BUTTON_HEIGHT + 10;

        this.overworldHeader = this.addScrollable(AioaScreenUtil.button(left, 0, width, AioaScreenUtil.sectionLabel("Overworld biomes", this.overworldExpanded), b -> {
            this.overworldExpanded = !this.overworldExpanded;
            this.refreshList();
        }), y);
        this.netherHeader = this.addScrollable(AioaScreenUtil.button(left, 0, width, AioaScreenUtil.sectionLabel("Nether biomes", this.netherExpanded), b -> {
            this.netherExpanded = !this.netherExpanded;
            this.refreshList();
        }), y);
        this.endHeader = this.addScrollable(AioaScreenUtil.button(left, 0, width, AioaScreenUtil.sectionLabel("End biomes", this.endExpanded), b -> {
            this.endExpanded = !this.endExpanded;
            this.refreshList();
        }), y);
        this.moddedHeader = this.addScrollable(AioaScreenUtil.button(left, 0, width, AioaScreenUtil.sectionLabel("Modded biomes", this.moddedExpanded), b -> {
            this.moddedExpanded = !this.moddedExpanded;
            this.refreshList();
        }), y);

        for (ResourceLocation ignored : this.allBiomes) {
            Button button = this.addScrollable(AioaScreenUtil.button(left, 0, width, "-", b -> this.toggleButton((Button) b)), y);
            this.setScrollableShown(button, false);
            this.optionButtons.add(button);
            this.visibleButtonBiomes.add(null);
        }

        this.selectAllButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Select All Visible", b -> {
            for (ResourceLocation biome : this.visibleButtonBiomes) {
                if (biome != null) {
                    this.selectedBiomeIds.add(biome.toString());
                }
            }
            this.refreshList();
        }), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.clearButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Clear Visible", b -> {
            for (ResourceLocation biome : this.visibleButtonBiomes) {
                if (biome != null) {
                    this.selectedBiomeIds.remove(biome.toString());
                }
            }
            this.refreshList();
        }), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.doneButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Done", b -> {
            this.editableConfig.daySurfaceSpawns.allowedBiomeIds = new ArrayList<>(this.selectedBiomeIds);
            this.minecraft.setScreen(this.parent);
        }), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.cancelButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Cancel", b -> this.minecraft.setScreen(this.parent)), y);

        this.refreshList();
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void tick() {
        if (this.searchBox != null) {
            this.searchBox.tick();
        }
    }

    private void refreshList() {
        String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        this.filteredBiomes = this.allBiomes.stream()
                .filter(id -> query.isBlank() || AioaScreenUtil.biomeLine(id).toLowerCase(Locale.ROOT).contains(query))
                .toList();

        if (this.focusedBiome == null || !this.filteredBiomes.contains(this.focusedBiome)) {
            this.focusedBiome = this.filteredBiomes.isEmpty() ? null : this.filteredBiomes.get(0);
        }

        Map<String, List<ResourceLocation>> groups = new HashMap<>();
        groups.put("Overworld", new ArrayList<>());
        groups.put("Nether", new ArrayList<>());
        groups.put("End", new ArrayList<>());
        groups.put("Modded", new ArrayList<>());
        for (ResourceLocation biome : this.filteredBiomes) {
            groups.computeIfAbsent(AioaScreenUtil.biomeCategory(biome), key -> new ArrayList<>()).add(biome);
        }

        int y = this.height < 260 ? 96 : 146;
        int slot = 0;
        y = this.layoutGroup(this.overworldHeader, "Overworld biomes", this.overworldExpanded, groups.get("Overworld"), y, slot);
        slot += this.overworldExpanded ? groups.get("Overworld").size() : 0;
        y = this.layoutGroup(this.netherHeader, "Nether biomes", this.netherExpanded, groups.get("Nether"), y, slot);
        slot += this.netherExpanded ? groups.get("Nether").size() : 0;
        y = this.layoutGroup(this.endHeader, "End biomes", this.endExpanded, groups.get("End"), y, slot);
        slot += this.endExpanded ? groups.get("End").size() : 0;
        y = this.layoutGroup(this.moddedHeader, "Modded biomes", this.moddedExpanded, groups.get("Modded"), y, slot);
        slot += this.moddedExpanded ? groups.get("Modded").size() : 0;

        for (int i = slot; i < this.optionButtons.size(); i++) {
            this.visibleButtonBiomes.set(i, null);
            this.setScrollableShown(this.optionButtons.get(i), false);
        }

        this.setScrollableRelativeY(this.selectAllButton, y + 6);
        this.setScrollableRelativeY(this.clearButton, y + 38);
        this.setScrollableRelativeY(this.doneButton, y + 70);
        this.setScrollableRelativeY(this.cancelButton, y + 102);
        boolean hasVisibleBiomes = this.visibleButtonBiomes.stream().anyMatch(biome -> biome != null);
        this.selectAllButton.active = hasVisibleBiomes;
        this.clearButton.active = hasVisibleBiomes;
        this.finishScrollLayout(y + 134);
    }

    private int layoutGroup(Button header, String label, boolean expanded, List<ResourceLocation> entries, int y, int slotStart) {
        this.setScrollableRelativeY(header, y);
        this.setScrollableShown(header, true);
        header.setMessage(Component.literal(AioaScreenUtil.sectionLabel(label + " (" + entries.size() + ")", expanded)));
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        if (!expanded) {
            return y;
        }
        int slot = slotStart;
        for (ResourceLocation biome : entries) {
            y = this.layoutBiomeButton(this.optionButtons.get(slot), slot, biome, y);
            slot++;
        }
        return y + 4;
    }

    private int layoutBiomeButton(Button button, int slot, ResourceLocation biome, int y) {
        boolean selected = this.selectedBiomeIds.contains(biome.toString());
        boolean focused = biome.equals(this.focusedBiome);
        this.visibleButtonBiomes.set(slot, biome);
        this.setScrollableRelativeY(button, y);
        this.setScrollableShown(button, true);
        button.active = true;
        button.setMessage(Component.literal((selected ? "Allowed: " : "Blocked: ") + AioaScreenUtil.clip(AioaScreenUtil.biomeDisplayName(biome), 24)));
        button.setTooltip(Tooltip.create(Component.literal(AioaScreenUtil.biomeLine(biome) + " | " + AioaScreenUtil.biomeCategory(biome))));
        if (focused) {
            button.setTooltip(Tooltip.create(Component.literal(AioaScreenUtil.biomeLine(biome) + " | focused")));
        }
        return y + AioaScreenUtil.BUTTON_HEIGHT + 10;
    }

    private void toggleButton(Button clicked) {
        int index = this.optionButtons.indexOf(clicked);
        if (index < 0 || index >= this.visibleButtonBiomes.size()) {
            return;
        }
        ResourceLocation biome = this.visibleButtonBiomes.get(index);
        if (biome == null) {
            return;
        }
        this.focusedBiome = biome;
        String rawId = biome.toString();
        if (this.selectedBiomeIds.contains(rawId)) {
            this.selectedBiomeIds.remove(rawId);
        } else {
            this.selectedBiomeIds.add(rawId);
        }
        this.refreshList();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        AioaScreenUtil.drawPanel(guiGraphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal("Choose which biomes can use day surface spawns. If none are selected, all biomes are allowed."), this.width / 2, 49, this.panelWidth - 72, AioaScreenUtil.TEXT_SUB);

        AioaScreenUtil.drawClippedContent(guiGraphics, this.panelLeft + 8, this.contentTop, this.panelLeft + this.panelWidth - 20, this.contentBottom, () -> {
            if (this.focusedBiome != null) {
                int previewHeight = this.height < 260 ? 64 : 108;
                int previewTop = this.contentTop + 10 - this.scrollOffset;
                boolean selected = this.selectedBiomeIds.contains(this.focusedBiome.toString());
                AioaScreenUtil.drawInsetPanel(guiGraphics, this.panelLeft + 20, previewTop, this.panelLeft + this.panelWidth - 20, previewTop + previewHeight, selected);
                guiGraphics.drawString(this.font, Component.literal(AioaScreenUtil.biomeDisplayName(this.focusedBiome)), this.panelLeft + 32, previewTop + 14, AioaScreenUtil.TEXT_MAIN);
                guiGraphics.drawString(this.font, Component.literal("Category: " + AioaScreenUtil.biomeCategory(this.focusedBiome)), this.panelLeft + 32, previewTop + 30, AioaScreenUtil.TEXT_SUB);
                if (previewHeight > 80) {
                    guiGraphics.drawString(this.font, Component.literal(selected ? "Status: allowed for day surface spawns" : "Status: not currently allowed"), this.panelLeft + 32, previewTop + 50, AioaScreenUtil.TEXT_MAIN);
                    guiGraphics.drawString(this.font, Component.literal("Biome id"), this.panelLeft + 32, previewTop + 70, AioaScreenUtil.TEXT_SUB);
                    guiGraphics.drawString(this.font, Component.literal(AioaScreenUtil.clip(this.focusedBiome.toString(), 56)), this.panelLeft + 32, previewTop + 84, AioaScreenUtil.TEXT_MAIN);
                }
            }
            AioaBiomeToggleScreen.super.render(guiGraphics, mouseX, mouseY, partialTick);
        });

        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
