package org.testin.editorPanel.runEditor;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.BaseCard;
import org.testin.enums.RunEditorAttributes;
import org.testin.mappers.TestRunItems;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class RunCard extends BaseCard {
    private final @NotNull Project p;
    private final @NotNull List<JComponent> badges = new ArrayList<>();
    private final @NotNull Map<String, String> details = new LinkedHashMap<>();

    public RunCard(final @NotNull Project p) {
        super();
        this.p = p;
    }

    @Override
    public void applyListFont(final Font listFont) {
        super.applyListFont(listFont);
    }

    public void updateData(final @NotNull Integer index, final @NotNull Set<?> activeDetails, final @NotNull TestRunItems runItem) {
        badges.clear();
        details.clear();

        Arrays.stream(RunEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(runItem, badges, details, p));

        updateUI(index, RunEditorAttributes.DESCRIPTION.getRunValueExtractor().execute(runItem, p), badges, details);

        final JBLabel statusLabel = attributeLabels.get(RunEditorAttributes.RUN_STATUS.getName());

        if (statusLabel != null) {
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
            final Color statusColor = runItem.getStatus().getRowColor();
            statusLabel.setForeground(Objects.requireNonNullElseGet(statusColor, UIUtil::getContextHelpForeground));
        }

    }
}