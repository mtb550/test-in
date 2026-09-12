package org.testin.editor.listeners;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * What one edit of one grid cell came to.
 * <p>
 * It was a boolean, and a boolean cannot say the third thing: that the grid
 * would not take the value and has already told the tester why. The parent
 * listener then followed a red <i>Could not read "Urgnt" as a Priority</i> with
 * a blue <i>Adjusted - Testin stored 'High' rather than 'Urgnt'</i>, because the
 * cell really did end up holding something other than what was typed - two
 * balloons for one keystroke, the second of them explaining the first as though
 * it were a different event (#66, finding 81).
 */
@Getter
@AllArgsConstructor
public enum GridEdit {

    /**
     * The value reached the test case or the run item, so the edit is confirmed,
     * saved and generated.
     */
    WROTE(true, false),

    /**
     * Nothing changed: a column that cannot be edited, a row the run does not
     * cover, or a value that came back equal to the one already there. Nothing
     * to save and nothing to say.
     */
    UNCHANGED(false, false),

    /**
     * The grid would not take the value, and has said so in its own words. There
     * is nothing left to tell the tester.
     */
    REFUSED(false, true);

    private final boolean written;
    private final boolean said;
}
