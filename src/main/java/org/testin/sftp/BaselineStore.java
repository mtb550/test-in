package org.testin.sftp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.application.PathManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.util.Mapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Keeps a {@link Baseline} between syncs (#94).
 * <p>
 * Not beside the test project, and this is deliberate three times over. It is
 * not test data, so it has no business in a tree the indexer owns and a tester
 * browses. It must never be committed, and a project that also happens to be in
 * a Git repository would carry it if it sat there. And it belongs to this
 * machine's copy of the project rather than to the project, because it records
 * what <em>this</em> machine last agreed with the server about.
 * <p>
 * So it lives under the IDE's own system directory, keyed on the test project's
 * path - not on the IDE project, which would give one test project two
 * disagreeing baselines when it is opened from two different IDE projects.
 * <p>
 * Stored as one gzipped document rather than a directory of files. A test
 * project of 2,246 files costs 9.2 MB as a copy on disk and about 150 KB as one
 * gzip stream, because 683-byte JSON files compress badly alone and well
 * together.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BaselineStore {

    private static final @NotNull TypeReference<Map<String, String>> CONTENTS = new TypeReference<>() {
    };

    /**
     * Where this machine remembers the given test project.
     */
    public static @NotNull Path fileFor(final @NotNull Path testProject) {
        return Path.of(PathManager.getSystemPath(), "testin", "baseline", keyOf(testProject) + ".json.gz");
    }

    /**
     * A file name that is unique to this project path and safe on every file
     * system.
     * <p>
     * The digest carries the identity and the folder name carries the meaning,
     * so somebody looking in that directory can tell which project a file
     * belongs to without decoding anything - and two projects with the same
     * folder name still get their own.
     */
    private static @NotNull String keyOf(final @NotNull Path testProject) {
        final @NotNull String digest = Manifest.sha256(testProject.toAbsolutePath().toString());
        final @NotNull Path name = testProject.getFileName();

        return (name == null ? "project" : name.toString().replaceAll("[^A-Za-z0-9._-]", "_"))
                + "-" + digest.substring(0, Math.min(16, digest.length()));
    }

    /**
     * What was last agreed with the server, and {@link Baseline#EMPTY} when
     * nothing has been - or when what was stored cannot be read.
     * <p>
     * An unreadable baseline answering empty is the safe failure: every file
     * then looks new to both sides, so the next sync asks about anything that
     * differs instead of quietly deciding it. A baseline that answered with half
     * of itself would delete the other half.
     */
    public static @NotNull Baseline read(final @NotNull Mapper mapper, final @NotNull Path file) {
        if (!Files.isRegularFile(file)) return Baseline.EMPTY;

        try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) {
            return new Baseline(mapper.readValue(new String(in.readAllBytes(), StandardCharsets.UTF_8), CONTENTS));
        } catch (final IOException | RuntimeException ex) {
            Logger.error("Could not read the sync baseline at " + file + ", starting from none: " + ex.getMessage());
            return Baseline.EMPTY;
        }
    }

    /**
     * Remembers what both sides now hold. Answers whether it was stored.
     * <p>
     * Written to a temporary file and moved into place, because a baseline
     * half-written by a transfer that was interrupted is worse than no baseline
     * at all: it would claim files were agreed that never arrived, and the next
     * sync would read that as the server having deleted them.
     */
    public static boolean write(final @NotNull Mapper mapper, final @NotNull Path file, final @NotNull Baseline baseline) {
        final @NotNull Path partial = file.resolveSibling(file.getFileName() + ".part");

        try {
            Files.createDirectories(file.getParent());

            try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(partial))) {
                out.write(mapper.writeValueAsString(baseline.contents()).getBytes(StandardCharsets.UTF_8));
            }

            Files.move(partial, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (final IOException | RuntimeException ex) {
            Logger.error("Could not store the sync baseline at " + file + ": " + ex.getMessage());
            quietlyRemove(partial);
            return false;
        }
    }

    private static void quietlyRemove(final @NotNull Path partial) {
        try {
            Files.deleteIfExists(partial);
        } catch (final IOException ex) {
            Logger.warn("Left a partial baseline behind at " + partial + ": " + ex.getMessage());
        }
    }
}
