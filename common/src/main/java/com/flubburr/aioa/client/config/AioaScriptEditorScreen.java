package com.flubburr.aioa.client.config;

import com.flubburr.aioa.behavior.AioaLuaScript;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.function.BiConsumer;

final class AioaScriptEditorScreen extends AioaAnimatedScreen {
    private final Screen parent;
    private final String draftKey;
    private final BiConsumer<String, String> onDone;
    private String source;
    private String language;
    private String result = "";
    private Path draft;
    private boolean dirty;
    private long saveAfter;

    AioaScriptEditorScreen(Screen parent, String draftKey, String language, String source, BiConsumer<String, String> onDone) {
        super(Component.literal("Mob script"));
        this.parent = parent;
        this.draftKey = draftKey;
        this.language = language;
        this.source = source;
        this.onDone = onDone;
    }

    private int editorWidth() { return Math.min(840, this.width - 24); }
    private int editorLeft() { return (this.width - editorWidth()) / 2; }

    @Override
    protected void init() {
        if (this.draft == null) loadDraft();
        this.clearWidgets();
        int left = editorLeft();
        int width = editorWidth();
        int footer = this.height - 38;
        Component hint = Component.literal("if target_exists then move_to_target(1.1) end");
        MultiLineEditBox code = new MultiLineEditBox(this.font, left + 10, 88, width - 20, footer - 112, hint, this.title);
        code.setCharacterLimit(AioaLuaScript.SOURCE_LIMIT);
        code.setValue(this.source);
        code.setValueListener(text -> {
            this.source = text;
            this.dirty = true;
            this.saveAfter = System.nanoTime() + 300_000_000L;
            this.result = "";
        });
        this.addRenderableWidget(code);
        this.setInitialFocus(code);
        this.addRenderableWidget(AioaScreenUtil.menuButton(left + 10, footer, 110, isLua() ? "Language  Lua" : "Language  Legacy", button -> {
            this.language = isLua() ? "legacy" : "lua";
            this.dirty = true;
            persistDraft();
            this.init();
        }));
        this.addRenderableWidget(AioaScreenUtil.menuButton(left + 124, footer, 96, "Check Lua", button -> {
            String error = AioaLuaScript.validate(this.source);
            this.result = error == null ? "Lua syntax OK" : error;
        })).active = isLua();
        this.addRenderableWidget(AioaScreenUtil.button(left + width - 120, footer, 110, "Save and return", button -> this.onClose()));
    }

    private boolean isLua() { return "lua".equals(this.language); }

    private void loadDraft() {
        String id = UUID.nameUUIDFromBytes(draftKey.getBytes(StandardCharsets.UTF_8)).toString();
        this.draft = this.minecraft.gameDirectory.toPath().resolve("config/aioa-script-drafts/" + id + ".txt");
        if (!Files.isRegularFile(this.draft)) return;
        try {
            String saved = Files.readString(this.draft);
            int separator = saved.indexOf('\n');
            if (separator < 0 || saved.length() - separator - 1 > AioaLuaScript.SOURCE_LIMIT) return;
            this.language = saved.substring(0, separator);
            this.source = saved.substring(separator + 1);
            this.result = "Recovered draft";
        } catch (IOException error) {
            this.result = "Could not read the saved draft";
        }
    }

    private boolean persistDraft() {
        if (!this.dirty || this.draft == null) return true;
        Path pending = this.draft.resolveSibling(this.draft.getFileName() + ".tmp");
        try {
            Files.createDirectories(this.draft.getParent());
            Files.writeString(pending, this.language + "\n" + this.source);
            try {
                Files.move(pending, this.draft, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(pending, this.draft, StandardCopyOption.REPLACE_EXISTING);
            }
            this.dirty = false;
            return true;
        } catch (IOException error) {
            this.result = "Could not save the script draft";
            this.saveAfter = System.nanoTime() + 2_000_000_000L;
            return false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dirty && System.nanoTime() >= this.saveAfter) persistDraft();
    }

    @Override
    public void removed() {
        persistDraft();
        super.removed();
    }

    @Override
    public void onClose() {
        if (!persistDraft()) return;
        this.onDone.accept(this.language, this.source);
        this.transitionTo(this.parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.beginUiRender(g);
        AioaScreenUtil.drawScreenBackground(g, this.width, this.height);
        int left = editorLeft();
        int right = left + editorWidth();
        g.fill(left, 12, right, this.height - 10, 0xFF18231D);
        g.fill(left, 12, right, 39, 0xFF24392B);
        BehaviorBoard.Part.BOOK.draw(g, left + 10, 17);
        g.drawString(this.font, this.title, left + 34, 22, BehaviorBoard.BONE, false);
        g.drawString(this.font, isLua() ? "Inputs   health  max_health  target_exists  target_distance  tick" : "Legacy commands   say  rotate  glow  heal  stop",
                left + 10, 48, AioaScreenUtil.TEXT_SUB, false);
        if (isLua()) g.drawString(this.font, "Actions   say()  glow()  heal()  move_to_target()  get()  set()",
                left + 10, 63, BehaviorBoard.PATINA, false);
        String status = this.result.isEmpty() ? (this.dirty ? "Saving draft…" : "Draft saved as you type") : this.result;
        g.drawString(this.font, this.font.plainSubstrByWidth(status.replace('\n', ' '), editorWidth() - 20),
                left + 10, this.height - 58, AioaScreenUtil.TEXT_SUB, false);
        super.render(g, mouseX, mouseY, partialTick);
        this.finishUiRender(g);
    }
}
