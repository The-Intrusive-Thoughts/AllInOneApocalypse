package com.flubburr.aioa.fabric;

import net.fabricmc.api.ClientModInitializer;

/**
 * 26.x beta client bootstrap. Gameplay, graph execution, networking and spawning are available;
 * the editor renderer is being migrated to Minecraft's extracted GUI render-state API.
 */
public final class AioaFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Intentionally renderer-free for the first unobfuscated 26.x beta.
    }
}
