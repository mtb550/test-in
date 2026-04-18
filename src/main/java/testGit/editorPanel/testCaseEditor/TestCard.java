package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.JBColor;
import testGit.editorPanel.BaseCard;
import testGit.editorPanel.Shared;
import testGit.pojo.TestEditorAttributes;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TestCard extends BaseCard {

    public TestCard() {
        super();
    }

    //todo: move it to test renerer ?
    public void updateData(final int index, final TestCaseDto tc, final Set<String> activeDetails, final boolean isUnsorted) {
        final List<JComponent> badges = new ArrayList<>();
        final Map<String, String> details = new LinkedHashMap<>();

        Arrays.stream(TestEditorAttributes.values())
                .filter(attr -> activeDetails.contains(attr.name()))
                .forEach(attr -> attr.applyToUI(tc, badges, details));

        if (isUnsorted) {
            badges.add(new Shared.RoundedBadge("Unsorted", new JBColor(new Color(255, 100, 100), new Color(130, 50, 50))));
        }

        updateUI(index, TestEditorAttributes.DESCRIPTION.getValue(tc), badges, details);
    }
}