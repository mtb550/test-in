package org.testin.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Writes one key into {@code testin.yml} and leaves the rest of the file exactly
 * as it was (#6).
 * <p>
 * The file is committed and meant to be read by people, so the comments in it are
 * the reason YAML was chosen at all. Serializing the config object back over the
 * file would produce the same keys and none of the comments. So this replaces the
 * single line the key is on, keeping its line ending, or appends one line when the
 * key is not there yet. Every other byte survives.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestinConfigWriter {

    /**
     * What a new file starts as. The comments say what the file is for and what
     * must never go in it, because the next person to open it is a tester who did
     * not write it.
     */
    /**
     * What a file the plugin creates starts as.
     * <p>
     * One key and no explanation. The format is documented where a tester
     * reads documentation, not in every repository that carries a copy of it -
     * a comment block here is written once and then maintained forever, in
     * files nobody remembers are theirs to update.
     */
    /**
     * What a file the plugin creates starts as.
     * <p>
     * Every key it can carry, listed once so a tester setting up a server months
     * later reads the file rather than the documentation - and short enough that
     * nobody has to maintain it. The commented lines are the whole format.
     */
    private static final @NotNull String HEADER = """
            # Which test project this repository drives. Committed, so a clone needs no setup.
            # No machine or person here: root folder, account and passwords live in Testin's
            # settings.

            location: local

            # location: remote             shared, then one of:
            # connection: git
            # RepoUrl: https://github.com/you/test-01.git
            # connection: sftp
            # sftpHost: 127.0.0.1
            # sftpPort: 22                 optional, 22
            # sftpPath: /Testin            the folder holding the projects
            # testinProject: test-01       which of them, in every case
            """;

    /**
     * A line that assigns the key at the top level of the document.
     * <p>
     * Indentation is not allowed, which is the point: every key in this format is
     * top level, so a match on an indented line would be some future nested
     * block's key of the same name. Anchoring at the start also leaves a key
     * alone when it is only mentioned - in a comment, or inside a value.
     */
    private static @NotNull Pattern assignment(final @NotNull String key) {
        return Pattern.compile("^" + Pattern.quote(key) + "[ \t]*:.*$");
    }

    /**
     * Puts {@code key: value} in the file, creating it when it is not there.
     * Answers whether the file now says so - a disk that refuses leaves the
     * caller with something it has to say out loud rather than report as done.
     */
    static boolean write(final @NotNull Path file, final @NotNull String key, final @NotNull String value) {
        try {
            final @NotNull String before = Files.isRegularFile(file) ? Files.readString(file) : HEADER;
            Files.writeString(file, apply(before, key, value));
            Logger.info("Wrote " + key + " to " + file);
            return true;

        } catch (final IOException ex) {
            Logger.error("Could not write " + key + " to " + file + ": " + ex.getMessage());
            return false;
        }
    }

    /**
     * The file's text with one key set, and nothing else touched. Separate from
     * {@link #write} because preserving the rest of the file is the whole point
     * and is what the tests pin.
     */
    static @NotNull String apply(final @NotNull String content, final @NotNull String key, final @NotNull String value) {
        // Split after each newline so every line keeps its own ending: a file
        // written on Windows stays that way, including the lines not touched.
        final String @NotNull[] lines = content.split("(?<=\n)", -1);
        final @NotNull Pattern assignment = assignment(key);
        final @NotNull String scalar = scalar(value);

        for (int i = 0; i < lines.length; i++) {
            final @NotNull String line = lines[i];
            final @NotNull String text = line.stripTrailing();
            if (!assignment.matcher(text).matches()) continue;

            // Everything the line ended with - trailing spaces, the newline, the
            // carriage return before it - comes back untouched, so a binding is a
            // one-line diff and not a whole-file one.
            lines[i] = key + ": " + scalar + line.substring(text.length());
            return String.join("", lines);
        }

        final @NotNull String separator = content.isEmpty() || content.endsWith("\n") ? "" : "\n";
        return content + separator + key + ": " + scalar + "\n";
    }

    /**
     * The value as YAML reads it back. Plain where that is unambiguous, quoted
     * where a leading indicator, an inner {@code ": "} or a trailing space would
     * otherwise make the file mean something the tester never typed.
     */
    private static @NotNull String scalar(final @NotNull String value) {
        final boolean plain = !value.isEmpty()
                && value.equals(value.strip())
                && !value.contains(": ")
                && !value.contains(" #")
                && !value.contains("\n")
                && "#&*!|>%@`{}[],\"'".indexOf(value.charAt(0)) < 0;

        return plain ? value : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
