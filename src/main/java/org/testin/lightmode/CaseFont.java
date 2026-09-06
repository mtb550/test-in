package org.testin.lightmode;

import com.intellij.util.ui.JBFont;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
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
 * <b>Three sizes, and the difference between them is the point.</b>
 * {@link #description} is the thing being tested and is set larger and bold;
 * {@link #body} - the expected result, the steps, the test data - is read
 * against it at the editor's own size; {@link #label}, for a field name or the
 * test set name, is smaller still, because a label names what is beside it
 * rather than being read for itself. Flattening them would leave a window of one
 * uniform paragraph.
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

    static @NotNull Font description() {
        return at(FontSync.getBaseFontSize() + BIGGER, Font.BOLD);
    }

    static @NotNull Font body() {
        return at(FontSync.getBaseFontSize(), Font.PLAIN);
    }

    static @NotNull Font label() {
        return at(FontSync.getBaseFontSize() - SMALLER, Font.PLAIN);
    }

    private static @NotNull Font at(final float size, @MagicConstant(flags = {Font.PLAIN, Font.BOLD, Font.ITALIC}) final int style) {
        return JBFont.label().deriveFont(style, Math.max(FontSync.FLOOR, size));
    }

    /**
     * A font at this window's zoom. One owner, because the case, its details and
     * the failure form each wrote the same multiplication.
     */
    static @NotNull Font zoomed(final @NotNull Font base, final float zoom) {
        return base.deriveFont(base.getSize2D() * zoom);
    }
}
