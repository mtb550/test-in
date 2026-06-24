package org.testin.editorPanel.runEditor;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.BaseCard;
import org.testin.pojo.RunEditorAttributes;
import org.testin.pojo.TestRunItems;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class RunCard extends BaseCard {
    private final Project project;
    private final List<JComponent> badges = new ArrayList<>();
    private final Map<String, String> details = new LinkedHashMap<>();

    public RunCard(final @NotNull Project project) {
        super();
        this.project = project;
    }

    @Override
    public void applyListFont(final Font listFont) {
        super.applyListFont(listFont);
    }

    public void updateData(final int index, final Set<?> activeDetails, final TestRunItems runItem) {
        badges.clear();
        details.clear();

        Arrays.stream(RunEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(runItem, badges, details, project));

        updateUI(index, RunEditorAttributes.DESCRIPTION.getValueExtractor().apply(runItem, project), badges, details);

        if (runItem != null) {
            final JBLabel statusLabel = attributeLabels.get(RunEditorAttributes.RUN_STATUS.getName());

            if (statusLabel != null) {
                statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
                final Color statusColor = runItem.getStatus().getRowColor();
                statusLabel.setForeground(Objects.requireNonNullElseGet(statusColor, UIUtil::getContextHelpForeground));
            }

        }
    }
}