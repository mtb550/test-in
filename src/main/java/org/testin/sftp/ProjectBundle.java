package org.testin.sftp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * A whole test project as one file (#94).
 * <p>
 * Measured on a real project of 2,246 files and 1,533,699 bytes, moved through
 * SFTP to a server on this same machine:
 *
 * <pre>
 * file by file   up 18.7 s   down 71.1 s     (and 153 s / 206 s over a 20 ms link)
 * one bundle     up 0.91 s   down 0.37 s     (and 0.97 s / 0.43 s over the same link)
 * </pre>
 * <p>
 * SFTP pays a round trip per file, so 2,246 of them is 2,246 conversations
 * whatever the bytes are - which is why file-by-file gets steadily worse as the
 * network gets slower and a bundle barely notices.
 * <p>
 * Not compressed, by decision. The speed comes from being <em>one</em> transfer
 * rather than 2,246, and that is true whatever the bytes are - a compressed
 * bundle and a plain one both move in about a second. Compression only shrinks
 * what crosses the wire, and this project's 1.5 MB was never the slow part.
 * <p>
 * Tar rather than a format of this plugin's own: the reader is already in the
 * distribution, brought in by the report generators, so it costs nothing to
 * ship - and a plain tar is something a tester can open with any archive tool,
 * or unpack on the server itself, which is most of what loose files gave them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProjectBundle {

    /**
     * The timestamp every entry is written with.
     * <p>
     * Fixed, so packing the same files twice produces the same bytes. A bundle
     * that differed every time it was built would look like a change to anything
     * comparing them, and the point of the manifest is to tell a change from a
     * rebuild.
     */
    private static final long FIXED_TIME = 0L;

    /**
     * The files as one archive, ready to be written to the server.
     * <p>
     * Sorted, for the same reason the time is fixed: two runs over the same
     * project must produce the same bytes.
     */
    public static byte @NotNull [] pack(final @NotNull Map<String, byte[]> files) {
        final @NotNull ByteArrayOutputStream packed = new ByteArrayOutputStream();

        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(packed)) {
            // A test case path is short, but a deep package nest is not, and the
            // ancient tar limit of 100 characters would silently truncate one.
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            for (final Map.Entry<String, byte[]> file : new TreeMap<>(files).entrySet()) {
                final @NotNull TarArchiveEntry entry = new TarArchiveEntry(file.getKey());
                entry.setSize(file.getValue().length);
                entry.setModTime(FIXED_TIME);

                tar.putArchiveEntry(entry);
                tar.write(file.getValue());
                tar.closeArchiveEntry();
            }
        } catch (final IOException ex) {
            Logger.error("Could not pack the project: " + ex.getMessage());
            throw new IllegalStateException("Could not pack the project: " + ex.getMessage());
        }

        return packed.toByteArray();
    }

    /**
     * What a bundle holds, by the same paths it was packed with.
     * <p>
     * Directories are skipped: a manifest describes files, and the folders come
     * back from the paths when they are written out.
     */
    public static @NotNull Map<String, byte[]> unpack(final byte @NotNull [] bundle) {
        final @NotNull Map<String, byte[]> files = new TreeMap<>();

        try (TarArchiveInputStream tar =
                     new TarArchiveInputStream(new ByteArrayInputStream(bundle))) {

            for (TarArchiveEntry entry = tar.getNextEntry(); entry != null; entry = tar.getNextEntry()) {
                if (entry.isDirectory()) continue;

                files.put(entry.getName(), tar.readAllBytes());
            }
        } catch (final IOException ex) {
            Logger.error("Could not read the project bundle: " + ex.getMessage());
            throw new IllegalStateException("Could not read the project bundle: " + ex.getMessage());
        }

        return files;
    }
}
