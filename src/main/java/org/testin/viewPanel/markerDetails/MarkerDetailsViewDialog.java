package org.testin.viewPanel.markerDetails;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.Config;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.markers.IMarker;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.FontSync;

import javax.swing.*;
import java.awt.*;
import java.time.ZonedDateTime;
import java.util.Optional;

public class MarkerDetailsViewDialog {
    final @NotNull Project p;
    final int LABEL_WIDTH = 255;
    final float LABEL_FONT_SIZE_OFFSET = 0.0f;
    final float VALUE_FONT_SIZE_OFFSET = 0.0f;
    final int INSETS_TOP = 12;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 12;
    final int INSETS_RIGHT = 8;
    final int VALUE_INSETS_RIGHT = 16;

    public MarkerDetailsViewDialog(final @NotNull Project p) {
        this.p = p;
    }

    public void show(final @NotNull DirectoryDto dto) {
        final JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        panel.setOpaque(false);
        DialogStyle.styleContent(panel);
        panel.setBorder(JBUI.Borders.empty(10));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Creation info comes from the marker - the only place it is stored -
        // through the IMarker contract; a new marker type needs no new branch.
        final IMarker marker = dto.getMarker();

        row = addRow(panel, gbc, "Name:", dto.getName(), row);
        row = addRow(panel, gbc, "Path:", dto.getPath().toString(), row);
        row = addRow(panel, gbc, "Created By:", marker.getCreatedBy(), row);
        row = addRow(panel, gbc, "Created At:", formatDate(marker.getCreatedAt()), row);
        row = addRow(panel, gbc, "Modified By:", marker.getModifiedBy(), row);
        row = addRow(panel, gbc, "Modified At:", formatDate(marker.getModifiedAt()), row);
        row = addRow(panel, gbc, "Status:", Optional.ofNullable(marker.getStatusLabel()).orElse(""), row);

        final GridBagConstraints spacerGbc = new GridBagConstraints();
        spacerGbc.gridy = row;
        spacerGbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), spacerGbc);

        final JBScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        final ComponentPopupBuilder builder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, null)
                .setTitle("Details")
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .setMinSize(new Dimension(400, 300));

        final JBPopup popup = builder.createPopup();
        popup.showCenteredInCurrentWindow(p);
    }

    private int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc,
                       final @NotNull String labelText, final @NotNull String valueText, final int row) {
        if (valueText.trim().isEmpty())
            return row;

        final JTextArea valueArea = new JTextArea(valueText);
        valueArea.setFont(JBFont.label().deriveFont(Font.PLAIN, FontSync.getBaseFontSize() + VALUE_FONT_SIZE_OFFSET));
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);
        valueArea.setOpaque(false);
        valueArea.setEditable(false);
        valueArea.setBorder(null);

        return addRow(panel, gbc, labelText, valueArea, row);
    }

    private int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull JComponent valueComponent, final int row) {
        gbc.gridy = row;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);

        final JBLabel label = new JBLabel(labelText);
        label.setForeground(JBColor.GRAY);
        label.setFont(JBFont.label().deriveFont(Font.BOLD, FontSync.getBaseFontSize() + LABEL_FONT_SIZE_OFFSET));

        final Dimension prefSize = label.getPreferredSize();
        label.setPreferredSize(new Dimension(LABEL_WIDTH, prefSize.height));
        label.setMinimumSize(new Dimension(LABEL_WIDTH, prefSize.height));

        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(INSETS_TOP, 0, INSETS_BOTTOM, VALUE_INSETS_RIGHT);

        panel.add(valueComponent, gbc);

        return row + 1;
    }

    private @NotNull String formatDate(final @Nullable ZonedDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(Config.getDateFormatterPattern());
    }
}
