package org.testin.editorPanel;

import com.intellij.ui.JBColor;

import java.awt.*;

/**
 * Shared colors for list and grid selection states.
 */
public final class EditorColors {
    public static final Color SELECTION_BACKGROUND = new JBColor(
            new Color(214, 230, 250),
            new Color(37, 55, 76)
    );
    public static final Color SELECTION_BORDER = JBColor.blue;

    private EditorColors() {
    }
}
