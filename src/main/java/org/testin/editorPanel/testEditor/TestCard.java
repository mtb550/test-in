package org.testin.editorPanel.testEditor;

import com.intellij.ui.JBColor;
import org.testin.editorPanel.BaseCard;
import org.testin.editorPanel.Shared;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TestCard extends BaseCard {
    private final List<JComponent> badges = new ArrayList<>();
    private final Map<String, String> details = new LinkedHashMap<>();
    private boolean isPendingCut = false;

    public TestCard() {
        super();
    }

    public void updateData(final int index, final TestCaseDto tc, final Set<?> activeDetails, final boolean isUnsorted) {
        badges.clear();
        details.clear();

        this.isPendingCut = TestEditorCM.isGlobalCutAction() && tc != null && TestEditorCM.getGlobalPendingCutIds().contains(tc.getId());

        Arrays.stream(TestEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(tc, badges, details));

        if (isUnsorted) {
            badges.add(new Shared.RoundedBadge("Unsorted", new JBColor(new Color(255, 100, 100), new Color(130, 50, 50))));
        }

        updateUI(index, TestEditorAttributes.DESCRIPTION.getValueExtractor().apply(tc), badges, details);
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