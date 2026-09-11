package org.testin.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import org.testin.util.Bundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What a group is, now that it is a word rather than a constant (#296).
 * <p>
 * It was an enum of eight, so the groups a team could use were the groups
 * somebody shipped - a team whose taxonomy said Performance or Payments could
 * not say so. #200 made all eight offered rather than three, which was right and
 * also showed the shape of the problem: a list that only grows, drawn as tick
 * boxes that only get wider.
 * <p>
 * A group is now what a module already was - free text, completed from what the
 * project has used, which {@code TestCaseCacheService} holds. This class is what
 * is left over when the enum goes: the one word that is not a group, and the two
 * ways a list of them is read and written.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Groups {

    /**
     * The filter row and the picker entry for a test case that is in no group.
     * <p>
     * Not a group, and never stored as one - a case with no groups holds an
     * empty list. It is a word on a menu, and the angle brackets are what say it
     * is not a name somebody typed.
     */
    public static final @NotNull String NONE = Bundle.message("groups.none");

    /**
     * The groups in a comma-separated line, as a cell and a sheet hold them.
     * <p>
     * Nothing here can be refused any more, which is the whole point: a group
     * Testin has never seen is a group the tester is adding. Blank is no groups,
     * and so is the No Group word, which the picker writes and has to read back.
     */
    public static @NotNull List<String> read(final @NotNull String raw) {
        if (raw.isBlank() || raw.trim().equalsIgnoreCase(NONE)) return new ArrayList<>();

        final @NotNull List<String> read = new ArrayList<>();
        for (final String name : raw.split(",")) {
            final @NotNull String group = name.trim();

            // Named twice is named once, which the bulk editor did and the grid
            // did not - so the same cell gave two answers depending on where it
            // was typed (#295).
            if (!group.isEmpty() && !read.contains(group)) read.add(group);
        }

        return read;
    }

    /**
     * The line a cell, a card and a sheet all show, so the three cannot drift.
     */
    public static @NotNull String text(final @NotNull List<String> groups) {
        return String.join(", ", groups);
    }
}
