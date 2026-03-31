package com.flubburr.aioa.compat;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class AioaEntityHelper {

    private static final ConcurrentHashMap<ResourceLocation, MobClassification> CLASSIFICATION_CACHE = new ConcurrentHashMap<>();

    private AioaEntityHelper() {
    }

    public static Optional<EntityType<?>> resolveEntityType(String rawEntityId) {
        Optional<ResourceLocation> id = resolveEntityId(rawEntityId, warning -> {
        });
        if (id.isEmpty()) {
            return Optional.empty();
        }

        return resolveEntityType(id.get());
    }

    public static Optional<EntityType<?>> resolveEntityType(ResourceLocation entityId) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)
                ? Optional.of(BuiltInRegistries.ENTITY_TYPE.get(entityId))
                : Optional.empty();
    }

    public static Optional<ResourceLocation> resolveEntityId(String rawSelector, Consumer<String> warningConsumer) {
        String trimmed = rawSelector == null ? "" : rawSelector.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation directId = parseResourceLocation(trimmed);
        if (directId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(directId)) {
            return Optional.of(directId);
        }

        ResourceLocation embeddedId = tryParseEmbeddedId(trimmed);
        if (embeddedId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(embeddedId)) {
            return Optional.of(embeddedId);
        }

        String normalizedSelector = normalizeSelector(trimmed);
        List<ResourceLocation> matches = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .filter(id -> matchesSelector(normalizedSelector, id))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();

        if (matches.size() == 1) {
            return Optional.of(matches.get(0));
        }
        if (matches.size() > 1) {
            warningConsumer.accept("AIOA found multiple mob matches for '" + rawSelector + "'. Use a full id or 'Name (modid:id)' to disambiguate.");
        }
        return Optional.empty();
    }

    public static ResourceLocation parseResourceLocation(String rawId) {
        String trimmed = rawId == null ? "" : rawId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return new ResourceLocation(trimmed);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String describeEntity(ResourceLocation entityId) {
        return toFriendlyName(entityId) + " (" + entityId + ")";
    }

    public static boolean isConfigurableMob(EntityType<?> entityType, ServerLevel level) {
        return classify(entityType, level).configurableMob();
    }

    public static boolean isHostileMob(EntityType<?> entityType, ServerLevel level) {
        if (entityType.getCategory() == MobCategory.MONSTER) {
            return true;
        }

        return classify(entityType, level).hostile();
    }

    public static List<ResourceLocation> enumerateConfigurableMobIds(ServerLevel level) {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> isConfigurableMob(type, level))
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private static MobClassification classify(EntityType<?> entityType, ServerLevel level) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return CLASSIFICATION_CACHE.computeIfAbsent(id, ignored -> inspectEntityType(entityType, level, id));
    }

    private static MobClassification inspectEntityType(EntityType<?> entityType, ServerLevel level, ResourceLocation id) {
        try {
            Entity entity = entityType.create(level);
            if (!(entity instanceof Mob mob)) {
                return MobClassification.NONE;
            }

            return new MobClassification(true, mob instanceof Enemy);
        } catch (Exception exception) {
            AioaConfigManager.warnOnce(
                    "entity-inspection:" + id,
                    "AIOA could not inspect entity type '" + id + "'. It will be treated as unsupported for generic spawn control."
            );
            AioaConstants.LOG.debug("Entity inspection failed for {}", id, exception);
            return MobClassification.NONE;
        }
    }

    private static boolean matchesSelector(String normalizedSelector, ResourceLocation entityId) {
        return normalizedSelector.equals(normalizeSelector(entityId.toString()))
                || normalizedSelector.equals(normalizeSelector(entityId.getPath()))
                || normalizedSelector.equals(normalizeSelector(toFriendlyName(entityId)))
                || normalizedSelector.equals(normalizeSelector(entityId.getNamespace() + " " + entityId.getPath()))
                || normalizedSelector.equals(normalizeSelector(toFriendlyName(entityId) + " " + entityId));
    }

    private static ResourceLocation tryParseEmbeddedId(String rawSelector) {
        int open = Math.max(rawSelector.lastIndexOf('('), rawSelector.lastIndexOf('['));
        int close = Math.max(rawSelector.lastIndexOf(')'), rawSelector.lastIndexOf(']'));
        if (open < 0 || close <= open) {
            return null;
        }

        return parseResourceLocation(rawSelector.substring(open + 1, close).trim());
    }

    private static String normalizeSelector(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[_:\\-\\[\\]\\(\\),]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String toFriendlyName(ResourceLocation entityId) {
        String pathName = titleCase(entityId.getPath());
        if ("minecraft".equals(entityId.getNamespace())) {
            return pathName;
        }
        return titleCase(entityId.getNamespace()) + " " + pathName;
    }

    private static String titleCase(String value) {
        String[] parts = value.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private record MobClassification(boolean configurableMob, boolean hostile) {
        private static final MobClassification NONE = new MobClassification(false, false);
    }
}
