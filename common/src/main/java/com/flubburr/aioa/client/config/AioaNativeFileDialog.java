package com.flubburr.aioa.client.config;

import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.util.Optional;

/** Native graph file dialogs backed by the LWJGL component already shipped with Minecraft. */
final class AioaNativeFileDialog {
    private AioaNativeFileDialog() { }

    static Optional<Path> chooseGraphToOpen(Path initialDirectory) {
        String selected = TinyFileDialogs.tinyfd_openFileDialog(
                "Import AIOA behavior graph",
                initialDirectory.toAbsolutePath().normalize().toString(),
                (PointerBuffer) null,
                "AIOA behavior graphs (*.aioagraph)",
                false);
        return selected == null || selected.isBlank() ? Optional.empty() : Optional.of(Path.of(selected));
    }

    static Optional<Path> chooseGraphToSave(Path suggestedFile) {
        String selected = TinyFileDialogs.tinyfd_saveFileDialog(
                "Save AIOA behavior graph",
                suggestedFile.toAbsolutePath().normalize().toString(),
                (PointerBuffer) null,
                "AIOA behavior graphs (*.aioagraph)");
        return selected == null || selected.isBlank() ? Optional.empty() : Optional.of(Path.of(selected));
    }
}
