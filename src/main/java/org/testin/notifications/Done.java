package org.testin.notifications;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * What a state-changing action says when it worked.
 * <p>
 * CLAUDE.md has stated the rule for a long time: the message is the outcome in
 * the past tense and nothing else, one word wherever one will do, no trailing
 * dot and no noun - the tester pressed the key on the thing in front of them,
 * so naming it back is a word they read every time and needed once.
 * <p>
 * The rule had no owner, so every action spelled its own word: eighteen of them
 * across two methods, with nothing to compare a new one against. A word here is
 * a word the whole plugin can use, and a nineteenth outcome is a constant rather
 * than a decision taken alone at a call site.
 * <p>
 * It also makes the rule enforceable rather than remembered. A present-tense
 * word, a sentence, or a noun cannot be passed to
 * {@code Notifier.softShow(Project, Done)} at all.
 */
@Getter
@AllArgsConstructor
public enum Done {

    COPIED("Copied"),
    CUT("Cut"),
    PASTED("Pasted"),
    MOVED("Moved"),

    CREATED("Created"),
    RENAMED("Renamed"),
    REMOVED("Removed"),
    UPDATED("Updated"),
    SAVED("Saved"),

    IMPORTED("Imported"),
    EXPORTED("Exported"),

    ORDERED("Ordered"),
    RE_SORTED("Re-sorted"),
    REFRESHED("Refreshed"),

    UNDONE("Undone"),
    REDONE("Redone"),
    REVERTED("Reverted"),

    STOPPED("Stopped"),
    CLEARED("Cleared");

    /**
     * The word itself. Past tense, one word where one will do - which is the
     * whole of the rule, and the reason a constant here is worth more than the
     * string it holds.
     */
    private final @NotNull String outcome;
}
