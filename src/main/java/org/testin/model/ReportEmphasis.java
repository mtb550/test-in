package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * How loudly a bug field reads in a report.
 * <p>
 * The three report formats each held their own copy of the same rule - blocker
 * and high are alarming, major and medium are cautionary, everything else is
 * muted - as six two-entry maps plus six "and anything else is grey" fallbacks
 * written at the lookup sites. Six copies of one sentence, in three files that
 * cannot see each other.
 * <p>
 * They had already drifted. The HTML generator's own comment said its colors
 * were "the colors the PDF gives these two"; its white skin resolved to
 * #9C4B4F and #BD7740 where the PDF painted #C0392B and #B8860B.
 * <p>
 * The severity or priority declares how loud it is; the report decides what
 * loud looks like in its own medium. That split is the one {@link ResultAnalysis}
 * already makes, and for the same reason: a point size, a hex string and a CSS
 * token are three encodings of one decision.
 */
@Getter
@AllArgsConstructor
public enum ReportEmphasis {

    /** Act on this. */
    ALARMING("C0392B", "var(--verdict-failed)"),

    /** Worth reading before the rest. */
    CAUTIONARY("B8860B", "var(--verdict-blocked)"),

    /** Present, and not asking for attention - including the unset value. */
    MUTED("595959", "var(--muted)");

    private final @NotNull String hexColor;
    private final @NotNull String cssToken;
}
