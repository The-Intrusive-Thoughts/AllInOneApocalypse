package com.flubburr.aioa.forge.config;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class AioaForgeFallbackConfigScreen extends Screen {

    private final Screen parent;
    private AioaConfig editableConfig;

    private AioaForgeFallbackConfigScreen(Screen parent, AioaConfig editableConfig) {
        super(Component.literal("AIOA Config (Forge Fallback)"));
        this.parent = parent;
        this.editableConfig = editableConfig;
    }

    public static Screen create(Screen parent) {
        return new AioaForgeFallbackConfigScreen(parent, AioaConfigManager.getConfigCopy());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonWidth = 280;
        int y = 44;
        int step = 24;

        this.addRenderableWidget(Button.builder(Component.literal("Hostile Spawn Control"), button ->
                        this.minecraft.setScreen(new HostileSettingsScreen(this, this.editableConfig)))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step;

        this.addRenderableWidget(Button.builder(Component.literal("Day Surface Spawn Settings"), button ->
                        this.minecraft.setScreen(new DaySurfaceSettingsScreen(this, this.editableConfig)))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step;

        this.addRenderableWidget(Button.builder(Component.literal("Edit Hostile Allow-List (one id per line)"), button ->
                        this.minecraft.setScreen(new DelimitedListScreen(
                                this,
                                "Hostile Allow-List",
                                "Each line should be a valid entity id such as minecraft:zombie.",
                                this.editableConfig.hostileSpawnControl.whitelistEntityIds,
                                value -> this.editableConfig.hostileSpawnControl.whitelistEntityIds = new ArrayList<>(value)
                        )))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step;

        this.addRenderableWidget(Button.builder(Component.literal("Edit Allowed Biomes (one id per line)"), button ->
                        this.minecraft.setScreen(new DelimitedListScreen(
                                this,
                                "Allowed Biome IDs",
                                "Leave empty to allow all biomes.",
                                this.editableConfig.daySurfaceSpawns.allowedBiomeIds,
                                value -> this.editableConfig.daySurfaceSpawns.allowedBiomeIds = new ArrayList<>(value)
                        )))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step;

        this.addRenderableWidget(Button.builder(Component.literal("Edit Day Spawn Pool Entries (one per line)"), button ->
                        this.minecraft.setScreen(new DelimitedListScreen(
                                this,
                                "Day Spawn Pool Entries",
                                "Format: entity_id;enabled=true;weight=10;chance=1.0;min=1;max=3",
                                this.editableConfig.daySurfaceSpawns.spawnPoolEntries,
                                value -> this.editableConfig.daySurfaceSpawns.spawnPoolEntries = new ArrayList<>(value)
                        )))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step + 4;

        int bottomY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), button ->
                        this.minecraft.setScreen(new AioaForgeFallbackConfigScreen(this.parent, AioaConfig.createDefault())))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
                    this.editableConfig = this.editableConfig.sanitize();
                    AioaConfigManager.save(this.editableConfig);
                    this.onClose();
                })
                .bounds(centerX - 142, bottomY, 140, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
                .bounds(centerX + 2, bottomY, 140, 20)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("YACL is unavailable. Use this built-in Forge config editor."),
                this.width / 2,
                28,
                0xA0A0A0
        );
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static Component booleanLabel(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "ON" : "OFF"));
    }

    private static final class HostileSettingsScreen extends Screen {

        private final Screen parent;
        private final AioaConfig editableConfig;

        private boolean enabled;
        private boolean overworldOnly;
        private boolean ignoreStructureSpawns;
        private boolean ignoreSpawnerSpawns;
        private boolean ignoreSpecialSpawns;

        private HostileSettingsScreen(Screen parent, AioaConfig editableConfig) {
            super(Component.literal("Hostile Spawn Control"));
            this.parent = parent;
            this.editableConfig = editableConfig;
        }

        @Override
        protected void init() {
            this.enabled = this.editableConfig.hostileSpawnControl.enabled;
            this.overworldOnly = this.editableConfig.hostileSpawnControl.overworldOnly;
            this.ignoreStructureSpawns = this.editableConfig.hostileSpawnControl.ignoreStructureSpawns;
            this.ignoreSpawnerSpawns = this.editableConfig.hostileSpawnControl.ignoreSpawnerSpawns;
            this.ignoreSpecialSpawns = this.editableConfig.hostileSpawnControl.ignoreSpecialSpawns;

            int centerX = this.width / 2;
            int w = 300;
            int y = 44;
            int step = 24;

            this.addRenderableWidget(Button.builder(booleanLabel("Hostile nullification", this.enabled), button -> {
                        this.enabled = !this.enabled;
                        button.setMessage(booleanLabel("Hostile nullification", this.enabled));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("Overworld only", this.overworldOnly), button -> {
                        this.overworldOnly = !this.overworldOnly;
                        button.setMessage(booleanLabel("Overworld only", this.overworldOnly));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("Ignore structure spawns", this.ignoreStructureSpawns), button -> {
                        this.ignoreStructureSpawns = !this.ignoreStructureSpawns;
                        button.setMessage(booleanLabel("Ignore structure spawns", this.ignoreStructureSpawns));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("Ignore spawner spawns", this.ignoreSpawnerSpawns), button -> {
                        this.ignoreSpawnerSpawns = !this.ignoreSpawnerSpawns;
                        button.setMessage(booleanLabel("Ignore spawner spawns", this.ignoreSpawnerSpawns));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("Ignore special spawns", this.ignoreSpecialSpawns), button -> {
                        this.ignoreSpecialSpawns = !this.ignoreSpecialSpawns;
                        button.setMessage(booleanLabel("Ignore special spawns", this.ignoreSpecialSpawns));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());

            int bottomY = this.height - 28;
            this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
                        this.editableConfig.hostileSpawnControl.enabled = this.enabled;
                        this.editableConfig.hostileSpawnControl.overworldOnly = this.overworldOnly;
                        this.editableConfig.hostileSpawnControl.ignoreStructureSpawns = this.ignoreStructureSpawns;
                        this.editableConfig.hostileSpawnControl.ignoreSpawnerSpawns = this.ignoreSpawnerSpawns;
                        this.editableConfig.hostileSpawnControl.ignoreSpecialSpawns = this.ignoreSpecialSpawns;
                        this.minecraft.setScreen(this.parent);
                    })
                    .bounds(centerX - 142, bottomY, 140, 20)
                    .build());

            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.minecraft.setScreen(this.parent))
                    .bounds(centerX + 2, bottomY, 140, 20)
                    .build());
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(this.parent);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class DaySurfaceSettingsScreen extends Screen {

        private final Screen parent;
        private final AioaConfig editableConfig;

        private boolean enabled;
        private boolean overworldOnly;
        private boolean requireDaytime;
        private boolean requireClearSky;
        private boolean preventSunlightBurn;

        private EditBox spawnIntervalTicks;
        private EditBox spawnAttemptsPerPlayer;
        private EditBox minSpawnDistance;
        private EditBox maxSpawnDistance;
        private EditBox maxNearbyManagedMobs;

        private DaySurfaceSettingsScreen(Screen parent, AioaConfig editableConfig) {
            super(Component.literal("Day Surface Spawn Settings"));
            this.parent = parent;
            this.editableConfig = editableConfig;
        }

        @Override
        protected void init() {
            this.enabled = this.editableConfig.daySurfaceSpawns.enabled;
            this.overworldOnly = this.editableConfig.daySurfaceSpawns.overworldOnly;
            this.requireDaytime = this.editableConfig.daySurfaceSpawns.requireDaytime;
            this.requireClearSky = this.editableConfig.daySurfaceSpawns.requireClearSky;
            this.preventSunlightBurn = this.editableConfig.daySurfaceSpawns.preventSunlightBurn;

            int centerX = this.width / 2;
            int w = 300;
            int y = 34;
            int step = 20;

            this.addRenderableWidget(Button.builder(booleanLabel("Enable day surface spawns", this.enabled), button -> {
                        this.enabled = !this.enabled;
                        button.setMessage(booleanLabel("Enable day surface spawns", this.enabled));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("Overworld only", this.overworldOnly), button -> {
                        this.overworldOnly = !this.overworldOnly;
                        button.setMessage(booleanLabel("Overworld only", this.overworldOnly));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("Require daytime", this.requireDaytime), button -> {
                        this.requireDaytime = !this.requireDaytime;
                        button.setMessage(booleanLabel("Require daytime", this.requireDaytime));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("Require clear sky", this.requireClearSky), button -> {
                        this.requireClearSky = !this.requireClearSky;
                        button.setMessage(booleanLabel("Require clear sky", this.requireClearSky));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("Prevent sunlight burn", this.preventSunlightBurn), button -> {
                        this.preventSunlightBurn = !this.preventSunlightBurn;
                        button.setMessage(booleanLabel("Prevent sunlight burn", this.preventSunlightBurn));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step + 8;

            int labelX = centerX - 150;
            int inputX = centerX + 52;
            int inputWidth = 98;

            this.spawnIntervalTicks = createNumberBox(inputX, y, inputWidth, this.editableConfig.daySurfaceSpawns.spawnIntervalTicks);
            this.spawnAttemptsPerPlayer = createNumberBox(inputX, y + step, inputWidth, this.editableConfig.daySurfaceSpawns.spawnAttemptsPerPlayer);
            this.minSpawnDistance = createNumberBox(inputX, y + (step * 2), inputWidth, this.editableConfig.daySurfaceSpawns.minSpawnDistance);
            this.maxSpawnDistance = createNumberBox(inputX, y + (step * 3), inputWidth, this.editableConfig.daySurfaceSpawns.maxSpawnDistance);
            this.maxNearbyManagedMobs = createNumberBox(inputX, y + (step * 4), inputWidth, this.editableConfig.daySurfaceSpawns.maxNearbyManagedMobs);

            this.addRenderableWidget(this.spawnIntervalTicks);
            this.addRenderableWidget(this.spawnAttemptsPerPlayer);
            this.addRenderableWidget(this.minSpawnDistance);
            this.addRenderableWidget(this.maxSpawnDistance);
            this.addRenderableWidget(this.maxNearbyManagedMobs);

            this.spawnIntervalTicks.setHint(Component.literal("20-24000"));
            this.spawnAttemptsPerPlayer.setHint(Component.literal("1-16"));
            this.minSpawnDistance.setHint(Component.literal(">=8"));
            this.maxSpawnDistance.setHint(Component.literal(">= min+8"));
            this.maxNearbyManagedMobs.setHint(Component.literal(">=1"));

            int bottomY = this.height - 28;
            this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
                        this.editableConfig.daySurfaceSpawns.enabled = this.enabled;
                        this.editableConfig.daySurfaceSpawns.overworldOnly = this.overworldOnly;
                        this.editableConfig.daySurfaceSpawns.requireDaytime = this.requireDaytime;
                        this.editableConfig.daySurfaceSpawns.requireClearSky = this.requireClearSky;
                        this.editableConfig.daySurfaceSpawns.preventSunlightBurn = this.preventSunlightBurn;

                        this.editableConfig.daySurfaceSpawns.spawnIntervalTicks = Math.max(20, parseInt(this.spawnIntervalTicks, this.editableConfig.daySurfaceSpawns.spawnIntervalTicks));
                        this.editableConfig.daySurfaceSpawns.spawnAttemptsPerPlayer = clamp(parseInt(this.spawnAttemptsPerPlayer, this.editableConfig.daySurfaceSpawns.spawnAttemptsPerPlayer), 1, 16);
                        this.editableConfig.daySurfaceSpawns.minSpawnDistance = Math.max(8, parseInt(this.minSpawnDistance, this.editableConfig.daySurfaceSpawns.minSpawnDistance));
                        this.editableConfig.daySurfaceSpawns.maxSpawnDistance = Math.max(
                                this.editableConfig.daySurfaceSpawns.minSpawnDistance + 8,
                                parseInt(this.maxSpawnDistance, this.editableConfig.daySurfaceSpawns.maxSpawnDistance)
                        );
                        this.editableConfig.daySurfaceSpawns.maxNearbyManagedMobs = Math.max(1, parseInt(this.maxNearbyManagedMobs, this.editableConfig.daySurfaceSpawns.maxNearbyManagedMobs));
                        this.editableConfig.sanitize();
                        this.minecraft.setScreen(this.parent);
                    })
                    .bounds(centerX - 142, bottomY, 140, 20)
                    .build());

            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.minecraft.setScreen(this.parent))
                    .bounds(centerX + 2, bottomY, 140, 20)
                    .build());

            this.setInitialFocus(this.spawnIntervalTicks);
        }

        @Override
        public void tick() {
            this.spawnIntervalTicks.tick();
            this.spawnAttemptsPerPlayer.tick();
            this.minSpawnDistance.tick();
            this.maxSpawnDistance.tick();
            this.maxNearbyManagedMobs.tick();
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(this.parent);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

            int labelX = this.width / 2 - 150;
            int y = 142;
            int step = 20;
            int color = 0xA0A0A0;
            guiGraphics.drawString(this.font, Component.literal("Spawn interval ticks"), labelX, y + 6, color);
            guiGraphics.drawString(this.font, Component.literal("Spawn attempts per player"), labelX, y + step + 6, color);
            guiGraphics.drawString(this.font, Component.literal("Minimum spawn distance"), labelX, y + (step * 2) + 6, color);
            guiGraphics.drawString(this.font, Component.literal("Maximum spawn distance"), labelX, y + (step * 3) + 6, color);
            guiGraphics.drawString(this.font, Component.literal("Max nearby managed mobs"), labelX, y + (step * 4) + 6, color);

            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        private EditBox createNumberBox(int x, int y, int width, int value) {
            EditBox box = new EditBox(this.font, x, y, width, 20, Component.literal("number"));
            box.setValue(Integer.toString(value));
            box.setFilter(text -> text.isEmpty() || text.matches("\\d{1,6}"));
            box.setMaxLength(6);
            return box;
        }

        private static int parseInt(EditBox box, int fallback) {
            String value = box.getValue().trim();
            if (value.isEmpty()) {
                return fallback;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static final class DelimitedListScreen extends Screen {

        private final Screen parent;
        private final String description;
        private final List<String> initialValues;
        private final Consumer<List<String>> saveConsumer;
        private MultiLineEditBox editor;

        private DelimitedListScreen(
                Screen parent,
                String title,
                String description,
                List<String> initialValues,
                Consumer<List<String>> saveConsumer
        ) {
            super(Component.literal(title));
            this.parent = parent;
            this.description = description;
            this.initialValues = new ArrayList<>(initialValues);
            this.saveConsumer = saveConsumer;
        }

        @Override
        protected void init() {
            int editorX = this.width / 2 - 170;
            int editorY = 48;
            int editorWidth = 340;
            int editorHeight = Math.max(90, this.height - 110);

            this.editor = new MultiLineEditBox(
                    this.font,
                    editorX,
                    editorY,
                    editorWidth,
                    editorHeight,
                    Component.literal("Entries"),
                    Component.literal("One entry per line")
            );
            this.editor.setCharacterLimit(32767);
            this.editor.setValue(String.join("\n", this.initialValues));
            this.addRenderableWidget(this.editor);

            int bottomY = this.height - 28;
            this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
                        List<String> values = this.editor.getValue()
                                .lines()
                                .map(String::trim)
                                .filter(line -> !line.isEmpty())
                                .collect(Collectors.toCollection(ArrayList::new));
                        this.saveConsumer.accept(values);
                        this.minecraft.setScreen(this.parent);
                    })
                    .bounds(this.width / 2 - 142, bottomY, 140, 20)
                    .build());

            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.minecraft.setScreen(this.parent))
                    .bounds(this.width / 2 + 2, bottomY, 140, 20)
                    .build());

            this.setInitialFocus(this.editor);
        }

        @Override
        public void tick() {
            this.editor.tick();
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(this.parent);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, Component.literal(this.description), this.width / 2, 30, 0xA0A0A0);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
