package org.testin.report.generators;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The one type scale the reports are set in.
 * <p>
 * Sizes were literals scattered through three generators - the PDF alone used
 * eight of them, four of which differed by half a point - so the same row of a
 * table came out one size in the PDF and another in the HTML, and nobody could
 * say which was intended. A role has one size here, and all three formats read
 * it.
 * <p>
 * Two numbers per role because the formats do not share a unit: iText and Word
 * measure in points, a browser in pixels, and 10pt is not 10px. Both are stated
 * rather than converted, so a role can be tuned for the medium it looks wrong in
 * without dragging the other two with it.
 * <p>
 * {@link #HEADING} and {@link #BODY} carry the same numbers today and are still
 * two roles: a table header and the cell under it are different questions, and
 * one should be answerable without the other changing.
 */
@AllArgsConstructor
public enum ReportFont {

    /** The report's own name, once, at the top. */
    TITLE(18f, 40),

    /** The big number on a summary tile. */
    FIGURE(20f, 42),

    /** A numbered section heading. */
    SECTION(13f, 28),

    /** The project line under the title. */
    SUBTITLE(12f, 24),

    /** The run name, a section's opening sentence, an analysis heading. */
    LEAD(11f, 21),

    /** A table's header row, and the label column of the overview. */
    HEADING(10f, 19),

    /** Values, table cells, the paragraphs a tester writes. */
    BODY(10f, 19),

    /** Row numbers and the caption under a summary tile. */
    SMALL(9f, 16),

    /** The confidentiality notice and the footer. */
    CAPTION(8f, 15);

    private final float pt;
    private final int px;

    /**
     * For iText, which takes a float.
     */
    public float pt() {
        return pt;
    }

    /**
     * For Word, whose run size is a whole number of points.
     */
    public int ptRounded() {
        return Math.round(pt);
    }

    /**
     * For a stylesheet, as the browser wants it written.
     */
    public @NotNull String css() {
        return px + "px";
    }
}
