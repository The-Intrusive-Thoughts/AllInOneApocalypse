package com.flubburr.aioa.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public final class AioaMobSelectionController {
    private static AioaBehaviorEditorScreen pendingEditor;
    private static long expiresAt;
    private static boolean wasRightDown;

    private AioaMobSelectionController() {
    }

    static void arm(AioaBehaviorEditorScreen editor) {
        pendingEditor = editor;
        expiresAt = System.currentTimeMillis() + 30_000L;
        wasRightDown = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("AIOA picker: right-click a mob within 30 seconds. Press F7 to cancel."), true);
        }
        minecraft.setScreen(null);
    }

    public static void tick(Minecraft minecraft) {
        if (pendingEditor == null || minecraft.player == null) return;
        if (System.currentTimeMillis() > expiresAt) {
            cancel(minecraft, "AIOA mob selection timed out.");
            return;
        }
        if (minecraft.screen != null) return;
        boolean rightDown = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (rightDown && !wasRightDown && minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Mob mob) {
            AioaBehaviorEditorScreen editor = pendingEditor;
            pendingEditor = null;
            editor.bindWorldSelectedMob(mob);
            minecraft.setScreen(editor);
        }
        wasRightDown = rightDown;
    }

    public static void cancel(Minecraft minecraft, String message) {
        AioaBehaviorEditorScreen editor = pendingEditor;
        pendingEditor = null;
        wasRightDown = false;
        if (minecraft.player != null) minecraft.player.displayClientMessage(Component.literal(message), true);
        if (editor != null && minecraft.screen == null) minecraft.setScreen(editor);
    }

    public static boolean isArmed() {
        return pendingEditor != null;
    }
}
