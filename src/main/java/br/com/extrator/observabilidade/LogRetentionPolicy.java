package br.com.extrator.observabilidade;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Politica simples de retenção por diretório.
 */
public final class LogRetentionPolicy {
    private static final Logger logger = LoggerFactory.getLogger(LogRetentionPolicy.class);

    private LogRetentionPolicy() {
        // utility class
    }

    public static void retainRecentFiles(final Path dir,
                                         final int maxFiles,
                                         final Predicate<Path> matcher) {
        if (dir == null || maxFiles <= 0 || matcher == null || !Files.isDirectory(dir)) {
            return;
        }

        try (Stream<Path> stream = Files.list(dir)) {
            final List<Path> files = stream
                .filter(Files::isRegularFile)
                .filter(matcher)
                .sorted(fileComparator())
                .toList();
            deleteExcess(files, maxFiles);
        } catch (final IOException e) {
            logger.warn("Falha ao aplicar retencao em {}: {}", dir, e.getMessage());
        }
    }

    public static void retainRecentFilesRecursively(final Path rootDir,
                                                    final int maxFiles,
                                                    final Predicate<Path> matcher) {
        if (rootDir == null || maxFiles <= 0 || matcher == null || !Files.isDirectory(rootDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(rootDir)) {
            final List<Path> files = stream
                .filter(Files::isRegularFile)
                .filter(matcher)
                .sorted(fileComparator())
                .toList();
            deleteExcess(files, maxFiles);
            deleteEmptyDirectories(rootDir);
        } catch (final IOException e) {
            logger.warn("Falha ao aplicar retencao recursiva em {}: {}", rootDir, e.getMessage());
        }
    }

    public static boolean hasExtension(final Path path, final String... extensions) {
        if (path == null || extensions == null || extensions.length == 0) {
            return false;
        }
        final String name = path.getFileName().toString().toLowerCase();
        for (final String extension : extensions) {
            if (extension != null && name.endsWith(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static Comparator<Path> fileComparator() {
        return Comparator
            .comparingLong(LogRetentionPolicy::lastModifiedSafe)
            .thenComparing(path -> path.getFileName().toString());
    }

    private static void deleteExcess(final List<Path> files, final int maxFiles) {
        if (files.size() <= maxFiles) {
            return;
        }
        for (int i = 0; i < files.size() - maxFiles; i++) {
            final Path candidate = files.get(i);
            try {
                Files.deleteIfExists(candidate);
            } catch (final IOException e) {
                logger.warn("Falha ao remover arquivo antigo {}: {}", candidate, e.getMessage());
            }
        }
    }

    private static void deleteEmptyDirectories(final Path rootDir) throws IOException {
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream
                .filter(Files::isDirectory)
                .sorted(Comparator.reverseOrder())
                .filter(path -> !path.equals(rootDir))
                .forEach(path -> {
                    try (Stream<Path> children = Files.list(path)) {
                        if (children.findAny().isEmpty()) {
                            Files.deleteIfExists(path);
                        }
                    } catch (final IOException e) {
                        logger.debug("Falha ao remover diretorio vazio {}: {}", path, e.getMessage());
                    }
                });
        }
    }

    private static long lastModifiedSafe(final Path path) {
        try {
            final FileTime lastModified = Files.getLastModifiedTime(path);
            return lastModified == null ? Long.MIN_VALUE : lastModified.toMillis();
        } catch (final IOException e) {
            logger.debug("Falha ao ler ultima modificacao de {}: {}", path, e.getMessage());
            return Long.MIN_VALUE;
        }
    }
}
