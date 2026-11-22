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
            if "net.minecraft.client" not in text and "/client/" not in source.as_posix():
                continue
            text = GUI_SYMBOL.sub(lambda match: GUI_NAMES[match[0]], text)
            text = text.replace("resizeDisplay", "resizeGui")
            text = re.sub(r"\.displayClientMessage\(([^\n]+), true\)", r".sendOverlayMessage(\1)", text)
            if game == "26.2":
                text = text.replace(".setScreen(", ".gui.setScreen(")
                text = re.sub(r"\b(client|minecraft)\.screen\b", r"\1.gui.screen()", text)
            source.write_text(text, encoding="utf-8")

