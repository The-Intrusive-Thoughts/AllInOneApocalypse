"""Apply the mechanical Mojang GUI symbol changes to the shared 26.x client sources."""
import re

GUI_NAMES = {
    "GuiGraphics": "GuiGraphicsExtractor",
    "renderBackground": "extractBackground",
    "renderWidget": "extractWidgetRenderState",
    "renderContents": "extractContents",
    "renderEntityInInventoryFollowsMouse": "extractEntityInInventoryFollowsMouse",
    "drawString": "text",
    "drawCenteredString": "centeredText",
    "renderItem": "item",
    "renderOutline": "outline",
    "render": "extractRenderState",
}
GUI_SYMBOL = re.compile(r"\b(" + "|".join(GUI_NAMES) + r")\b(?=\s*[.( ,;\n])")


def port_client(staging, game):
    for module in ("common", "fabric", "neoforge", "forge"):
        for source in (staging / module).rglob("*.java"):
            text = source.read_text(encoding="utf-8")
            original = text
            if game == "26.3":
                text = text.replace(".setInvulnerable(", ".setPermanentlyInvulnerable(")
                text = text.replace(".hurtMarked = true;", ".syncVelocity = true;")
                if source.name == "AioaNativeFileDialog.java":
                    text = (staging / "AioaNativeFileDialog.java").read_text(encoding="utf-8")
            if "net.minecraft.client" not in text and "/client/" not in source.as_posix():
                if text != original:
                    source.write_text(text, encoding="utf-8")
                continue
            text = GUI_SYMBOL.sub(lambda match: GUI_NAMES[match[0]], text)
            text = text.replace("resizeDisplay", "resizeGui")
            text = re.sub(r"\.displayClientMessage\(([^\n]+), true\)", r".sendOverlayMessage(\1)", text)
            if game in ("26.2", "26.3"):
                text = text.replace(".setScreen(", ".gui.setScreen(")
                text = re.sub(r"\b(client|minecraft)\.screen\b", r"\1.gui.screen()", text)
            if game == "26.3":
                text = port_sdl_input(text)
            source.write_text(text, encoding="utf-8")


def port_sdl_input(text):
    text = text.replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
    text = text.replace("int button = event.button();", """int button = switch (event.button()) {
            case 1 -> 0; // SDL left
            case 3 -> 1; // SDL right
            case 2 -> 2; // SDL middle
            default -> event.button();
        };""")
    if "import org.lwjgl.glfw.GLFW;" not in text:
        return text
    text = text.replace("import org.lwjgl.glfw.GLFW;", "")
    if "import com.mojang.blaze3d.platform.InputConstants;" not in text:
        text = re.sub(r"(package [^;]+;)", r"\1\n\nimport com.mojang.blaze3d.platform.InputConstants;", text, count=1)
    text = text.replace("GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS",
                        "minecraft.mouseHandler.isRightPressed()")
    text = text.replace("        long window = this.minecraft.getWindow().handle();\n", "")
    text = re.sub(r"GLFW.glfwGetKey\(window, GLFW.GLFW_KEY_(LEFT|RIGHT)_(CONTROL|SHIFT)\) == GLFW.GLFW_PRESS",
                  lambda match: f"InputConstants.isKeyDown(InputConstants.KEY_{match[1][0]}{match[2]})", text)
    text = re.sub(r"GLFW.GLFW_(KEY_\w+|MOD_CONTROL)", r"InputConstants.\1", text)
    keys = {257: "RETURN", 65: "A", 90: "Z", 89: "Y", 83: "S", 78: "N", 68: "D",
            48: "0", 261: "DELETE", 72: "H", 70: "F"}
    return re.sub(r"keyCode == (" + "|".join(map(str, keys)) + r")\b",
                  lambda match: "keyCode == InputConstants.KEY_" + keys[int(match[1])], text)

