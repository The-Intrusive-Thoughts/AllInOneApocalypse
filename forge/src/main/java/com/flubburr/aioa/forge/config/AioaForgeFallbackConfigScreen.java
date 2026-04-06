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
        super(tr("aioa.forge.fallback.title"));
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

        this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.hostile_settings"), button ->
                        this.minecraft.setScreen(new HostileSettingsScreen(this, this.editableConfig)))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step;

        this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.day_settings"), button ->
                        this.minecraft.setScreen(new DaySurfaceSettingsScreen(this, this.editableConfig)))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step;

        this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.hostile_allowlist"), button ->
                        this.minecraft.setScreen(new DelimitedListScreen(
                                this,
                                tr("aioa.forge.fallback.list.hostile_allowlist.title"),
                                tr("aioa.forge.fallback.list.hostile_allowlist.desc"),
                                this.editableConfig.hostileSpawnControl.whitelistEntityIds,
                                value -> this.editableConfig.hostileSpawnControl.whitelistEntityIds = new ArrayList<>(value)
                        )))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step;

        this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.allowed_biomes"), button ->
                        this.minecraft.setScreen(new DelimitedListScreen(
                                this,
                                tr("aioa.forge.fallback.list.allowed_biomes.title"),
                                tr("aioa.forge.fallback.list.allowed_biomes.desc"),
                                this.editableConfig.daySurfaceSpawns.allowedBiomeIds,
                                value -> this.editableConfig.daySurfaceSpawns.allowedBiomeIds = new ArrayList<>(value)
                        )))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step;

        this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.day_pool"), button ->
                        this.minecraft.setScreen(new DelimitedListScreen(
                                this,
                                tr("aioa.forge.fallback.list.day_pool.title"),
                                tr("aioa.forge.fallback.list.day_pool.desc"),
                                this.editableConfig.daySurfaceSpawns.spawnPoolEntries,
                                value -> this.editableConfig.daySurfaceSpawns.spawnPoolEntries = new ArrayList<>(value)
                        )))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());
        y += step + 4;

        int bottomY = this.height - 28;
        this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.reset_defaults"), button ->
                        this.minecraft.setScreen(new AioaForgeFallbackConfigScreen(this.parent, AioaConfig.createDefault())))
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.save"), button -> {
                    this.editableConfig = this.editableConfig.sanitize();
                    AioaConfigManager.save(this.editableConfig);
                    this.onClose();
                })
                .bounds(centerX - 142, bottomY, 140, 20)
                .build());

        this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.cancel"), button -> this.onClose())
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
                tr("aioa.forge.fallback.subtitle"),
                this.width / 2,
                28,
                0xA0A0A0
        );
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static Component booleanLabel(String labelKey, boolean value) {
        return Component.translatable(
                "aioa.forge.fallback.toggle_format",
                tr(labelKey),
                value ? tr("aioa.common.on") : tr("aioa.common.off")
        );
    }

    private static Component tr(String key) {
        return Component.translatable(key);
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
            super(tr("aioa.forge.fallback.hostile.title"));
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

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.hostile.enabled", this.enabled), button -> {
                        this.enabled = !this.enabled;
                        button.setMessage(booleanLabel("aioa.forge.fallback.hostile.enabled", this.enabled));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.hostile.overworld_only", this.overworldOnly), button -> {
                        this.overworldOnly = !this.overworldOnly;
                        button.setMessage(booleanLabel("aioa.forge.fallback.hostile.overworld_only", this.overworldOnly));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.hostile.ignore_structure_spawns", this.ignoreStructureSpawns), button -> {
                        this.ignoreStructureSpawns = !this.ignoreStructureSpawns;
                        button.setMessage(booleanLabel("aioa.forge.fallback.hostile.ignore_structure_spawns", this.ignoreStructureSpawns));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.hostile.ignore_spawner_spawns", this.ignoreSpawnerSpawns), button -> {
                        this.ignoreSpawnerSpawns = !this.ignoreSpawnerSpawns;
                        button.setMessage(booleanLabel("aioa.forge.fallback.hostile.ignore_spawner_spawns", this.ignoreSpawnerSpawns));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.hostile.ignore_special_spawns", this.ignoreSpecialSpawns), button -> {
                        this.ignoreSpecialSpawns = !this.ignoreSpecialSpawns;
                        button.setMessage(booleanLabel("aioa.forge.fallback.hostile.ignore_special_spawns", this.ignoreSpecialSpawns));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());

            int bottomY = this.height - 28;
            this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.done"), button -> {
                        this.editableConfig.hostileSpawnControl.enabled = this.enabled;
                        this.editableConfig.hostileSpawnControl.overworldOnly = this.overworldOnly;
                        this.editableConfig.hostileSpawnControl.ignoreStructureSpawns = this.ignoreStructureSpawns;
                        this.editableConfig.hostileSpawnControl.ignoreSpawnerSpawns = this.ignoreSpawnerSpawns;
                        this.editableConfig.hostileSpawnControl.ignoreSpecialSpawns = this.ignoreSpecialSpawns;
                        this.minecraft.setScreen(this.parent);
                    })
                    .bounds(centerX - 142, bottomY, 140, 20)
                    .build());

            this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.cancel"), button -> this.minecraft.setScreen(this.parent))
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
        private boolean removeBabyVariants;
        private boolean exportMobCatalog;

        private EditBox spawnIntervalTicks;
        private EditBox spawnAttemptsPerPlayer;
        private EditBox minSpawnDistance;
        private EditBox maxSpawnDistance;
        private EditBox maxNearbyManagedMobs;

        private DaySurfaceSettingsScreen(Screen parent, AioaConfig editableConfig) {
            super(tr("aioa.forge.fallback.day.title"));
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
            this.removeBabyVariants = this.editableConfig.daySurfaceSpawns.removeBabyVariants;
            this.exportMobCatalog = this.editableConfig.daySurfaceSpawns.exportMobCatalog;

            int centerX = this.width / 2;
            int w = 300;
            int y = 34;
            int step = 20;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.day.enabled", this.enabled), button -> {
                        this.enabled = !this.enabled;
                        button.setMessage(booleanLabel("aioa.forge.fallback.day.enabled", this.enabled));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.day.overworld_only", this.overworldOnly), button -> {
                        this.overworldOnly = !this.overworldOnly;
                        button.setMessage(booleanLabel("aioa.forge.fallback.day.overworld_only", this.overworldOnly));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.day.require_daytime", this.requireDaytime), button -> {
                        this.requireDaytime = !this.requireDaytime;
                        button.setMessage(booleanLabel("aioa.forge.fallback.day.require_daytime", this.requireDaytime));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.day.require_clear_sky", this.requireClearSky), button -> {
                        this.requireClearSky = !this.requireClearSky;
                        button.setMessage(booleanLabel("aioa.forge.fallback.day.require_clear_sky", this.requireClearSky));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.day.prevent_sunlight_burn", this.preventSunlightBurn), button -> {
                        this.preventSunlightBurn = !this.preventSunlightBurn;
                        button.setMessage(booleanLabel("aioa.forge.fallback.day.prevent_sunlight_burn", this.preventSunlightBurn));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.day.remove_baby_variants", this.removeBabyVariants), button -> {
                        this.removeBabyVariants = !this.removeBabyVariants;
                        button.setMessage(booleanLabel("aioa.forge.fallback.day.remove_baby_variants", this.removeBabyVariants));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step + 8;

            this.addRenderableWidget(Button.builder(booleanLabel("aioa.forge.fallback.day.export_mob_catalog", this.exportMobCatalog), button -> {
                        this.exportMobCatalog = !this.exportMobCatalog;
                        button.setMessage(booleanLabel("aioa.forge.fallback.day.export_mob_catalog", this.exportMobCatalog));
                    })
                    .bounds(centerX - w / 2, y, w, 20)
                    .build());
            y += step;

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

            this.spawnIntervalTicks.setHint(tr("aioa.forge.fallback.day.hint.interval"));
            this.spawnAttemptsPerPlayer.setHint(tr("aioa.forge.fallback.day.hint.attempts"));
            this.minSpawnDistance.setHint(tr("aioa.forge.fallback.day.hint.min_distance"));
            this.maxSpawnDistance.setHint(tr("aioa.forge.fallback.day.hint.max_distance"));
            this.maxNearbyManagedMobs.setHint(tr("aioa.forge.fallback.day.hint.max_nearby"));

            int bottomY = this.height - 28;
            this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.done"), button -> {
                        this.editableConfig.daySurfaceSpawns.enabled = this.enabled;
                        this.editableConfig.daySurfaceSpawns.overworldOnly = this.overworldOnly;
                        this.editableConfig.daySurfaceSpawns.requireDaytime = this.requireDaytime;
                        this.editableConfig.daySurfaceSpawns.requireClearSky = this.requireClearSky;
                        this.editableConfig.daySurfaceSpawns.preventSunlightBurn = this.preventSunlightBurn;
                        this.editableConfig.daySurfaceSpawns.removeBabyVariants = this.removeBabyVariants;
                        this.editableConfig.daySurfaceSpawns.exportMobCatalog = this.exportMobCatalog;

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

            this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.cancel"), button -> this.minecraft.setScreen(this.parent))
                    .bounds(centerX + 2, bottomY, 140, 20)
                    .build());

            this.setInitialFocus(this.spawnIntervalTicks);
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
            int y = 162;
            int step = 20;
            int color = 0xA0A0A0;
            guiGraphics.drawString(this.font, tr("aioa.forge.fallback.day.label.spawn_interval_ticks"), labelX, y + 6, color);
            guiGraphics.drawString(this.font, tr("aioa.forge.fallback.day.label.spawn_attempts_per_player"), labelX, y + step + 6, color);
            guiGraphics.drawString(this.font, tr("aioa.forge.fallback.day.label.min_spawn_distance"), labelX, y + (step * 2) + 6, color);
            guiGraphics.drawString(this.font, tr("aioa.forge.fallback.day.label.max_spawn_distance"), labelX, y + (step * 3) + 6, color);
            guiGraphics.drawString(this.font, tr("aioa.forge.fallback.day.label.max_nearby_managed_mobs"), labelX, y + (step * 4) + 6, color);

            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        private EditBox createNumberBox(int x, int y, int width, int value) {
            EditBox box = new EditBox(this.font, x, y, width, 20, tr("aioa.forge.fallback.day.number_box"));
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
        private final Component description;
        private final List<String> initialValues;
        private final Consumer<List<String>> saveConsumer;
        private MultiLineEditBox editor;

        private DelimitedListScreen(
                Screen parent,
                Component title,
                Component description,
                List<String> initialValues,
                Consumer<List<String>> saveConsumer
        ) {
            super(title);
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
                    tr("aioa.forge.fallback.list.entries"),
                    tr("aioa.forge.fallback.list.entries_hint")
            );
            this.editor.setCharacterLimit(32767);
            this.editor.setValue(String.join("\n", this.initialValues));
            this.addRenderableWidget(this.editor);

            int bottomY = this.height - 28;
            this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.done"), button -> {
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

            this.addRenderableWidget(Button.builder(tr("aioa.forge.fallback.button.cancel"), button -> this.minecraft.setScreen(this.parent))
                    .bounds(this.width / 2 + 2, bottomY, 140, 20)
                    .build());

            this.setInitialFocus(this.editor);
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(this.parent);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, this.description, this.width / 2, 30, 0xA0A0A0);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
