package com.flubburr.aioa.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class AioaEntityPickerScreen extends AioaScrollableScreen {
    private static final int PREVIEW_HEIGHT = 126;

    private final Screen parent;
    private final List<ResourceLocation> allOptions;
    private final Consumer<ResourceLocation> selectionConsumer;
    private final String description;
    private final String actionLabel;
    private final List<Button> optionButtons = new ArrayList<>();
    private EditBox searchBox;
    private Button addButton;
    private Button backButton;
    private ResourceLocation selectedOption;
    private String searchQuery = "";
    private List<ResourceLocation> filteredOptions = List.of();

    AioaEntityPickerScreen(Screen parent, String title, List<ResourceLocation> allOptions, Consumer<ResourceLocation> selectionConsumer) {
        this(parent, title, allOptions, "Search the mob list and add a creature to the day spawn pool.", "Add Selected Mob", selectionConsumer);
    }

    AioaEntityPickerScreen(Screen parent, String title, List<ResourceLocation> allOptions, String description, String actionLabel, Consumer<ResourceLocation> selectionConsumer) {
        super(Component.literal(title));
        this.parent = parent;
        this.allOptions = new ArrayList<>(allOptions);
        this.description = description;
        this.actionLabel = actionLabel;
        this.selectionConsumer = selectionConsumer;
    }

    @Override
    protected void init() {
        int contentTop = AioaScreenUtil.adaptiveContentTop(this.height, 76, 68, 120);
        int contentBottom = AioaScreenUtil.adaptiveContentBottom(this.height, this.height - 68, 40, contentTop, 120);
        this.resetScrollLayout(620, contentTop, contentBottom);
        this.optionButtons.clear();
        int width = this.panelWidth - 40;
        int left = this.panelLeft + 20;
        int y = 0;

        this.searchBox = this.addScrollable(new EditBox(this.font, left, 0, width, 20, Component.literal("Search mobs")), y);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(value -> {
            this.searchQuery = value;
            this.refreshList();
        });
        y += 30;

        for (ResourceLocation ignored : this.allOptions) {
            Button button = this.addScrollable(AioaScreenUtil.button(left, 0, width, "-", b -> this.selectButton((Button) b)), y);
            button.visible = false;
            this.optionButtons.add(button);
        }

        this.addButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, this.actionLabel, b -> this.confirmSelection()), y);
        y += AioaScreenUtil.BUTTON_HEIGHT + 8;
        this.backButton = this.addScrollable(AioaScreenUtil.button(left, 0, width, "Back", b -> this.transitionTo(this.parent)), y);

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
        this.filteredOptions = this.allOptions.stream()
                .filter(id -> query.isBlank() || AioaScreenUtil.entityLine(id).toLowerCase(Locale.ROOT).contains(query))
                .toList();

        if (this.selectedOption == null || !this.filteredOptions.contains(this.selectedOption)) {
            this.selectedOption = this.filteredOptions.isEmpty() ? null : this.filteredOptions.get(0);
        }

        int y = 164;
        for (int i = 0; i < this.optionButtons.size(); i++) {
            Button button = this.optionButtons.get(i);
            if (i >= this.filteredOptions.size()) {
                this.setScrollableShown(button, false);
                continue;
            }

            ResourceLocation id = this.filteredOptions.get(i);
            boolean selected = id.equals(this.selectedOption);
            this.setScrollableRelativeY(button, y);
            this.setScrollableShown(button, true);
            button.active = true;
            button.setMessage(Component.literal((selected ? "> " : "") + AioaScreenUtil.clip(AioaScreenUtil.entityDisplayName(id), 28)));
            button.setTooltip(Tooltip.create(Component.literal(AioaScreenUtil.entityLine(id))));
            y += AioaScreenUtil.BUTTON_HEIGHT + 6;
        }

        this.setScrollableRelativeY(this.addButton, y + 6);
        this.setScrollableRelativeY(this.backButton, y + AioaScreenUtil.BUTTON_HEIGHT + 14);
        this.addButton.active = this.selectedOption != null;
        this.finishScrollLayout(y + (AioaScreenUtil.BUTTON_HEIGHT * 2) + 44);
    }

    private void selectButton(Button clicked) {
        int index = this.optionButtons.indexOf(clicked);
        if (index < 0 || index >= this.filteredOptions.size()) {
            return;
        }
        this.selectedOption = this.filteredOptions.get(index);
        this.refreshList();
    }

    private void confirmSelection() {
        if (this.selectedOption == null) {
            return;
        }
        this.selectionConsumer.accept(this.selectedOption);
        this.transitionTo(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(guiGraphics);
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        AioaScreenUtil.drawPanel(guiGraphics, this.panelLeft, 24, this.panelLeft + this.panelWidth, this.height - 40);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 34, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font, Component.literal(this.description), this.width / 2, 49, this.panelWidth - 72, AioaScreenUtil.TEXT_SUB);

        AioaScreenUtil.drawClippedContent(guiGraphics, this.panelLeft + 8, this.contentTop, this.panelLeft + this.panelWidth - 20, this.contentBottom, () -> {
            if (this.selectedOption != null) {
                int previewTop = this.contentTop + 34 - this.scrollOffset;
                AioaScreenUtil.drawMobPreview(
                        guiGraphics,
                        this.font,
                        this.panelLeft + 20,
                        previewTop,
                        this.panelWidth - 40,
                        PREVIEW_HEIGHT,
                        this.selectedOption,
                        true,
                        List.of(
                                Component.literal("Status: ready to add to the spawn pool"),
                                Component.literal("ID: " + this.selectedOption)
                        )
                );
            }
            AioaEntityPickerScreen.super.render(guiGraphics, mouseX, mouseY, partialTick);
        });

        AioaScreenUtil.drawScrollBar(guiGraphics, this.panelLeft + this.panelWidth - 14, this.contentTop, this.contentBottom - this.contentTop, this.scrollOffset, this.maxScroll);
        this.finishUiRender(guiGraphics);
    }

    @Override
    public void onClose() {
        this.transitionTo(this.parent);
    }
}
