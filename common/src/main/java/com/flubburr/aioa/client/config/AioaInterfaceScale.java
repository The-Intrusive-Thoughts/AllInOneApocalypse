package com.flubburr.aioa.client.config;

import net.minecraft.client.Minecraft;

public final class AioaInterfaceScale {
    private static int lastScale;

    private AioaInterfaceScale() {}

    public static int requested(Minecraft client, int vanilla) {
        if (client.gui == null || !(client.screen instanceof AioaAnimatedScreen
                || client.screen instanceof AioaSpawnStudioScreen || client.screen instanceof AioaPresetScreen)) return vanilla;
        var window = client.getWindow();
        double density = Math.min(window.getWidth() / 820.0, window.getHeight() / 460.0);
        return Math.max(1, (int) Math.round(density));
    }

    static void update(Minecraft client) {
        int scale = requested(client, 0);
        if (scale == lastScale) return;
        lastScale = scale;
        // Use the same resize path as fullscreen so input, clipping and text share one pixel space.
        client.resizeDisplay();
    }
}
