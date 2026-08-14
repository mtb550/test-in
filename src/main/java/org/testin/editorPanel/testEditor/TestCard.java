package org.testin.editorPanel.testEditor;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.BaseCard;
import org.testin.editorPanel.RoundedBadge;
import org.testin.enums.RunStatus;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TestCard extends BaseCard {
    private final @NotNull Project p;
    private final @NotNull List<JComponent> badges = new ArrayList<>();
    private final @NotNull Map<String, String> details = new LinkedHashMap<>();
    private boolean isPendingCut = false;

    public TestCard(final @NotNull Project p) {
        super();
        this.p = p;
    }

    public void updateData(final int index, final @NotNull TestCaseDto tc, final @NotNull Set<?> activeDetails, final boolean isUnsorted) {
        badges.clear();
        details.clear();

        this.isPendingCut = TestEditorContextMenu.isGlobalCutAction() && TestEditorContextMenu.getGlobalPendingCutIds().contains(tc.getId());

        Arrays.stream(TestEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(tc, badges, details, p));

        if (isUnsorted) {
            badges.add(new RoundedBadge("Unsorted", new JBColor(new Color(255, 100, 100), new Color(130, 50, 50))));
        }

        final RunStatus runStatus = tc.getTempStatus();
        this.isRunning = runStatus == RunStatus.RUNNING;

        final RunStatus.Badge badge = runStatus.getBadge();
        if (badge != null) badges.add(new RoundedBadge(badge.label(), badge.color()));

        updateUI(index, TestEditorAttributes.DESCRIPTION.getTestValueExtractor().execute(tc, p), badges, details);
    }

    @Override
    public void paint(final Graphics g) {
        if (isPendingCut) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            super.paint(g2);
            g2.dispose();
        } else {
            super.paint(g);
        }
    }
}