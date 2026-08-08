package com.flubburr.aioa.behavior;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;

public final class AioaGraphLibrary {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private AioaGraphLibrary() { }

    public static Path exportGraph(AioaBehaviorGraph graph, AioaConfig config) throws IOException {
        return exportGraph(graph, config, false);
    }

    public static Path exportGraphAs(AioaBehaviorGraph graph, AioaConfig config) throws IOException {
        return exportGraph(graph, config, true);
    }

    private static Path exportGraph(AioaBehaviorGraph graph, AioaConfig config, boolean uniqueCopy) throws IOException {
        Path directory = libraryDirectory(config);
        Files.createDirectories(directory);
        String slug = graph.name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) slug = "behavior";
        String suffix = uniqueCopy ? "-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .format(java.time.LocalDateTime.now()) : "";
        Path target = directory.resolve(slug + suffix + ".aioagraph");
        return exportGraphTo(graph, target);
    }

    public static Path exportGraphTo(AioaBehaviorGraph graph, Path requestedTarget) throws IOException {
        Path target = requestedTarget.toAbsolutePath().normalize();
        if (!target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".aioagraph")) {
            target = target.resolveSibling(target.getFileName() + ".aioagraph");
        }
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(new GraphFile("aioa-behavior-graph", 2, graph.copy()), writer);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    public static Optional<AioaBehaviorGraph> loadNewest(AioaConfig config) throws IOException {
        Path directory = libraryDirectory(config);
        if (!Files.isDirectory(directory)) return Optional.empty();
        Optional<Path> newest;
        try (var files = Files.list(directory)) {
            newest = files.filter(path -> path.getFileName().toString().endsWith(".aioagraph"))
                    .max(Comparator.comparingLong(AioaGraphLibrary::lastModified));
        }
        if (newest.isEmpty()) return Optional.empty();
        return loadGraph(newest.get());
    }

    public static Optional<AioaBehaviorGraph> loadGraph(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) return Optional.empty();
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            GraphFile file = GSON.fromJson(reader, GraphFile.class);
            if (file == null || !"aioa-behavior-graph".equals(file.format)
                    || file.version < 1 || file.version > 2 || file.graph == null) return Optional.empty();
            return Optional.of(file.graph.sanitize());
        }
    }

    public static Path libraryDirectory(AioaConfig config) {
        Path root = Services.PLATFORM.getConfigDirectory().toAbsolutePath().normalize();
        Path directory = root.resolve(config.behaviorEngine.graphLibraryDirectory).normalize();
        return directory.startsWith(root) ? directory : root.resolve("aioa/graphs");
    }

    private static long lastModified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return Long.MIN_VALUE; }
    }

    private static final class GraphFile {
        String format;
        int version;
        AioaBehaviorGraph graph;

        GraphFile(String format, int version, AioaBehaviorGraph graph) {
            this.format = format;
            this.version = version;
            this.graph = graph;
        }
    }
}
