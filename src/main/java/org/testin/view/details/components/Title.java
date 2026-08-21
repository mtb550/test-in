package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Display;
import org.testin.util.FontSync;

import javax.swing.*;
import java.awt.*;

public class Title extends BaseDetails {

    final int INSETS_TOP = 20;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 0;
    final int INSETS_RIGHT = 16;

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {

        final @NotNull String titleText = Display.format(dto.getDescription());
        final @NotNull String finalValue = titleText.trim().isEmpty() ? "-" : titleText;

        final @NotNull JTextArea mainTitleArea = new JTextArea(finalValue);

        final float titleFontSize = FontSync.getBaseFontSize();
        mainTitleArea.setFont(JBFont.label().deriveFont(Font.BOLD, titleFontSize));

        mainTitleArea.setLineWrap(true);
        mainTitleArea.setWrapStyleWord(true);
        mainTitleArea.setOpaque(false);
        mainTitleArea.setEditable(false);
        mainTitleArea.setBorder(null);

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);

        panel.add(mainTitleArea, gbc);

        return currentRow + 1;
    }
}