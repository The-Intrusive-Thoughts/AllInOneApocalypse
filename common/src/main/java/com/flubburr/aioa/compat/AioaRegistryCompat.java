package com.flubburr.aioa.compat;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Registry lookups that avoid linking common code to version-specific built-in
 * registry fields. This matters when the same common sources are recompiled by
 * several loader/version worktrees.
 */
public final class AioaRegistryCompat {

    private AioaRegistryCompat() {
    }

    @SuppressWarnings("unchecked")
    public static Registry<SoundEvent> soundEvents() {
        Object registry = BuiltInRegistries.REGISTRY.getValue(Registries.SOUND_EVENT.identifier());
        return registry instanceof Registry<?> found ? (Registry<SoundEvent>) found : null;
    }

    public static SoundEvent getSoundEvent(Identifier id) {
        Registry<SoundEvent> registry = soundEvents();
        return registry == null ? null : registry.getValue(id);
    }
}
