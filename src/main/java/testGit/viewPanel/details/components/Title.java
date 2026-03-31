package testGit.viewPanel.details.components;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.Tools;

import java.awt.*;

public class Title extends BaseDetails {
    @Override
    public int render(@NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int currentRow) {
        JBLabel mainTitleLabel = new JBLabel(Tools.format(dto.getTitle()));
        mainTitleLabel.setFont(JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f));
        mainTitleLabel.setForeground(UIUtil.getLabelForeground());

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 16, 4, 16);
        panel.add(mainTitleLabel, gbc);

        return currentRow + 1;
    }
}