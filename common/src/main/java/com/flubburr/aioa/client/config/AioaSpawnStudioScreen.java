package com.flubburr.aioa.client.config;

import com.flubburr.aioa.network.AioaClientNetworking;
import com.flubburr.aioa.network.AioaSpawnRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;

public final class AioaSpawnStudioScreen extends Screen {
    private final Screen parent;
    private Identifier entityId = Identifier.fromNamespaceAndPath("minecraft", "zombie");
    private Button mobButton;
    private boolean noAi;
    private boolean facePlayer = true;
    private boolean persistent = true;
    private Button noAiButton;
    private Button facePlayerButton;
    private Button persistentButton;

    private AioaSpawnStudioScreen(Screen parent) {
        super(Component.literal("AIOA Spawn Studio"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new AioaSpawnStudioScreen(parent);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int panelWidth = AioaScreenUtil.panelWidth(this.width, 520);
        int left = (this.width - panelWidth) / 2;
        int fieldWidth = panelWidth - 44;
        int x = left + 22;
        int y = Math.max(82, (this.height - 264) / 2 + 48);

        this.mobButton = this.addRenderableWidget(AioaScreenUtil.button(x, y, fieldWidth,
                "Mob: " + AioaScreenUtil.entityDisplayName(this.entityId), button -> Minecraft.getInstance().setScreen(
                        new AioaEntityPickerScreen(this, "Choose Spawn Mob", AioaScreenUtil.allEntityIds(),
                                "Search translated mob names and use the live preview. Registry ids stay hidden.", "Done", id -> {
                            this.entityId = id;
                            this.mobButton.setMessage(Component.literal("Mob: " + AioaScreenUtil.entityDisplayName(id)));
                        }))));
        y += 34;

        this.noAiButton = this.addRenderableWidget(AioaScreenUtil.button(x, y, fieldWidth, AioaScreenUtil.boolLabel("No AI", this.noAi), button -> {
            this.noAi = !this.noAi;
            this.refreshLabels();
        }));
        y += 32;
        this.facePlayerButton = this.addRenderableWidget(AioaScreenUtil.button(x, y, fieldWidth, AioaScreenUtil.boolLabel("Face player", this.facePlayer), button -> {
            this.facePlayer = !this.facePlayer;
            this.refreshLabels();
        }));
        y += 32;
        this.persistentButton = this.addRenderableWidget(AioaScreenUtil.button(x, y, fieldWidth, AioaScreenUtil.boolLabel("Persistent", this.persistent), button -> {
            this.persistent = !this.persistent;
            this.refreshLabels();
        }));
        y += 40;

        this.addRenderableWidget(AioaScreenUtil.button(x, y, (fieldWidth - 10) / 2, "Spawn at cursor", button -> this.spawnAtCursor()));
        this.addRenderableWidget(AioaScreenUtil.button(x + ((fieldWidth - 10) / 2) + 10, y, (fieldWidth - 10) / 2, "Done", button -> this.onClose()));
    }

    private void refreshLabels() {
        this.noAiButton.setMessage(Component.literal(AioaScreenUtil.boolLabel("No AI", this.noAi)));
        this.facePlayerButton.setMessage(Component.literal(AioaScreenUtil.boolLabel("Face player", this.facePlayer)));
        this.persistentButton.setMessage(Component.literal(AioaScreenUtil.boolLabel("Persistent", this.persistent)));
    }

    private void spawnAtCursor() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Vec3 target;
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS) {
            target = minecraft.hitResult.getLocation().add(0.0D, 0.05D, 0.0D);
        } else {
            target = minecraft.player.getEyePosition().add(minecraft.player.getLookAngle().scale(5.0D));
        }
        AioaClientNetworking.sendSpawnRequest(new AioaSpawnRequest(
                this.entityId.toString(), target.x, target.y, target.z, this.noAi, this.facePlayer, this.persistent
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AioaScreenUtil.drawScreenBackground(guiGraphics, this.width, this.height);
        int panelWidth = AioaScreenUtil.panelWidth(this.width, 520);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(24, (this.height - 264) / 2);
        AioaScreenUtil.drawPanel(guiGraphics, left, top, left + panelWidth, Math.min(this.height - 24, top + 264));
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 14, AioaScreenUtil.TEXT_MAIN);
        AioaScreenUtil.drawWrappedCenteredText(guiGraphics, this.font,
                Component.literal("Creative tool: configure a mob, aim at a block, and spawn a repeatable shot-ready instance."),
                this.width / 2, top + 30, panelWidth - 52, AioaScreenUtil.TEXT_SUB);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
