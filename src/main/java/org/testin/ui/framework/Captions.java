package org.testin.ui.framework;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * The framework's caption column: platform hint style (small, gray), fixed
 * width so captioned rows align across components.
 */
final class Captions {

    private Captions() {
    }

    static @NotNull JBPanel<?> panel(final @NotNull String caption) {
        final JBPanel<?> captionPanel = new JBPanel<>(new GridBagLayout());
        captionPanel.setOpaque(false);

        final JBLabel label = new JBLabel(caption);
        label.setFont(JBUI.Fonts.smallFont());
        label.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        captionPanel.add(label);

        captionPanel.setPreferredSize(new Dimension(JBUI.scale(96), captionPanel.getPreferredSize().height));
        return captionPanel;
    }
}
