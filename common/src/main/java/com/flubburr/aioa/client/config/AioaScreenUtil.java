package com.flubburr.aioa.client.config;

import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.config.AioaSpawnEntry;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;

public final class AioaScreenUtil {

    private static final Map<ResourceLocation, LivingEntity> PREVIEW_ENTITY_CACHE = new HashMap<>();
    private static final RandomSource UI_SOUND_RANDOM = RandomSource.create();
    private static final ResourceLocation UI_CLICK_SOUND = ResourceLocation.fromNamespaceAndPath("aioa", "ui.click");
    private static final ResourceLocation UI_HOVER_SOUND = ResourceLocation.fromNamespaceAndPath("aioa", "ui.hover");
    private static final ResourceLocation UI_SLIDER_SOUND = ResourceLocation.fromNamespaceAndPath("aioa", "ui.slider");
    private static final ResourceLocation MENU_MUSIC_SOUND = ResourceLocation.fromNamespaceAndPath("aioa", "music.menu");
    private static MenuLoopSound menuMusic;

    static final int BUTTON_HEIGHT = 24;
    static final int PANEL_BACKGROUND = 0xE0101010;
    static final int PANEL_BORDER = 0xFF000000;
    static final int PANEL_ACCENT = 0xFF01BF63;
    static final int PANEL_SOFT = 0xC0121A16;
    static final int PANEL_SOFT_BORDER = 0xFF000000;
    static final int PANEL_SELECTED = 0xD001BF63;
    static final int PANEL_CARD = 0xDD0F1713;
    static final int TEXT_MAIN = 0x6BF250;
    static final int TEXT_SUB = 0xFF9AD6AE;
    static final int TEXT_MUTED = 0xFF9AD6AE;
    static final int BUTTON_TEXT = 0xFF6EFFBA;
    static final int ROWS_PER_PAGE = 7;

    private AioaScreenUtil() {
    }

