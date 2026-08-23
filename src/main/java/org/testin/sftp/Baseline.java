package org.testin.sftp;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.TreeMap;

/**
 * The project exactly as it stood after the last successful transfer (#94).
 * <p>
 * This is the piece that makes an SSH server behave like a colleague rather
 * than a shared folder. Git hands a merge three versions of a conflicted file -
 * the common ancestor, this machine's and the one that arrived - and
 * {@code TestCaseMerge} settles almost every field between them. A server keeps
 * one copy and no history, so the ancestor has to come from somewhere: it comes
 * from here, remembered at the moment both sides last agreed.
 * <p>
 * Without it, two testers editing different fields of the same test case would
 * have no way to both keep their work, and every difference would be somebody
 * choosing which of them to discard.
 * <p>
 * It holds content, not just hashes, for the same reason: a hash can say that
 * something changed, and only the text can be merged against.
 */
public record Baseline(@NotNull Map<String, String> contents) {

    /**
     * What is remembered before anything has ever been transferred. Every file
     * then reads as absent, which is what makes a first sync the ordinary case
     * rather than a path of its own.
     */
    public static final @NotNull Baseline EMPTY = new Baseline(Map.of());

    /**
     * Sorted and copied, so the stored document is stable between writes and a
     * baseline cannot be changed underneath a merge that is reading it.
     */
    public Baseline {
        contents = Map.copyOf(new TreeMap<>(contents));
    }

    /**
     * What that file held at the last transfer, and empty when it held nothing -
     * because it was new here, new there, or never transferred at all.
     * <p>
     * Empty is the honest ancestor for a file with no history: a three-way merge
     * given an empty base treats both sides as additions, which is what they are.
     */
    public @NotNull String at(final @NotNull String path) {
        return contents.getOrDefault(path, "");
    }

    /**
     * The hashes of what is remembered, so the same comparison that runs against
     * the two live sides can run against this one.
     * <p>
     * Derived rather than stored. Keeping hashes beside the content would be two
     * records of one fact, and the day they disagree is the day a sync deletes
     * something.
     */
    public @NotNull Manifest manifest() {
        final @NotNull Map<String, Manifest.Entry> entries = new TreeMap<>();
        contents.forEach((path, content) -> entries.put(path,
                new Manifest.Entry(Manifest.sha256(content), content.getBytes(StandardCharsets.UTF_8).length)));

        return new Manifest(entries);
    }
}
