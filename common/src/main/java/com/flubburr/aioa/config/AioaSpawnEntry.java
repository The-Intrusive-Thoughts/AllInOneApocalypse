package com.flubburr.aioa.config;

import com.flubburr.aioa.compat.AioaEntityHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public record AioaSpawnEntry(
        String rawEntry,
        ResourceLocation entityId,
        boolean enabled,
        int weight,
        double chance,
        int minGroupSize,
        int maxGroupSize
) {

    public static Optional<AioaSpawnEntry> parse(String rawEntry, Consumer<String> warningConsumer) {
        String trimmed = rawEntry == null ? "" : rawEntry.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        String[] segments = trimmed.split(";");
        Optional<ResourceLocation> resolvedEntityId = AioaEntityHelper.resolveEntityId(segments[0].trim(), warningConsumer);
        if (resolvedEntityId.isEmpty()) {
            warningConsumer.accept("Ignoring malformed AIOA spawn entry '" + trimmed + "': the mob selector did not resolve.");
            return Optional.empty();
        }
        ResourceLocation entityId = resolvedEntityId.get();

        boolean enabled = true;
        int weight = 10;
        double chance = 1.0D;
        int minGroupSize = 1;
        int maxGroupSize = 3;

        for (int index = 1; index < segments.length; index++) {
            String segment = segments[index].trim();
            if (segment.isEmpty()) {
                continue;
            }

            int separator = segment.indexOf('=');
            if (separator < 0) {
                warningConsumer.accept("Ignoring malformed AIOA spawn entry option '" + segment + "' in '" + trimmed + "'.");
                continue;
            }

            String key = segment.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = segment.substring(separator + 1).trim();

            try {
                switch (key) {
                    case "enabled" -> enabled = Boolean.parseBoolean(value);
                    case "weight" -> weight = Math.max(1, Integer.parseInt(value));
                    case "rarity" -> weight = parseRarityWeight(value);
                    case "chance", "spawnchance", "spawn_chance" -> chance = Mth.clamp(Double.parseDouble(value), 0.0D, 1.0D);
                    case "min", "mingroup", "min_group" -> minGroupSize = Math.max(1, Integer.parseInt(value));
                    case "max", "maxgroup", "max_group" -> maxGroupSize = Math.max(1, Integer.parseInt(value));
                    default -> warningConsumer.accept("Ignoring unknown AIOA spawn option '" + key + "' in '" + trimmed + "'.");
                }
            } catch (NumberFormatException exception) {
                warningConsumer.accept("Ignoring invalid numeric AIOA spawn option '" + segment + "' in '" + trimmed + "'.");
            }
        }

        if (maxGroupSize < minGroupSize) {
            maxGroupSize = minGroupSize;
        }

        return Optional.of(new AioaSpawnEntry(trimmed, entityId, enabled, weight, chance, minGroupSize, maxGroupSize));
    }

    private static int parseRarityWeight(String rawValue) {
        String normalized = rawValue.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "very_common", "verycommon" -> 16;
            case "common" -> 12;
            case "uncommon" -> 8;
            case "rare" -> 4;
            case "very_rare", "veryrare", "epic" -> 2;
            case "legendary" -> 1;
            default -> Math.max(1, Integer.parseInt(rawValue));
        };
    }

    public String toConfigLine() {
        return "%s;enabled=%s;weight=%d;chance=%s;min=%d;max=%d".formatted(
                this.entityId,
                this.enabled,
                this.weight,
                this.chance,
                this.minGroupSize,
                this.maxGroupSize
        );
    }
}
