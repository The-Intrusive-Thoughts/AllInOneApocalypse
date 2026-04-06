package com.flubburr.aioa.forge;

import com.flubburr.aioa.AioaCommon;
import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.forge.config.AioaForgeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(AioaConstants.MOD_ID)
public final class AioaForge {

    public AioaForge() {
        AioaCommon.init();
        TickEvent.LevelTickEvent.Post.BUS.addListener(this::onLevelTick);

        if (FMLEnvironment.dist.isClient()) {
            AioaForgeClient.registerConfigScreen();
            TickEvent.ClientTickEvent.Post.BUS.addListener(this::onClientTick);
        }
    }

    private void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.level().isClientSide()) {
            return;
        }

        if (event.level() instanceof ServerLevel serverLevel) {
            AioaCommon.onServerLevelTick(serverLevel);
        }
    }

    private void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (AioaForgeClient.openConfigKey().consumeClick()) {
            if (minecraft.player != null && minecraft.player.isCreative()) {
                minecraft.setScreen(AioaForgeClient.createScreen(minecraft.screen));
            }
        }
    }
}
