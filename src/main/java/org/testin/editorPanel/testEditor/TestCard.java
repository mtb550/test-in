package org.testin.editorPanel.testEditor;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.BaseCard;
import org.testin.editorPanel.Shared;
import org.testin.enums.TestEditorAttributes;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TestCard extends BaseCard {
    private final Project project;
    private final List<JComponent> badges = new ArrayList<>();
    private final Map<String, String> details = new LinkedHashMap<>();
    private boolean isPendingCut = false;

    public TestCard(final @NotNull Project p) {
        super();
        this.project = p;
    }

    public void updateData(final int index, final @NotNull TestCaseDto tc, final Set<?> activeDetails, final boolean isUnsorted) {
        badges.clear();
        details.clear();

        this.isPendingCut = TestEditorCM.isGlobalCutAction() && TestEditorCM.getGlobalPendingCutIds().contains(tc.getId());

        Arrays.stream(TestEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(tc, badges, details, project));

        if (isUnsorted) {
            badges.add(new Shared.RoundedBadge("Unsorted", new JBColor(new Color(255, 100, 100), new Color(130, 50, 50))));
        }

        this.isRunning = "RUNNING".equals(tc.getTempStatus());

        final String tempStatus = tc.getTempStatus();

        if (!tempStatus.trim().isEmpty()) {
            final Color badgeColor;
            final String displayText = switch (tempStatus) {
                case "RUNNING" -> {
                    badgeColor = new JBColor(new Color(255, 200, 100), new Color(200, 150, 50));
                    yield "Running";
                }
                case "PASSED" -> {
                    badgeColor = new JBColor(new Color(100, 200, 100), new Color(50, 150, 50));
                    yield "Passed";
                }
                case "FAILED" -> {
                    badgeColor = new JBColor(new Color(255, 100, 100), new Color(180, 50, 50));
                    yield "Failed";
                }
                default -> {
                    badgeColor = new JBColor(new Color(180, 180, 180), new Color(120, 120, 120));
                    yield tempStatus;
                }
            };
            badges.add(new Shared.RoundedBadge(displayText, badgeColor));
        }

        updateUI(index, TestEditorAttributes.DESCRIPTION.getValueExtractor().apply(tc, project), badges, details);
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