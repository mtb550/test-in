package org.testin.lightmode;

import com.intellij.util.ui.JBFont;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.FontSync;

import java.awt.Font;

/**
 * How large light mode writes the case, before the tester's own zoom (#13).
 * <p>
 * <b>Anchored to the editor, not to the UI font.</b> The size comes from
 * {@link FontSync#getBaseFontSize()}, which is what the details panel and every
 * other place the plugin shows a test case already uses - so a tester who has
 * set their IDE font to sixteen point reads the case at sixteen point here too,
 * rather than at whatever the platform happens to make of a label. Light mode
 * used to take the UI font and stood out from every other surface for it.
 * <p>
 * <b>Three sizes, and the difference between them is the point.</b> The
 * description is the thing being tested and is set larger and bold; the expected
 * result is read against it and sits at the editor's own size; the field names
 * and the test set name are smaller still, because they are labels rather than
 * text. Flattening them would leave a window of one uniform paragraph.
 * <p>
 * Methods rather than constants, deliberately. The editor font is a setting the
 * tester can change while the IDE is running, and a constant would freeze
 * whichever size was in force when this class was first loaded - the same
 * mistake {@code TestStatus} records beside its lazy colors.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CaseFont {

    /**
     * How much larger the description is than the case's other text, and how
     * much smaller a label is. Points rather than a ratio, because the anchor is
     * a point size the tester chose and these read as steps from it.
     */
    private static final int BIGGER = 3;
    private static final int SMALLER = 2;

    /**
     * Nothing is drawn below this, however small the editor font is set - the
     * same floor {@link FontSync} keeps.
     */
    private static final float FLOOR = 8.0f;

    /**
     * The test case description: the sentence the window exists to show.
     */
    static @NotNull Font description() {
        return at(FontSync.getBaseFontSize() + BIGGER, Font.BOLD);
    }

    /**
     * The expected result, the steps, the test data - everything read at the
     * size the editor would have shown it.
     */
    static @NotNull Font body() {
        return at(FontSync.getBaseFontSize(), Font.PLAIN);
    }

    /**
     * A field name or the test set name: naming what is beside it rather than
     * being read for itself.
     */
    static @NotNull Font label() {
        return at(FontSync.getBaseFontSize() - SMALLER, Font.PLAIN);
    }

    private static @NotNull Font at(final float size, final int style) {
        return JBFont.label().deriveFont(style, Math.max(FLOOR, size));
    }
}
