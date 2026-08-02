package com.flubburr.aioa.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class AioaAnimatedScreen extends Screen {

    private static final int TRANSITION_TICKS = 1;
    private static final int OPEN_OVERLAY_ALPHA = 28;
    private static final int CLOSE_OVERLAY_ALPHA = 20;

    private int openTicks;
    private int closingTicks = -1;
    private Screen pendingScreen;
    private boolean skipOpenTransition;

    protected AioaAnimatedScreen(Component title) {
        super(title);
    }

    protected final void transitionTo(Screen screen) {
        if (this.minecraft == null || this.closingTicks >= 0) {
            return;
        }
        if (screen instanceof AioaAnimatedScreen animatedScreen) {
            animatedScreen.skipOpenTransition = true;
        }
        this.pendingScreen = screen;
        this.closingTicks = TRANSITION_TICKS + 1;
    }

    protected final boolean isTransitionClosing() {
        return this.closingTicks >= 0;
    }

    protected final boolean canEditServerAffectingConfig() {
        return this.minecraft == null || this.minecraft.hasSingleplayerServer() || this.minecraft.getCurrentServer() == null;
    }

    protected final float transitionVolumeFactor() {
        if (this.closingTicks >= 0) {
            return Math.max(0.0F, Math.min(1.0F, this.closingTicks / (float) TRANSITION_TICKS));
        }
        if (this.skipOpenTransition) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, this.openTicks / (float) TRANSITION_TICKS));
    }

    protected final void beginUiRender(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushMatrix();
    }

    protected final void finishUiRender(GuiGraphics guiGraphics) {
        guiGraphics.pose().popMatrix();
        int scrimColor = this.transitionScrimColor();
        if ((scrimColor >>> 24) > 0) {
            guiGraphics.fillGradient(0, 0, this.width, this.height, scrimColor, scrimColor);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // AIOA owns its opaque backdrop. Suppressing vanilla's post-process path avoids
        // blur stacking, framebuffer conflicts, and background resizing with UI mods.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.skipOpenTransition) {
            this.openTicks = TRANSITION_TICKS;
            this.skipOpenTransition = false;
        }
        if (this.openTicks < TRANSITION_TICKS) {
            this.openTicks++;
        }
        if (this.closingTicks >= 0) {
            if (this.closingTicks == 0) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(this.pendingScreen);
                }
                this.closingTicks = -1;
                this.pendingScreen = null;
            } else {
                this.closingTicks--;
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.closingTicks < 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.closingTicks >= 0) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private float closeProgress() {
        return 1.0F - (Math.min(TRANSITION_TICKS, this.closingTicks) / (float) TRANSITION_TICKS);
    }

    private int transitionScrimColor() {
        if (this.closingTicks >= 0) {
            int alpha = Math.round(CLOSE_OVERLAY_ALPHA * this.closeProgress());
            return (alpha << 24) | 0x0010140F;
        }
        if (this.skipOpenTransition) {
            return 0;
        }
        float openProgress = Math.min(1.0F, this.openTicks / (float) TRANSITION_TICKS);
        int alpha = Math.round(OPEN_OVERLAY_ALPHA * (1.0F - openProgress));
        return (alpha << 24) | 0x00060805;
    }
}
