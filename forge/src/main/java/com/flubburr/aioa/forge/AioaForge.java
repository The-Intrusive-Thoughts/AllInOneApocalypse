package com.flubburr.aioa.forge;

import com.flubburr.aioa.AioaCommon;
import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.forge.config.AioaForgeClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

@Mod(AioaConstants.MOD_ID)
public final class AioaForge {

    public AioaForge() {
        AioaCommon.init();
        registerLevelTickListener();

        if (FMLEnvironment.dist.isClient()) {
            AioaForgeClient.registerConfigScreen();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerLevelTickListener() {
        Consumer listener = this::onLevelTickEvent;

        try {
            Class<?> postEventClass = Class.forName("net.minecraftforge.event.TickEvent$LevelTickEvent$Post");
            Field postBusField = postEventClass.getField("BUS");
            Object postBus = postBusField.get(null);
            Method addListener = postBus.getClass().getMethod("addListener", Consumer.class);
            addListener.invoke(postBus, listener);
            return;
        } catch (Exception ignored) {
        }

        try {
            Class<?> levelTickEventClass = Class.forName("net.minecraftforge.event.TickEvent$LevelTickEvent");
            Field busField = levelTickEventClass.getField("BUS");
            Object bus = busField.get(null);
            Method addListener = bus.getClass().getMethod("addListener", Consumer.class);
            addListener.invoke(bus, listener);
            return;
        } catch (Exception exception) {
            throw new IllegalStateException("AIOA could not register a Forge level tick listener for this Forge version.", exception);
        }
    }

    private void onLevelTickEvent(Object event) {
        ServerLevel serverLevel = extractServerLevel(event);
        if (serverLevel != null) {
            AioaCommon.onServerLevelTick(serverLevel);
        }
    }

    private static ServerLevel extractServerLevel(Object event) {
        try {
            Method levelMethod = event.getClass().getMethod("level");
            Object level = levelMethod.invoke(event);
            return level instanceof ServerLevel serverLevel ? serverLevel : null;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception exception) {
            AioaConstants.LOG.debug("AIOA failed to inspect Forge tick event via level() method.", exception);
            return null;
        }

        try {
            Field phaseField = event.getClass().getField("phase");
            Object phase = phaseField.get(event);
            if (phase == null || !"END".equals(String.valueOf(phase))) {
                return null;
            }

            Field levelField = event.getClass().getField("level");
            Object level = levelField.get(event);
            return level instanceof ServerLevel serverLevel ? serverLevel : null;
        } catch (Exception exception) {
            AioaConstants.LOG.debug("AIOA failed to inspect legacy Forge tick event fields.", exception);
            return null;
        }
    }
}
