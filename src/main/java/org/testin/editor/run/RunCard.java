package org.testin.editor.run;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.BaseCard;
import org.testin.editor.CardHoverAction;
import org.testin.editor.Shared;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestRunItems;

import java.awt.*;
import java.util.*;
import java.util.List;

public class RunCard extends BaseCard {
    private final @NotNull Project p;
    private final @NotNull List<Shared.Badge> badges = new ArrayList<>();
    private final @NotNull Map<String, String> details = new LinkedHashMap<>();

    public RunCard(final @NotNull Project p) {
        super();
        this.p = p;
    }

    public void updateData(final @NotNull Integer index, final @NotNull Set<?> activeDetails, final @NotNull TestRunItems runItem, final @NotNull String title) {
        badges.clear();
        details.clear();

        Arrays.stream(RunEditorAttributes.values())
                .filter(activeDetails::contains)
                .forEach(attr -> attr.applyToUI(runItem, badges, details, p));

        this.runSlot = CardHoverAction.runSlot(runItem.requireTc().getTempStatus());

        updateUI(index, title, badges, details);

        final JBLabel statusLabel = attributeLabels.get(RunEditorAttributes.RUN_STATUS.getName());

        if (statusLabel != null) {
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
            statusLabel.setForeground(runItem.getStatus().getRowColor());
        }

    }
}