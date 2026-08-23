package org.testin.sftp;

import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * What a side of the sync holds: every file, by path, with its hash and size
 * (#94).
 * <p>
 * The server keeps one of these beside the project, so a sync can tell what
 * changed without downloading anything. That matters more here than anywhere
 * else in the plugin: a test project measured 2,246 files and 1.5 MB, and at a
 * realistic 20 ms round trip, asking the server about each file one at a time
 * costs about 135 seconds against 1.2 seconds for the bytes themselves. The
 * latency is a hundred times the data, so the whole design is "fetch one small
 * document, then move only what differs".
 * <p>
 * Paths are relative to the project root and always use forward slashes, so a
 * manifest written on Windows and read on Linux describes the same files.
 */
public record Manifest(@NotNull Map<String, Entry> entries) {

    /**
     * What a side that holds nothing reports - a server nobody has synced to
     * yet, or a baseline before the first transfer.
     */
    public static final @NotNull Manifest EMPTY = new Manifest(Map.of());

    /**
     * Sorted, so two manifests of the same files serialize to the same bytes and
     * a diff of the document is readable by a human.
     */
    public Manifest {
        entries = Map.copyOf(new TreeMap<>(entries));
    }

    /**
     * The manifest of a set of files, given their contents.
     */
    public static @NotNull Manifest of(final @NotNull Map<String, byte[]> files) {
        final @NotNull Map<String, Entry> entries = new LinkedHashMap<>();
        files.forEach((path, content) -> entries.put(path, Entry.of(content)));

        return new Manifest(entries);
    }

    /**
     * What this side holds at that path, and {@link Entry#ABSENT} when it holds
     * nothing there.
     * <p>
     * The one place absence is decided, so {@link TransferAction#of} can be
     * handed three hashes and never a question about whether a file exists.
     */
    public @NotNull Entry at(final @NotNull String path) {
        return entries.getOrDefault(path, Entry.ABSENT);
    }

    /**
     * Every path either side knows about, which is what a sync has to walk. A
     * path only one side has is exactly the interesting case, so the union is
     * the right set rather than the intersection.
     */
    public @NotNull Set<String> pathsWith(final @NotNull Manifest other) {
        final @NotNull Set<String> all = new java.util.TreeSet<>(entries.keySet());
        all.addAll(other.entries().keySet());

        return all;
    }

    public long totalBytes() {
        return entries.values().stream().mapToLong(Entry::size).sum();
    }

    /**
     * One file in a manifest.
     *
     * @param sha256 lowercase hex, and empty for {@link #ABSENT} - a real digest
     *               is never empty, which is what lets absence be a value rather
     *               than a null
     */
    public record Entry(@NotNull String sha256, long size) {

        /**
         * The entry for a file this side does not have.
         */
        public static final @NotNull Entry ABSENT = new Entry("", 0);

        public static @NotNull Entry of(final byte @NotNull [] content) {
            // Qualified: this record has a component called sha256, whose
            // accessor takes no arguments and would win the name otherwise.
            return new Entry(Manifest.sha256(content), content.length);
        }

        /**
         * Whether this side has the file at all.
         * <p>
         * Not written to the manifest. Jackson turns a getter into a property,
         * and a property the record has no component for cannot be read back -
         * which made every manifest unreadable, so every sync saw an empty
         * server and every file looked deleted.
         */
        @com.fasterxml.jackson.annotation.JsonIgnore
        public boolean isAbsent() {
            return sha256.isEmpty();
        }
    }

    /**
     * The digest a manifest compares by.
     * <p>
     * SHA-256 rather than a size and a timestamp: a test case edited back to the
     * same length keeps its size, and timestamps do not survive a transfer
     * intact. Rather than truncate it - the manifest is one gzipped document,
     * so the saved bytes would be invisible - the whole digest is kept, because
     * a collision here silently drops somebody's work and nobody would ever
     * work out why.
     * <p>
     * Answers empty when the algorithm is missing, which no JVM this plugin runs
     * on lacks; that empty then reads as "absent" everywhere downstream, so a
     * broken JVM produces a sync that does nothing rather than one that deletes.
     */
    static @NotNull String sha256(final byte @NotNull [] content) {
        try {
            final byte @NotNull [] digest = MessageDigest.getInstance("SHA-256").digest(content);
            final @NotNull StringBuilder hex = new StringBuilder(digest.length * 2);
            for (final byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }

            return hex.toString();
        } catch (final NoSuchAlgorithmException ex) {
            Logger.error("This JVM has no SHA-256, so nothing can be compared: " + ex.getMessage());
            return "";
        }
    }

    /**
     * The digest of text, for a caller holding content rather than bytes.
     */
    static @NotNull String sha256(final @NotNull String content) {
        return sha256(content.getBytes(StandardCharsets.UTF_8));
    }
}
