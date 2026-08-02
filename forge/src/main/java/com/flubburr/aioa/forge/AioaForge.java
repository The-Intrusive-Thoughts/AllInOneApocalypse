package com.flubburr.aioa.forge;

import com.flubburr.aioa.AioaCommon;
import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.forge.config.AioaForgeClient;
import com.flubburr.aioa.network.AioaSpawnRequest;
import com.flubburr.aioa.spawn.AioaSpawnStudioHandler;
import com.flubburr.aioa.behavior.AioaGraphUpdateHandler;
import com.flubburr.aioa.network.AioaGraphUpdateRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

@Mod(AioaConstants.MOD_ID)
public final class AioaForge {

    private static final int NETWORK_VERSION = 1;
    public static final SimpleChannel NETWORK = ChannelBuilder
            .named(new net.minecraft.resources.ResourceLocation(AioaConstants.MOD_ID, "main"))
            .networkProtocolVersion(NETWORK_VERSION)
            .acceptedVersions(Channel.VersionTest.exact(NETWORK_VERSION))
            .simpleChannel();

    public AioaForge() {
        AioaCommon.init();
        NETWORK.messageBuilder(AioaSpawnRequest.class, 0)
                .encoder(AioaForge::encodeSpawnRequest)
                .decoder(AioaForge::decodeSpawnRequest)
                .consumerMainThread(AioaForge::handleSpawnRequest)
                .add();
        NETWORK.messageBuilder(AioaGraphUpdateRequest.class, 1)
                .encoder((request, buffer) -> buffer.writeUtf(request.graphJson(), 65_536))
                .decoder(buffer -> new AioaGraphUpdateRequest(buffer.readUtf(65_536)))
                .consumerMainThread(AioaForge::handleGraphUpdate)
                .add();
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTick);

        if (FMLEnvironment.dist.isClient()) {
            AioaForgeClient.registerConfigScreen();
            MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        }
    }

    private static void encodeSpawnRequest(AioaSpawnRequest request, net.minecraft.network.FriendlyByteBuf buffer) {
        buffer.writeUtf(request.entityId(), 128);
        buffer.writeDouble(request.x());
        buffer.writeDouble(request.y());
        buffer.writeDouble(request.z());
        buffer.writeBoolean(request.noAi());
        buffer.writeBoolean(request.facePlayer());
        buffer.writeBoolean(request.persistent());
    }

    private static AioaSpawnRequest decodeSpawnRequest(net.minecraft.network.FriendlyByteBuf buffer) {
        return new AioaSpawnRequest(buffer.readUtf(128), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    private static void handleSpawnRequest(AioaSpawnRequest request, CustomPayloadEvent.Context context) {
        if (context.getSender() != null) {
            AioaSpawnStudioHandler.handle(context.getSender(), request);
        }
        context.setPacketHandled(true);
    }

    private static void handleGraphUpdate(AioaGraphUpdateRequest request, CustomPayloadEvent.Context context) {
        if (context.getSender() != null) {
            AioaGraphUpdateHandler.handle(context.getSender(), request);
        }
        context.setPacketHandled(true);
    }

    private void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }

        if (event.level instanceof ServerLevel serverLevel) {
            AioaCommon.onServerLevelTick(serverLevel);
        }
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        com.flubburr.aioa.client.config.AioaMobSelectionController.tick(minecraft);
        while (AioaForgeClient.openConfigKey().consumeClick()) {
            if (minecraft.player != null && minecraft.player.isCreative()) {
                minecraft.setScreen(AioaForgeClient.createScreen(minecraft.screen));
            }
        }
        while (AioaForgeClient.openSpawnStudioKey().consumeClick()) {
            if (com.flubburr.aioa.client.config.AioaMobSelectionController.isArmed()) {
                com.flubburr.aioa.client.config.AioaMobSelectionController.cancel(minecraft, "AIOA mob selection cancelled.");
                continue;
            }
            if (minecraft.player != null && minecraft.player.isCreative()) {
                minecraft.setScreen(com.flubburr.aioa.client.config.AioaSpawnStudioScreen.create(minecraft.screen));
            }
        }
    }
}
