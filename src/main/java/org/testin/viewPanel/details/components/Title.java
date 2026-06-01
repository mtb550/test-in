package org.testin.viewPanel.details.components;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.FontSyncUtil;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.*;

public class Title extends BaseDetails {

    private static final int INSETS_TOP = 20;
    private static final int INSETS_LEFT = 16;
    private static final int INSETS_BOTTOM = 0;
    private static final int INSETS_RIGHT = 16;

    @Override
    public int render(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {

        final String titleText = Tools.getInstance().format(dto.getDescription());
        final String finalValue = titleText.trim().isEmpty() ? "-" : titleText;

        final JTextArea mainTitleArea = new JTextArea(finalValue);

        float titleFontSize = FontSyncUtil.getBaseFontSize() + 11.0f;
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