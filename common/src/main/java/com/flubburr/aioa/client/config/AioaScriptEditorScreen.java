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
    private static final int LINE_COUNT = 16;
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
        int top = Math.max(24, (this.height - 500) / 2);
        String[] commands = this.initialScript.replace(';', '\n').split("\\R", -1);
        for (int i = 0; i < LINE_COUNT; i++) {
            EditBox line = new EditBox(this.font, left + 54, top + 68 + i * 24, panelWidth - 76, 20,
                    Component.literal("Code line " + (i + 1)));
            line.setMaxLength(512);
            line.setHint(Component.literal(i == 0 ? "let rage = 1" : i == 1 ? "if health_percent < 0.5 {" : i == 2 ? "  say(\"{mob} enraged!\")" : ""));
            if (i < commands.length) line.setValue(commands[i].trim());
            this.lines.add(this.addRenderableWidget(line));
        }
        this.addRenderableWidget(AioaScreenUtil.button(left + 18, top + 458, panelWidth - 36, "Done", button -> saveAndClose()));
    }

    private void saveAndClose() {
        String script = this.lines.stream().map(EditBox::getValue).map(String::trim).filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.joining("\n"));
        this.onDone.accept(script.substring(0, Math.min(8192, script.length())));
        this.transitionTo(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(graphics);
        AioaScreenUtil.drawScreenBackground(graphics, this.width, this.height);
        int panelWidth = Math.min(760, this.width - 32);
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(24, (this.height - 500) / 2);
        AioaScreenUtil.drawPanel(graphics, left, top, left + panelWidth, top + 492);
        graphics.drawString(this.font, "CREATOR SCRIPT  :: safe code runtime", left + 18, top + 14, AioaScreenUtil.TEXT_MAIN);
        graphics.drawString(this.font, "Variables: let name = value   Blocks: if condition { / else / }", left + 18, top + 30, 0xFF9AD6AE);
        graphics.drawString(this.font, "API: say  actionbar  rotate  glow  aggressive  stop  heal  damage_target  move_to_target  tag", left + 18, top + 44, 0xFF77BFEA);
        graphics.drawString(this.font, "Built-ins: health, health_percent, target_exists, target_distance, phase, tick", left + 18, top + 56, 0xFFBBA7F4);
        for (int i = 0; i < this.lines.size(); i++) {
            String command = this.lines.get(i).getValue();
            int y = top + 68 + i * 24;
            int color = scriptColor(command);
            graphics.fill(left + 18, y, left + 24, y + 21, color);
            graphics.drawString(this.font, String.format(Locale.ROOT, "%02d", i + 1), left + 28, y + 6, color);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        this.finishUiRender(graphics);
    }

    private static int scriptColor(String command) {
        String name = command.trim().split("[ (=]", 2)[0];
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "say", "actionbar" -> 0xFF59C7FF;
            case "let" -> 0xFFC6A8FF;
            case "if", "else", "}" -> 0xFFFFC857;
            case "glow", "heal", "tag" -> 0xFF8DFF9D;
            case "aggressive", "damage_target" -> 0xFFFF7777;
            case "rotate", "stop", "move_to_target" -> 0xFFBBA7F4;
            default -> 0xFF7E9B89;
        };
    }

    @Override
    public void onClose() { this.transitionTo(this.parent); }
}
