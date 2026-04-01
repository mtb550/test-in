package testGit.viewPanel.details.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public abstract class BaseDetails {

    protected static final int LABEL_WIDTH = 255;
    protected static final float LABEL_FONT_SIZE = 20.0f;
    protected static final float VALUE_FONT_SIZE = 23.0f;
    private static final String EMPTY_VALUE_PLACEHOLDER = "-";
    private static final int LABEL_INSETS_TOP = 12;
    private static final int LABEL_INSETS_LEFT = 16;
    private static final int LABEL_INSETS_BOTTOM = 12;
    private static final int LABEL_INSETS_RIGHT = 8;
    private static final int VALUE_INSETS_TOP = 12;
    private static final int VALUE_INSETS_LEFT = 0;
    private static final int VALUE_INSETS_BOTTOM = 12;
    private static final int VALUE_INSETS_RIGHT = 16;

    public abstract int render(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow);

    protected int addRow(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final String labelText, @Nullable final String valueText, final int row) {

        final String finalValue = (valueText == null || valueText.trim().isEmpty()) ? EMPTY_VALUE_PLACEHOLDER : valueText;

        final JTextArea valueArea = new JTextArea(finalValue);
        valueArea.setFont(JBFont.label().deriveFont(Font.PLAIN, VALUE_FONT_SIZE));
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);
        valueArea.setOpaque(false);
        valueArea.setEditable(false);
        valueArea.setBorder(null);

        return addRow(panel, gbc, labelText, valueArea, row);
    }

    protected int addRow(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final String labelText, @NotNull final JComponent valueComponent, final int row) {

        gbc.gridy = row;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(LABEL_INSETS_TOP, LABEL_INSETS_LEFT, LABEL_INSETS_BOTTOM, LABEL_INSETS_RIGHT);

        final JBLabel label = new JBLabel(labelText);
        label.setForeground(JBColor.GRAY);
        label.setFont(JBFont.label().deriveFont(Font.BOLD, LABEL_FONT_SIZE));

        final Dimension prefSize = label.getPreferredSize();
        label.setPreferredSize(new Dimension(LABEL_WIDTH, prefSize.height));
        label.setMinimumSize(new Dimension(LABEL_WIDTH, prefSize.height));

        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(VALUE_INSETS_TOP, VALUE_INSETS_LEFT, VALUE_INSETS_BOTTOM, VALUE_INSETS_RIGHT);

        panel.add(valueComponent, gbc);

        return row + 1;
    }
}