package org.testin.editor.test;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Badges;
import org.testin.clipboard.CutState;
import org.testin.codegen.AutomationState;
import org.testin.editor.BaseCard;
import org.testin.editor.CardHoverAction;
import org.testin.model.RunStatus;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;

import java.awt.*;
import java.util.*;
import java.util.List;

public class TestCard extends BaseCard {
    private final @NotNull Project p;
    private final @NotNull List<Badges.Badge> badges = new ArrayList<>();
    private final @NotNull Map<String, String> details = new LinkedHashMap<>();
    private boolean isPendingCut = false;

    public TestCard(final @NotNull Project p) {
        super();
        this.p = p;
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-021
    public void updateData(final int index, final @NotNull TestCaseDto tc, final @NotNull Set<?> activeDetails, final @NotNull String title) {
        badges.clear();
        details.clear();

        this.isPendingCut = Services.getInstance(p, CutState.class).isPending(tc.getId());

        // Read here beside the cut state, for the same reason: a card learns
        // everything it draws at the moment it is filled in. Whatever is known
        // right now - unknown on the first paint of a page, which draws exactly
        // what the button always drew.
        this.automation = Services.getInstance(p, AutomationState.class).of(tc.getId());

        Arrays.stream(TestEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(tc, badges, details));

        final @NotNull RunStatus runStatus = Services.getInstance(p, TestNGExecution.class).statusOf(tc);
        this.runSlot = CardHoverAction.runSlot(p, tc);

        if (runStatus.hasBadge()) badges.add(Badges.createRunStatusBadge(runStatus.getBadge()));

        updateUI(index, title, badges, details);
    }

    // UC-EDITOR-PANEL-016, Rule-EDITOR-PANEL-078
    @Override
    public void paint(final Graphics g) {
        if (isPendingCut) {
            final @NotNull Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            super.paint(g2);
            g2.dispose();
        } else {
            super.paint(g);
        }
    }
}