    private static int lerpColor(int from, int to, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int a = Math.round(((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
        int r = Math.round(((from >>> 16) & 0xFF) + ((((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t));
        int g = Math.round(((from >>> 8) & 0xFF) + ((((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t));
        int b = Math.round((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static Button button(int x, int y, int width, String label, Button.OnPress onPress) {
        return new AioaButton(x, y, width, BUTTON_HEIGHT, Component.literal(label), onPress);
    }

    static EditBox searchBox(int x, int y, int width, String hint) {
        return new AioaSearchBox(x, y, width, BUTTON_HEIGHT, Component.literal(hint));
    }

    static void drawPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, PANEL_BACKGROUND);
        guiGraphics.fill(left, top, right, top + 1, PANEL_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, PANEL_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, PANEL_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, PANEL_BORDER);
        guiGraphics.fill(left + 1, top + 1, right - 1, top + 4, PANEL_ACCENT);
    }

    public static void drawScreenBackground(GuiGraphics guiGraphics, int width, int height) {
        guiGraphics.fillGradient(0, 0, width, height, 0xF0101010, 0xFF080808);
    }

    static void drawInsetPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom, boolean selected) {
        guiGraphics.fill(left, top, right, bottom, selected ? PANEL_SELECTED : PANEL_SOFT);
        guiGraphics.fill(left, top, right, top + 1, selected ? PANEL_ACCENT : PANEL_SOFT_BORDER);
        guiGraphics.fill(left, bottom - 1, right, bottom, selected ? PANEL_ACCENT : PANEL_SOFT_BORDER);
        guiGraphics.fill(left, top, left + 1, bottom, selected ? PANEL_ACCENT : PANEL_SOFT_BORDER);
        guiGraphics.fill(right - 1, top, right, bottom, selected ? PANEL_ACCENT : PANEL_SOFT_BORDER);
    }

    static void drawClippedContent(GuiGraphics guiGraphics, int left, int top, int right, int bottom, Runnable contentRenderer) {
        guiGraphics.enableScissor(left, top, right, bottom);
        contentRenderer.run();
        guiGraphics.disableScissor();
    }

    static int drawWrappedCenteredText(GuiGraphics guiGraphics, Font font, Component text, int centerX, int top, int maxWidth, int color) {
        int y = top;
        for (FormattedCharSequence line : font.split(text, Math.max(40, maxWidth))) {
            guiGraphics.drawCenteredString(font, line, centerX, y, color);
            y += 10;
        }
        return y;
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

    static int panelWidth(int screenWidth, int maxWidth) {
        return Math.min(maxWidth, Math.max(360, screenWidth - 48));
    }

    static int panelLeft(int screenWidth, int panelWidth) {
        return (screenWidth - panelWidth) / 2;
    }

    static int adaptiveContentTop(int screenHeight, int desiredTop, int bottomPadding, int minContentHeight) {
        int maxTop = Math.max(52, screenHeight - bottomPadding - minContentHeight);
        return Math.max(52, Math.min(desiredTop, maxTop));
    }

    static int adaptiveContentBottom(int screenHeight, int desiredBottom, int minBottomPadding, int contentTop, int minContentHeight) {
        int bottom = Math.max(contentTop + minContentHeight, desiredBottom);
        return Math.min(screenHeight - Math.max(28, minBottomPadding), bottom);
    }

    static int contentTop(int panelTop) {
        return panelTop + 28;
    }

    static void drawScrollBar(GuiGraphics guiGraphics, int x, int top, int height, int scrollOffset, int maxScroll) {
        drawInsetPanel(guiGraphics, x, top, x + 8, top + height, false);
        if (maxScroll <= 0) {
            guiGraphics.fill(x + 1, top + 1, x + 7, top + height - 1, 0x6601BF63);
            return;
        }

        int thumbHeight = Math.max(20, height / 4);
        int range = Math.max(1, height - thumbHeight - 2);
        int thumbTop = top + 1 + (int) Math.round((scrollOffset / (double) maxScroll) * range);
        guiGraphics.fill(x + 1, thumbTop, x + 7, thumbTop + thumbHeight, PANEL_ACCENT);
    }

    public static void tickMenuAudio(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }

        if (menuMusic != null && menuMusic.isStopped()) {
            menuMusic = null;
        }

        if (minecraft.screen instanceof AioaAnimatedScreen) {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(MENU_MUSIC_SOUND);
            if (soundEvent == null) {
                return;
            }

            if (menuMusic == null || !minecraft.getSoundManager().isActive(menuMusic)) {
                menuMusic = new MenuLoopSound(soundEvent);
                minecraft.getSoundManager().play(menuMusic);
            }
            return;
        }

        if (menuMusic != null) {
            minecraft.getSoundManager().stop(menuMusic);
            menuMusic = null;
        }
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

    static List<ResourceLocation> allBiomeIds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return new ArrayList<>(minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME).listElementIds()
                    .map(key -> key.location()).sorted(Comparator.comparing(AioaScreenUtil::biomeSortKey).thenComparing(ResourceLocation::toString)).toList());
        }
        if (minecraft.getConnection() != null) {
            return new ArrayList<>(minecraft.getConnection().registryAccess().lookupOrThrow(Registries.BIOME).listElementIds()
                    .map(key -> key.location()).sorted(Comparator.comparing(AioaScreenUtil::biomeSortKey).thenComparing(ResourceLocation::toString)).toList());
        }
        return defaultBiomeIds();
    }

    private static ArrayList<ResourceLocation> defaultBiomeIds() {
        List<String> vanillaBiomes = List.of(
                "minecraft:badlands",
                "minecraft:bamboo_jungle",
                "minecraft:basalt_deltas",
                "minecraft:beach",
                "minecraft:birch_forest",
                "minecraft:cherry_grove",
                "minecraft:cold_ocean",
                "minecraft:crimson_forest",
                "minecraft:dark_forest",
                "minecraft:deep_cold_ocean",
                "minecraft:deep_dark",
                "minecraft:deep_frozen_ocean",
                "minecraft:deep_lukewarm_ocean",
                "minecraft:deep_ocean",
                "minecraft:desert",
                "minecraft:dripstone_caves",
                "minecraft:end_barrens",
                "minecraft:end_highlands",
                "minecraft:end_midlands",
                "minecraft:eroded_badlands",
                "minecraft:flower_forest",
                "minecraft:forest",
                "minecraft:frozen_ocean",
                "minecraft:frozen_peaks",
                "minecraft:frozen_river",
                "minecraft:grove",
                "minecraft:ice_spikes",
                "minecraft:jagged_peaks",
                "minecraft:jungle",
                "minecraft:lukewarm_ocean",
                "minecraft:lush_caves",
                "minecraft:mangrove_swamp",
                "minecraft:meadow",
                "minecraft:mushroom_fields",
                "minecraft:nether_wastes",
                "minecraft:ocean",
                "minecraft:old_growth_birch_forest",
                "minecraft:old_growth_pine_taiga",
                "minecraft:old_growth_spruce_taiga",
                "minecraft:plains",
                "minecraft:river",
                "minecraft:savanna",
                "minecraft:savanna_plateau",
                "minecraft:small_end_islands",
                "minecraft:snowy_beach",
                "minecraft:snowy_plains",
                "minecraft:snowy_slopes",
                "minecraft:snowy_taiga",
                "minecraft:soul_sand_valley",
                "minecraft:sparse_jungle",
                "minecraft:stony_peaks",
                "minecraft:stony_shore",
                "minecraft:sunflower_plains",
                "minecraft:swamp",
                "minecraft:taiga",
                "minecraft:the_end",
                "minecraft:the_void",
                "minecraft:warm_ocean",
                "minecraft:warped_forest",
                "minecraft:windswept_forest",
                "minecraft:windswept_gravelly_hills",
                "minecraft:windswept_hills",
                "minecraft:windswept_savanna",
                "minecraft:wooded_badlands"
        );
        return new ArrayList<>(vanillaBiomes.stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AioaScreenUtil::biomeSortKey).thenComparing(ResourceLocation::toString))
                .toList());
    }

    static String entityDisplayName(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) {
            return id.toString();
        }
        String description = type.getDescription().getString();
        return description == null || description.isBlank() ? id.toString() : description;
    }

    static String entityLine(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        String category = type == null ? "unknown" : type.getCategory().getName();
        return entityDisplayName(id) + " [" + category + "] - " + id;
    }

    static String categoryLabel(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        return type == null ? "unknown" : humanizeEnum(type.getCategory().getName());
    }

    static String biomeDisplayName(ResourceLocation id) {
        return Arrays.stream(id.getPath().split("[_/]"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    static String biomeLine(ResourceLocation id) {
        return biomeDisplayName(id) + " - " + id;
    }

    static String biomeCategory(ResourceLocation id) {
        if (!"minecraft".equals(id.getNamespace())) {
            return "Modded";
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("nether")
                || path.contains("soul_sand")
                || path.contains("crimson")
                || path.contains("warped")
                || path.contains("basalt_deltas")) {
            return "Nether";
        }
        if (path.contains("end")
                || path.contains("small_end_islands")
                || path.contains("end_barrens")
                || path.contains("end_midlands")
                || path.contains("end_highlands")) {
            return "End";
        }
        return "Overworld";
    }

    static String dimensionCategory(ResourceLocation id) {
        if (!"minecraft".equals(id.getNamespace())) {
            return "Modded";
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("blaze")
                || path.contains("ghast")
                || path.contains("hoglin")
                || path.contains("piglin")
                || path.contains("zoglin")
                || path.contains("magma_cube")
                || path.contains("wither_skeleton")
                || path.contains("strider")) {
            return "Nether";
        }
        if (path.contains("enderman")
                || path.contains("endermite")
                || path.contains("shulker")
                || path.contains("ender_dragon")) {
            return "End";
        }
        return "Overworld";
    }

    static ItemStack entityPreviewItem(ResourceLocation id) {
        ResourceLocation eggId = AioaEntityHelper.parseResourceLocation(id.getNamespace() + ":" + id.getPath() + "_spawn_egg");
        if (eggId == null) {
            return new ItemStack(Items.BARRIER);
        }
        var item = BuiltInRegistries.ITEM.getValue(eggId);
        if (item instanceof SpawnEggItem) {
            return new ItemStack(item);
        }
        return new ItemStack(Items.BARRIER);
    }

    static void drawMobPreview(GuiGraphics guiGraphics, Font font, int left, int top, int width, int height, ResourceLocation id, boolean selected, List<Component> detailLines) {
        drawInsetPanel(guiGraphics, left, top, left + width, top + height, selected);
        int padding = 12;
        int modelPanelWidth = Math.max(92, Math.min(132, width / 3));
        int modelLeft = left + width - modelPanelWidth - padding;
        int modelTop = top + 10;
        int modelBottom = top + height - 10;
        int textLeft = left + padding;
        int textRight = modelLeft - 12;
        int maxTextWidth = Math.max(96, textRight - textLeft);

        guiGraphics.drawString(font, Component.literal(clip(entityDisplayName(id), Math.max(18, maxTextWidth / 6))), textLeft, top + 12, TEXT_MAIN);
        guiGraphics.drawString(font, Component.literal(categoryLabel(id)), textLeft, top + 27, TEXT_SUB);

        int lineY = top + 44;
        for (Component line : detailLines) {
            if (lineY > top + height - 22) {
                break;
            }
            guiGraphics.drawString(font, Component.literal(clip(line.getString(), Math.max(18, maxTextWidth / 6))), textLeft, lineY, lineY == top + 44 ? TEXT_MAIN : TEXT_SUB);
            lineY += 14;
        }

        drawInsetPanel(guiGraphics, modelLeft, modelTop, left + width - padding, modelBottom, false);
        LivingEntity previewEntity = previewEntity(id);
        if (previewEntity != null) {
            int modelCenterX = modelLeft + ((left + width - padding - modelLeft) / 2);
            int modelAnchorY = modelBottom - 10;
            int scale = Math.max(24, Math.min(42, (modelBottom - modelTop) / 2));
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, modelCenterX - scale, modelTop, modelCenterX + scale, modelAnchorY, scale, 0.0F, 0.0F, 0.0F, previewEntity);
        } else {
            int itemX = modelLeft + (((left + width - padding) - modelLeft) / 2) - 8;
            int itemY = modelTop + ((modelBottom - modelTop) / 2) - 8;
            guiGraphics.renderItem(entityPreviewItem(id), itemX, itemY);
        }
    }

    static void drawEntityViewport(GuiGraphics guiGraphics, Font font, int left, int top, int width, int height,
                                   ResourceLocation id, int mouseX, int mouseY, String state) {
        drawInsetPanel(guiGraphics, left, top, left + width, top + height, true);
        guiGraphics.fill(left + 2, top + 20, left + width - 2, top + height - 2, 0xFF080D0A);
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + 20, 0xFF17271E);
        guiGraphics.drawString(font, "3D VIEWPORT", left + 8, top + 7, TEXT_MAIN);
        String clippedState = font.plainSubstrByWidth(state, Math.max(20, width - 100));
        guiGraphics.drawString(font, clippedState, left + width - font.width(clippedState) - 8, top + 7, TEXT_SUB);

        int floorTop = top + Math.max(36, height / 2);
        int floorBottom = top + height - 4;
        for (int row = 0; row <= 5; row++) {
            double t = row / 5.0D;
            int y = floorTop + (int) Math.round(t * t * (floorBottom - floorTop));
            guiGraphics.fill(left + 4, y, left + width - 4, y + 1, 0x3A4ACB78);
        }
        int centerX = left + width / 2;
        for (int column = -4; column <= 4; column++) {
            int nearX = centerX + column * Math.max(8, width / 9);
            int farX = centerX + column * Math.max(2, width / 32);
            drawViewportLine(guiGraphics, nearX, floorBottom, farX, floorTop, 0x334ACB78);
        }

        LivingEntity entity = previewEntity(id);
        if (entity != null) {
            guiGraphics.enableScissor(left + 2, top + 21, left + width - 2, top + height - 2);
            float simulationYaw = (System.currentTimeMillis() % 12_000L) / 12_000.0F * 360.0F;
            float orbitX = (float) Math.sin(System.currentTimeMillis() / 1600.0D) * 18.0F;
            float orbitY = -8.0F;
            int scale = Math.max(28, Math.min(72, height / 2));
            int mobX = width >= 230 ? left + width * 2 / 5 : centerX;
            entity.tickCount++;
            entity.setYRot(simulationYaw);
            entity.setYHeadRot(simulationYaw);
            entity.setYBodyRot(simulationYaw);
            entity.walkAnimation.update(0.65F, 1.0F, 1.0F);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, mobX - scale, top + 22, mobX + scale, floorBottom - 3, scale, 0.0F, orbitX, orbitY, entity);
            if (width >= 230 && Minecraft.getInstance().player != null) {
                int playerX = left + width * 3 / 4;
                int playerScale = Math.max(24, scale * 3 / 4);
                InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, playerX - playerScale, top + 22, playerX + playerScale,
                        floorBottom - 3, playerScale, 0.0F, orbitX, orbitY, Minecraft.getInstance().player);
            }
            guiGraphics.disableScissor();
        } else {
            guiGraphics.renderItem(entityPreviewItem(id), centerX - 8, top + height / 2 - 8);
        }
    }

    private static void drawViewportLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(1, Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
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

    static LivingEntity previewEntity(ResourceLocation id) {
        LivingEntity cached = PREVIEW_ENTITY_CACHE.get(id);
        if (cached != null && cached.isAlive()) {
            return cached;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) {
            return null;
        }

        Entity entity;
        try {
            entity = type.create(minecraft.level, EntitySpawnReason.COMMAND);
        } catch (Exception ignored) {
            return null;
        }

        if (!(entity instanceof LivingEntity livingEntity)) {
            return null;
        }

        livingEntity.setYRot(25.0F);
        livingEntity.setXRot(0.0F);
        PREVIEW_ENTITY_CACHE.put(id, livingEntity);
        return livingEntity;
    }

    static String sectionLabel(String title, boolean expanded) {
        return (expanded ? "[-] " : "[+] ") + title;
    }

    static AioaSlider intSlider(int x, int y, int width, String label, int min, int max, int step, int initialValue, java.util.function.IntConsumer consumer) {
        return new AioaSlider(
                x,
                y,
                width,
                min,
                max,
                step,
                initialValue,
                value -> label + ": " + (int) Math.round(value),
                value -> consumer.accept((int) Math.round(value))
        );
    }

    static AioaSlider decimalSlider(int x, int y, int width, String label, double min, double max, double step, double initialValue, DoubleFunction<String> formatter, DoubleConsumer consumer) {
        return new AioaSlider(
                x,
                y,
                width,
                min,
                max,
                step,
                initialValue,
                formatter != null ? formatter : value -> label + ": " + value,
                consumer
        );
    }

    private static String entitySortKey(ResourceLocation id) {
        return entityDisplayName(id).toLowerCase(Locale.ROOT);
    }

    private static String biomeSortKey(ResourceLocation id) {
        return biomeDisplayName(id).toLowerCase(Locale.ROOT);
    }

    private static void playUiSound(ResourceLocation soundId, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        float configuredVolume = (float) AioaConfigManager.getConfig().clientUi.uiSoundVolume;
        if (configuredVolume <= 0.0F) return;
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                soundId,
                SoundSource.MASTER,
                volume * configuredVolume,
                pitch,
                UI_SOUND_RANDOM,
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true
        ));
    }

    private static float randomPitch(float min, float max) {
        return min + (UI_SOUND_RANDOM.nextFloat() * (max - min));
    }

    private static final class MenuLoopSound extends AbstractTickableSoundInstance {

        private MenuLoopSound(SoundEvent soundEvent) {
            super(soundEvent, SoundSource.MASTER, UI_SOUND_RANDOM);
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
            this.volume = configuredMenuVolume();
            this.pitch = 1.0F;
        }

        @Override
        public void tick() {
            this.volume = configuredMenuVolume();
        }

        private static float configuredMenuVolume() {
            return (float) AioaConfigManager.getConfig().clientUi.menuSfxVolume;
        }
    }

    static final class AioaSlider extends AbstractSliderButton {

        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> labelFactory;
        private final DoubleConsumer consumer;
        private boolean wasHovered;
        private float hoverProgress;
        private double lastSoundValue = Double.NaN;

        AioaSlider(
                int x,
                int y,
                int width,
                double min,
                double max,
                double step,
                double initialValue,
                DoubleFunction<String> labelFactory,
                DoubleConsumer consumer
        ) {
            super(x, y, width, BUTTON_HEIGHT, Component.empty(), 0.0D);
            this.min = min;
            this.max = max;
            this.step = step;
            this.labelFactory = labelFactory;
            this.consumer = consumer;
            this.setSliderValue(initialValue);
        }

        double actualValue() {
            return snap(this.min + ((this.max - this.min) * this.value));
        }

        int actualIntValue() {
            return (int) Math.round(this.actualValue());
        }

        void setSliderValue(double actualValue) {
            double clamped = Math.max(this.min, Math.min(this.max, actualValue));
            this.value = (clamped - this.min) / (this.max - this.min);
            this.value = Math.max(0.0D, Math.min(1.0D, this.value));
            this.updateMessage();
            this.applyValue();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.labelFactory.apply(this.actualValue())));
        }

        @Override
        protected void applyValue() {
            this.consumer.accept(this.actualValue());
        }

        @Override
        public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
            playUiSound(UI_SLIDER_SOUND, 0.55F, randomPitch(0.82F, 0.96F));
            this.lastSoundValue = this.actualValue();
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            super.onDrag(mouseX, mouseY, dragX, dragY);
            this.playSliderStepIfChanged();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            this.lastSoundValue = Double.NaN;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int left = this.getX();
            int top = this.getY();
            int right = left + this.width;
            int bottom = top + this.height;
            boolean hovered = this.isHoveredOrFocused();
            this.hoverProgress += ((hovered ? 1.0F : 0.0F) - this.hoverProgress) * 0.24F;
            if (hovered && !this.wasHovered && this.active) {
                playUiSound(UI_HOVER_SOUND, 0.45F, randomPitch(1.05F, 1.22F));
            }
            this.wasHovered = hovered;

            drawInsetPanel(guiGraphics, left, top, right, bottom, this.hoverProgress > 0.45F);
            guiGraphics.fill(left + 2, top + 2, right - 2, top + 4, hovered ? 0x88000000 : 0x55000000);

            int trackLeft = left + 12;
            int trackRight = right - 12;
            int trackTop = top + 17;
            int trackBottom = top + 21;
            int trackWidth = Math.max(1, trackRight - trackLeft);
            int progressWidth = (int) Math.round(trackWidth * this.value);
            int knobCenterX = trackLeft + progressWidth;
            knobCenterX = Math.max(trackLeft, Math.min(trackRight, knobCenterX));

            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    this.getMessage(),
                    left + 12,
                    top + 4,
                    this.active ? TEXT_MAIN : TEXT_MUTED
            );

