package org.testin.editorPanel.testEditor;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.BaseCard;
import org.testin.editorPanel.Shared;
import org.testin.enums.TestCardStatus;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TestCard extends BaseCard {
    private final @NotNull Project p;
    private final List<JComponent> badges = new ArrayList<>();
    private final Map<String, String> details = new LinkedHashMap<>();
    private boolean isPendingCut = false;

    public TestCard(final @NotNull Project p) {
        super();
        this.p = p;
    }

    public void updateData(final int index, final @NotNull TestCaseDto tc, final Set<?> activeDetails, final boolean isUnsorted) {
        badges.clear();
        details.clear();

        this.isPendingCut = TestEditorContextMenu.isGlobalCutAction() && TestEditorContextMenu.getGlobalPendingCutIds().contains(tc.getId());

        Arrays.stream(TestEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(tc, badges, details, p));

        if (isUnsorted) {
            badges.add(new Shared.RoundedBadge("Unsorted", new JBColor(new Color(255, 100, 100), new Color(130, 50, 50))));
        }

        this.isRunning = "RUNNING".equals(tc.getTempStatus());

        final String tempStatus = tc.getTempStatus();

        if (!tempStatus.trim().isEmpty()) {
            final TestCardStatus status = TestCardStatus.from(tempStatus);
            if (status != null) {
                badges.add(new Shared.RoundedBadge(status.getLabel(), status.getBadgeColor()));
            } else {
                final JBColor gray = new JBColor(new Color(180, 180, 180), new Color(120, 120, 120));
                badges.add(new Shared.RoundedBadge(tempStatus, gray));
            }
        }

        updateUI(index, TestEditorAttributes.DESCRIPTION.getTestValueExtractor().execute(tc, p), badges, details);
    }

    @Override
    public void paint(Graphics g) {
        if (isPendingCut) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            super.paint(g2);
            g2.dispose();
        } else {
            super.paint(g);
        }
    }
}