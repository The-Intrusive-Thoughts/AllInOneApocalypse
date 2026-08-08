package com.flubburr.aioa.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Searchable icon grid used by equipment parameters; registry ids stay an implementation detail. */
final class AioaItemPickerScreen extends AioaAnimatedScreen {
    private static final int CELL = 44;
    private static final int COLUMNS = 10;
    private static final int ROWS = 5;
    private static final int PAGE_SIZE = COLUMNS * ROWS;

    private final Screen parent;
    private final Consumer<Identifier> selectionConsumer;
    private final List<Identifier> allItems = new ArrayList<>();
    private List<Identifier> filteredItems = List.of();
    private EditBox search;
    private Button useButton;
    private int page;
    private Identifier selected;
    private int gridLeft;
    private int gridTop;

    AioaItemPickerScreen(Screen parent, Identifier initial, Consumer<Identifier> selectionConsumer) {
        super(Component.literal("Choose equipment item"));
        this.parent = parent;
        this.selectionConsumer = selectionConsumer;
        this.selected = initial;
        BuiltInRegistries.ITEM.keySet().stream()
                .sorted(Comparator.comparing(id -> itemName(id).toLowerCase(Locale.ROOT)))
                .forEach(this.allItems::add);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(500, this.width - 24);
        this.gridLeft = (this.width - COLUMNS * CELL) / 2;
        this.gridTop = Math.max(70, (this.height - ROWS * CELL) / 2 - 8);
        int left = (this.width - panelWidth) / 2;
        this.search = this.addRenderableWidget(new EditBox(this.font, left + 14, 42, panelWidth - 28, 22,
                Component.literal("Search items")));
        this.search.setHint(Component.literal("Search by item name"));
        this.search.setResponder(value -> refresh(value));

        int footerY = Math.min(this.height - 34, this.gridTop + ROWS * CELL + 12);
        this.addRenderableWidget(AioaScreenUtil.button(left + 14, footerY, 70, "< Page", button -> {
            this.page = Math.max(0, this.page - 1);
        }));
        this.addRenderableWidget(AioaScreenUtil.button(left + 90, footerY, 70, "Page >", button -> {
            if ((this.page + 1) * PAGE_SIZE < this.filteredItems.size()) this.page++;
        }));
        this.useButton = this.addRenderableWidget(AioaScreenUtil.button(left + panelWidth - 174, footerY, 80,
                "Use item", button -> confirm()));
        this.addRenderableWidget(AioaScreenUtil.button(left + panelWidth - 88, footerY, 74,
                "Back", button -> transitionTo(this.parent)));
        refresh("");
        this.setInitialFocus(this.search);
    }

    private void refresh(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        this.filteredItems = this.allItems.stream().filter(id -> normalized.isBlank()
                || itemName(id).toLowerCase(Locale.ROOT).contains(normalized)
                || id.toString().toLowerCase(Locale.ROOT).contains(normalized)).toList();
        this.page = Math.min(this.page, Math.max(0, (this.filteredItems.size() - 1) / PAGE_SIZE));
        if (this.selected == null && !this.filteredItems.isEmpty()) this.selected = this.filteredItems.get(0);
        if (this.useButton != null) this.useButton.active = this.selected != null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= this.gridLeft && mouseX < this.gridLeft + COLUMNS * CELL
                && mouseY >= this.gridTop && mouseY < this.gridTop + ROWS * CELL) {
            int column = ((int) mouseX - this.gridLeft) / CELL;
            int row = ((int) mouseY - this.gridTop) / CELL;
            int index = this.page * PAGE_SIZE + row * COLUMNS + column;
            if (index >= 0 && index < this.filteredItems.size()) {
                Identifier clicked = this.filteredItems.get(index);
                if (clicked.equals(this.selected)) {
                    confirm();
                } else {
                    this.selected = clicked;
                    this.useButton.active = true;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void confirm() {
        if (this.selected == null) return;
        this.selectionConsumer.accept(this.selected);
        this.transitionTo(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(graphics);
        AioaScreenUtil.drawScreenBackground(graphics, this.width, this.height);
        int panelLeft = Math.max(6, this.gridLeft - 16);
        int panelRight = Math.min(this.width - 6, this.gridLeft + COLUMNS * CELL + 16);
        AioaScreenUtil.drawPanel(graphics, panelLeft, 20, panelRight, Math.min(this.height - 8, this.gridTop + ROWS * CELL + 52));
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 27, AioaScreenUtil.TEXT_MAIN);

        int hoveredIndex = -1;
        int start = this.page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && start + slot < this.filteredItems.size(); slot++) {
            Identifier id = this.filteredItems.get(start + slot);
            int x = this.gridLeft + (slot % COLUMNS) * CELL;
            int y = this.gridTop + (slot / COLUMNS) * CELL;
            boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            boolean chosen = id.equals(this.selected);
            graphics.fill(x + 2, y + 2, x + CELL - 2, y + CELL - 2,
                    chosen ? 0xDD17673E : hovered ? 0xDD243A2D : 0xCC101713);
            graphics.fill(x + 2, y + 2, x + CELL - 2, y + 3, chosen ? 0xFF92F5B8 : 0xFF335442);
            graphics.renderItem(new ItemStack(BuiltInRegistries.ITEM.getValue(id)), x + 14, y + 8);
            String shortName = this.font.plainSubstrByWidth(itemName(id), CELL - 6);
            graphics.drawCenteredString(this.font, shortName, x + CELL / 2, y + 29, 0xFFD8F7E2);
            if (hovered) hoveredIndex = start + slot;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredIndex >= 0 && hoveredIndex < this.filteredItems.size()) {
            Identifier id = this.filteredItems.get(hoveredIndex);
            graphics.renderTooltip(this.font, Component.literal(itemName(id) + "\n" + id), mouseX, mouseY);
        }
        this.finishUiRender(graphics);
    }

    private static String itemName(Identifier id) {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == null ? id.getPath() : item.getDescription().getString();
    }

    @Override
    public void onClose() {
        this.transitionTo(this.parent);
    }
}
