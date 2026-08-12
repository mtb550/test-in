package org.testin.editorPanel;

import com.intellij.ui.JBColor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Shared colors for list and grid selection states.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditorColors {
    public static final @NotNull Color SELECTION_BACKGROUND = new JBColor(
            new Color(214, 230, 250),
            new Color(37, 55, 76)
    );
    public static final @NotNull Color SELECTION_BORDER = JBColor.blue;

}
