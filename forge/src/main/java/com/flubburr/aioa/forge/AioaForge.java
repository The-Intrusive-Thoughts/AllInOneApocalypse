package com.flubburr.aioa.forge;

import com.flubburr.aioa.AioaCommon;
import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.forge.config.AioaForgeClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(AioaConstants.MOD_ID)
public final class AioaForge {

    public AioaForge() {
        AioaCommon.init();
        TickEvent.LevelTickEvent.BUS.addListener(this::onLevelTick);

        if (FMLEnvironment.dist.isClient()) {
            AioaForgeClient.registerConfigScreen();
        }
    }

    private void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }

        if (event.level instanceof ServerLevel serverLevel) {
            AioaCommon.onServerLevelTick(serverLevel);
        }
    }
}
