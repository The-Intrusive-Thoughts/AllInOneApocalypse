package com.flubburr.aioa.client.config;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

abstract class AioaScrollableScreen extends Screen {

    protected final List<AbstractWidget> scrollWidgets = new ArrayList<>();
    protected final List<Integer> scrollBaseY = new ArrayList<>();
    protected final List<Boolean> scrollShown = new ArrayList<>();
    protected int scrollOffset;
    protected int maxScroll;
    protected int panelLeft;
    protected int panelWidth;
    protected int contentTop;
    protected int contentBottom;

    protected AioaScrollableScreen(Component title) {
        super(title);
    }

    protected void resetScrollLayout(int maxPanelWidth, int contentTop, int contentBottom) {
        this.clearWidgets();
        this.scrollWidgets.clear();
        this.scrollBaseY.clear();
        this.scrollShown.clear();
        this.panelWidth = AioaScreenUtil.panelWidth(this.width, maxPanelWidth);
        this.panelLeft = AioaScreenUtil.panelLeft(this.width, this.panelWidth);
        this.contentTop = contentTop;
        this.contentBottom = contentBottom;
    }

    protected <T extends AbstractWidget> T addScrollable(T widget, int relativeY) {
        this.scrollWidgets.add(widget);
        this.scrollBaseY.add(relativeY);
        this.scrollShown.add(true);
        this.addRenderableWidget(widget);
        return widget;
    }

    protected void finishScrollLayout(int contentHeight) {
        this.maxScroll = Math.max(0, contentHeight - (this.contentBottom - this.contentTop));
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
        this.updateScrollLayout();
    }

    protected void updateScrollLayout() {
        for (int i = 0; i < this.scrollWidgets.size(); i++) {
            AbstractWidget widget = this.scrollWidgets.get(i);
            int y = this.contentTop + this.scrollBaseY.get(i) - this.scrollOffset;
            widget.setY(y);
            boolean shown = this.scrollShown.get(i);
            widget.visible = shown && y + widget.getHeight() >= this.contentTop && y <= this.contentBottom;
        }
    }

    protected void setScrollableRelativeY(AbstractWidget widget, int relativeY) {
        int index = this.scrollWidgets.indexOf(widget);
        if (index >= 0) {
            this.scrollBaseY.set(index, relativeY);
        }
    }

    protected void setScrollableShown(AbstractWidget widget, boolean shown) {
        int index = this.scrollWidgets.indexOf(widget);
        if (index >= 0) {
            this.scrollShown.set(index, shown);
        }
        widget.visible = shown;
    }

    protected boolean handleScroll(double delta, int step) {
        if (this.maxScroll <= 0) {
            return false;
        }
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll, this.scrollOffset - ((int) delta * step)));
        this.updateScrollLayout();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return this.handleScroll(delta, 28) || super.mouseScrolled(mouseX, mouseY, delta);
    }
}
