package com.flubburr.aioa.compat;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AioaEntityHelper {

    private static final ConcurrentHashMap<ResourceLocation, MobClassification> CLASSIFICATION_CACHE = new ConcurrentHashMap<>();

    private AioaEntityHelper() {
    }

    public static Optional<EntityType<?>> resolveEntityType(String rawEntityId) {
        ResourceLocation id = ResourceLocation.tryParse(rawEntityId == null ? "" : rawEntityId.trim());
        if (id == null) {
            return Optional.empty();
        }

        return resolveEntityType(id);
    }

    public static Optional<EntityType<?>> resolveEntityType(ResourceLocation entityId) {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)
                ? Optional.ofNullable(BuiltInRegistries.ENTITY_TYPE.getValue(entityId))
                : Optional.empty();
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
            Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);
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

    private record MobClassification(boolean configurableMob, boolean hostile) {
        private static final MobClassification NONE = new MobClassification(false, false);
    }
}