            drawInsetPanel(guiGraphics, trackLeft, trackTop, trackRight, trackBottom, false);
            if (progressWidth > 0) {
                guiGraphics.fill(trackLeft + 1, trackTop + 1, Math.min(trackLeft + progressWidth, trackRight - 1), trackBottom - 1, PANEL_ACCENT);
            }

            int knobRadius = 5 + Math.round(this.hoverProgress);
            guiGraphics.fill(knobCenterX - knobRadius, trackTop - 4, knobCenterX + knobRadius, trackBottom + 4, this.active ? TEXT_MAIN : TEXT_MUTED);
            guiGraphics.fill(knobCenterX - 2, trackTop - 1, knobCenterX + 2, trackBottom + 1, 0xFF0C1711);

            String valueText = this.valueText();
            int bubbleWidth = Math.max(26, Minecraft.getInstance().font.width(valueText) + 10);
            int bubbleLeft = Math.max(left + 8, Math.min(right - bubbleWidth - 8, knobCenterX - (bubbleWidth / 2)));
            int bubbleTop = top - 16;
            drawInsetPanel(guiGraphics, bubbleLeft, bubbleTop, bubbleLeft + bubbleWidth, bubbleTop + 12, hovered);
            guiGraphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    Component.literal(valueText),
                    bubbleLeft + (bubbleWidth / 2),
                    bubbleTop + 2,
                    hovered ? TEXT_MAIN : TEXT_SUB
            );
        }

        private double snap(double raw) {
            if (this.step <= 0.0D) {
                return raw;
            }
            double snapped = Math.round(raw / this.step) * this.step;
            return Math.max(this.min, Math.min(this.max, snapped));
        }

        private String valueText() {
            double actual = this.actualValue();
            if (this.step >= 1.0D && Math.abs(actual - Math.rint(actual)) < 1.0E-6D) {
                return Integer.toString((int) Math.round(actual));
            }
            String raw = String.format(Locale.ROOT, "%.3f", actual);
            return raw.indexOf('.') >= 0 ? raw.replaceAll("0+$", "").replaceAll("\\.$", "") : raw;
        }

        private void playSliderStepIfChanged() {
            double currentValue = this.actualValue();
            if (!Double.isNaN(this.lastSoundValue) && Math.abs(currentValue - this.lastSoundValue) < 1.0E-6D) {
                return;
            }

            this.lastSoundValue = currentValue;
            playUiSound(UI_SLIDER_SOUND, 0.5F, randomPitch(0.9F, 1.15F));
        }
    }

    static final class AioaButton extends Button {

        private boolean wasHovered;
        private float hoverProgress;
        private float pressPulse;

        AioaButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
            playUiSound(UI_CLICK_SOUND, 0.65F, randomPitch(0.75F, 1.08F));
            this.pressPulse = 1.0F;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int left = this.getX();
            int top = this.getY();
            int right = left + this.width;
            int bottom = top + this.height;
            boolean hovered = this.isHoveredOrFocused();
            this.hoverProgress += ((hovered ? 1.0F : 0.0F) - this.hoverProgress) * 0.28F;
            this.pressPulse *= 0.72F;
            if (hovered && !this.wasHovered && this.active) {
                playUiSound(UI_HOVER_SOUND, 0.45F, randomPitch(1.05F, 1.22F));
            }
            this.wasHovered = hovered;
            int pressInset = Math.round(this.pressPulse * 2.0F);
            left += pressInset;
            top += pressInset;
            right -= pressInset;
            bottom -= pressInset;
            int fill = !this.active ? 0xAA0B0B0B : lerpColor(0xE001BF63, 0xFF03D772, this.hoverProgress);
            int text = !this.active ? TEXT_MUTED : BUTTON_TEXT;

            guiGraphics.fill(left, top, right, bottom, fill);
            guiGraphics.fill(left, top, right, top + 1, PANEL_BORDER);
            guiGraphics.fill(left, bottom - 1, right, bottom, PANEL_BORDER);
            guiGraphics.fill(left, top, left + 1, bottom, PANEL_BORDER);
            guiGraphics.fill(right - 1, top, right, bottom, PANEL_BORDER);
            guiGraphics.fill(left + 2, top + 2, right - 2, top + 4, hovered ? 0x88000000 : 0x55000000);
            int sweepWidth = Math.round((right - left - 4) * this.hoverProgress);
            if (sweepWidth > 0) {
                guiGraphics.fill(left + 2, bottom - 3, left + 2 + sweepWidth, bottom - 1, 0xCCB4FFD4);
            }

            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), (left + right) / 2, (top + bottom - 8) / 2, text);
        }
    }

    static final class AioaSearchBox extends EditBox {

        AioaSearchBox(int x, int y, int width, int height, Component hint) {
            super(Minecraft.getInstance().font, x, y, width, height, hint);
            this.setBordered(false);
            this.setTextColor(TEXT_MAIN);
            this.setTextColorUneditable(TEXT_MUTED);
            this.setHint(Component.literal(hint.getString()));
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int left = this.getX();
            int top = this.getY();
            int right = left + this.getWidth();
            int bottom = top + this.getHeight();
            drawInsetPanel(guiGraphics, left, top, right, bottom, this.isFocused());
            guiGraphics.fill(left + 1, top + 1, right - 1, top + 4, 0x55000000);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(6.0F, 2.0F, 0.0F);
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.pose().popPose();
        }
    }
}
