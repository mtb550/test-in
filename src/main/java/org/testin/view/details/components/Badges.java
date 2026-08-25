package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.Shared;
import org.testin.model.Group;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Badges extends BaseDetails {

    final int FLOW_GAP = 6;
    final int INSETS_TOP = 8;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 16;
    final int INSETS_RIGHT = 16;

    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull JBPanel<?> badgesPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(FLOW_GAP), 0));
        badgesPanel.setOpaque(false);

        final @NotNull List<Shared.Badge> badges = new ArrayList<>();
        badges.add(Shared.createPriorityBadge(dto));

        for (final Group group : dto.getGroup()) {
            badges.add(Shared.createGroupBadge(group));
        }

        // Last, the way a card orders them. None for a case nobody has run.
        final @NotNull RunStatus tempStatus = Services.getInstance(p, TestNGExecution.class).statusOf(dto);
        if (tempStatus.hasBadge()) badges.add(Shared.createRunStatusBadge(tempStatus.getBadge()));

        Shared.showBadges(badgesPanel, badges);

        return addFullWidthRow(panel, gbc, badgesPanel,
                JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT), currentRow);
    }
}