package com.flubburr.aioa.client.config;

import com.flubburr.aioa.config.AioaSpawnEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class AioaScreenUtil {

    static final int PANEL_BACKGROUND = 0xD01A1F29;
    static final int PANEL_BORDER = 0xFF5D6B7E;
    static final int PANEL_ACCENT = 0xFFB24735;
    static final int TEXT_MAIN = 0xFFF4F1E8;
    static final int TEXT_SUB = 0xFFB6C0CC;
    static final int ROWS_PER_PAGE = 7;

    private AioaScreenUtil() {
    }

    static Button button(int x, int y, int width, String label, Button.OnPress onPress) {
        return Button.builder(Component.literal(label), onPress).bounds(x, y, width, 20).build();
    }

    static void drawPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, PANEL_BACKGROUND);
        guiGraphics.fill(left, top, right, top + 1, PANEL_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, PANEL_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, PANEL_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, PANEL_BORDER);
        guiGraphics.fill(left + 1, top + 1, right - 1, top + 4, PANEL_ACCENT);
    }

    static String boolLabel(String label, boolean value) {
        return label + ": " + (value ? "ON" : "OFF");
    }

    static String cycleLabel(String label, Object value) {
        return label + ": " + humanizeEnum(value);
    }

    static String humanizeEnum(Object value) {
        return value.toString().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    static String clip(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    static List<ResourceLocation> allEntityIds() {
        return new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .sorted(Comparator.comparing(AioaScreenUtil::entitySortKey).thenComparing(ResourceLocation::toString))
                .toList());
    }

    static List<ResourceLocation> hostileEntityIds() {
        return new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> type.getCategory() == MobCategory.MONSTER)
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .sorted(Comparator.comparing(AioaScreenUtil::entitySortKey).thenComparing(ResourceLocation::toString))
                .toList());
    }

    static String entityDisplayName(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) {
            return id.toString();
        }
        String description = type.getDescription().getString();
        return description == null || description.isBlank() ? id.toString() : description;
    }

    static String entityLine(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        String category = type == null ? "unknown" : type.getCategory().getName();
        return entityDisplayName(id) + " [" + category + "] - " + id;
    }

    static String summarizeEntry(String rawEntry) {
        Optional<AioaSpawnEntry> parsed = AioaSpawnEntry.parse(rawEntry, warning -> {
        });
        if (parsed.isEmpty()) {
            return "Invalid entry: " + rawEntry;
        }

        AioaSpawnEntry entry = parsed.get();
        return entityDisplayName(entry.entityId())
                + " | "
                + (entry.enabled() ? "on" : "off")
                + " | wt "
                + entry.weight()
                + " | "
                + Math.round(entry.chance() * 100.0D)
                + "% | "
                + entry.minGroupSize()
                + "-"
                + entry.maxGroupSize();
    }

    static <E extends Enum<E>> E next(E current, E[] values) {
        return values[(current.ordinal() + 1) % values.length];
    }

    static EditBox numberBox(int x, int y, int width, int value) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.literal("number"));
        box.setValue(Integer.toString(value));
        box.setFilter(text -> text.isEmpty() || text.matches("\\d{1,6}"));
        box.setMaxLength(6);
        return box;
    }

    static EditBox decimalBox(int x, int y, int width, double value) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.literal("decimal"));
        box.setValue(Double.toString(value));
        box.setFilter(text -> text.isEmpty() || text.matches("\\d{0,1}(\\.\\d{0,3})?"));
        box.setMaxLength(5);
        return box;
    }

    static int readNumber(EditBox box, int fallback) {
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static double readDecimal(EditBox box, double fallback) {
        try {
            return Double.parseDouble(box.getValue().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static double clampChance(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static String entitySortKey(ResourceLocation id) {
        return entityDisplayName(id).toLowerCase(Locale.ROOT);
    }
}
