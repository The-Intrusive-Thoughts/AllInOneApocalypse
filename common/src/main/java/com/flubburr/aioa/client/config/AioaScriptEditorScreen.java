package com.flubburr.aioa.client.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class AioaScriptEditorScreen extends AioaAnimatedScreen {
    private static final int LINE_COUNT = 10;
    private final Screen parent;
    private final Consumer<String> onDone;
    private final String initialScript;
    private final List<EditBox> lines = new ArrayList<>();

    AioaScriptEditorScreen(Screen parent, String initialScript, Consumer<String> onDone) {
        super(Component.literal("AIOA Creator Script"));
        this.parent = parent;
        this.initialScript = initialScript == null ? "" : initialScript;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.lines.clear();
        int panelWidth = Math.min(760, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(44, (this.height - 350) / 2);
        String[] commands = this.initialScript.split(";", -1);
        for (int i = 0; i < LINE_COUNT; i++) {
            EditBox line = new EditBox(this.font, left + 126, top + 46 + i * 25, panelWidth - 150, 21,
                    Component.literal("Script command " + (i + 1)));
            line.setMaxLength(160);
            line.setHint(Component.literal(i == 0 ? "say={mob} started" : "command=value"));
            if (i < commands.length) line.setValue(commands[i].trim());
            this.lines.add(this.addRenderableWidget(line));
        }
        this.addRenderableWidget(AioaScreenUtil.button(left + 18, top + 304, panelWidth - 36, "Done", button -> saveAndClose()));
    }

    private void saveAndClose() {
        String script = this.lines.stream().map(EditBox::getValue).map(String::trim).filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.joining("; "));
        this.onDone.accept(script.substring(0, Math.min(1024, script.length())));
        this.transitionTo(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(graphics);
        AioaScreenUtil.drawScreenBackground(graphics, this.width, this.height);
        int panelWidth = Math.min(760, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(44, (this.height - 350) / 2);
        AioaScreenUtil.drawPanel(graphics, left, top, left + panelWidth, top + 338);
        graphics.drawString(this.font, "SCRIPT NODE  :: one safe command per line", left + 18, top + 14, AioaScreenUtil.TEXT_MAIN);
        graphics.drawString(this.font, "say  rotate  glow  aggressive  stop", left + 18, top + 29, 0xFF9AD6AE);
        for (int i = 0; i < this.lines.size(); i++) {
            String command = this.lines.get(i).getValue();
            int y = top + 46 + i * 25;
            int color = scriptColor(command);
            graphics.fill(left + 18, y, left + 24, y + 21, color);
            graphics.drawString(this.font, String.format(Locale.ROOT, "%02d", i + 1), left + 34, y + 7, color);
            String token = command.contains("=") ? command.substring(0, command.indexOf('=')) : command;
            graphics.drawString(this.font, this.font.plainSubstrByWidth(token.trim(), 62), left + 58, y + 7, color);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        this.finishUiRender(graphics);
    }

    private static int scriptColor(String command) {
        String name = command.contains("=") ? command.substring(0, command.indexOf('=')) : command;
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "say" -> 0xFF59C7FF;
            case "rotate" -> 0xFFFFC857;
            case "glow" -> 0xFF8DFF9D;
            case "aggressive" -> 0xFFFF7777;
            case "stop" -> 0xFFC6A8FF;
            default -> 0xFF7E9B89;
        };
    }

    @Override
    public void onClose() { this.transitionTo(this.parent); }
}
