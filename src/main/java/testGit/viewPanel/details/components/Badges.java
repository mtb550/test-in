package testGit.viewPanel.details.components;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.Shared;
import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public class Badges extends BaseDetails {
    @Override
    public int render(@NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int currentRow) {
        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        badgesPanel.setOpaque(false);

        if (dto.getPriority() != null) {
            badgesPanel.add(Shared.createPriorityBadge(dto));
        }

        if (dto.getGroups() != null && !dto.getGroups().isEmpty()) {
            for (Groups groups : dto.getGroups()) {
                if (groups != null) {
                    badgesPanel.add(Shared.createGroupBadge(groups));
                }
            }
        }

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 16, 16, 16);
        panel.add(badgesPanel, gbc);

        return currentRow + 1;
    }
}