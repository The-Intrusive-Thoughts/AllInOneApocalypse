package com.flubburr.aioa.client.config;

import java.awt.FileDialog;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.Optional;

final class AioaNativeFileDialog {
    private AioaNativeFileDialog() {}

    static Optional<Path> chooseGraphToOpen(Path directory) {
        return choose(directory, null, "Import AIOA behavior graph", FileDialog.LOAD);
    }

    static Optional<Path> chooseGraphToSave(Path suggestedFile) {
        Path target = suggestedFile.toAbsolutePath().normalize();
        return choose(target.getParent(), target.getFileName().toString(), "Save AIOA behavior graph", FileDialog.SAVE);
    }

    private static Optional<Path> choose(Path directory, String filename, String title, int mode) {
        FileDialog dialog = new FileDialog((Frame) null, title, mode);
        try {
            dialog.setDirectory(directory.toAbsolutePath().normalize().toString());
            dialog.setFile(filename);
            dialog.setVisible(true);
            if (dialog.getFile() == null) return Optional.empty();
            return Optional.of(Path.of(dialog.getDirectory(), dialog.getFile()));
        } finally {
            dialog.dispose();
        }
    }
}
