package org.testin.viewPanel.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.Shared;
import org.testin.enums.Group;
import org.testin.mappers.dto.TestCaseDto;

import java.awt.*;

public class Badges extends BaseDetails {

    final int FLOW_GAP = 6;
    final int INSETS_TOP = 8;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 16;
    final int INSETS_RIGHT = 16;

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final JBPanel<?> badgesPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(FLOW_GAP), 0));
        badgesPanel.setOpaque(false);

        badgesPanel.add(Shared.createPriorityBadge(dto));

        if (!dto.getGroup().isEmpty()) {
            for (final Group group : dto.getGroup()) {
                if (group != null) {
                    badgesPanel.add(Shared.createGroupBadge(group));
                }
            }
        }

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);
        panel.add(badgesPanel, gbc);

        return currentRow + 1;
    }
}