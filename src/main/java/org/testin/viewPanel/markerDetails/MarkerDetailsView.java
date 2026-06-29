package org.testin.viewPanel.markerDetails;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.TestRunMarker;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.markers.TestProjectMarker;
import org.testin.util.FontSyncUtil;

import javax.swing.*;
import java.awt.*;
import java.time.ZonedDateTime;

public class MarkerDetailsView {

    private static final int LABEL_WIDTH = 255;
    private static final float LABEL_FONT_SIZE_OFFSET = 5.0f;
    private static final float VALUE_FONT_SIZE_OFFSET = 8.0f;
    private static final int INSETS_TOP = 12;
    private static final int INSETS_LEFT = 16;
    private static final int INSETS_BOTTOM = 12;
    private static final int INSETS_RIGHT = 8;
    private static final int VALUE_INSETS_RIGHT = 16;

    public static void show(final @NotNull Project project, final @NotNull DirectoryDto dto) {
        JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        row = addRow(panel, gbc, "Name:", dto.getName(), row);
        row = addRow(panel, gbc, "Path:", dto.getPath().toString(), row);
        row = addRow(panel, gbc, "Created By:", dto.getCreatedBy(), row);
        row = addRow(panel, gbc, "Created At:", formatDate(dto.getCreatedAt()), row);
        row = addRow(panel, gbc, "Modified By:", dto.getModifiedBy(), row);
        row = addRow(panel, gbc, "Modified At:", formatDate(dto.getModifiedAt()), row);

        if (dto instanceof TestProjectDirectoryDto projectDto) {
            TestProjectMarker marker = projectDto.getMarker();

            row = addRow(panel, gbc, "Status:", marker.getStatus() != null ? marker.getStatus().getDescription() : "", row);
            row = addRow(panel, gbc, "Marker Created By:", marker.getCreatedBy(), row);
            row = addRow(panel, gbc, "Marker Created At:", formatDate(marker.getCreatedAt()), row);

        } else if (dto instanceof TestRunDirectoryDto runDto) {
            TestRunMarker marker = runDto.getMarker();
            if (marker != null) {
                row = addRow(panel, gbc, "Status:", marker.getStatus().getLabel(), row);
                row = addRow(panel, gbc, "Marker Created By:", marker.getCreatedBy(), row);
                row = addRow(panel, gbc, "Marker Created At:", formatDate(marker.getCreatedAt()), row);
            }
        }
        // todo: add TestSetMarker support when implemented

        GridBagConstraints spacerGbc = new GridBagConstraints();
        spacerGbc.gridy = row;
        spacerGbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), spacerGbc);

        JBScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        ComponentPopupBuilder builder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, null)
                .setTitle("Details")
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .setMinSize(new Dimension(400, 300));

        JBPopup popup = builder.createPopup();
        popup.showCenteredInCurrentWindow(project);
    }

    private static int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc,
                              final @NotNull String labelText, final @NotNull String valueText, final int row) {
        if (valueText.trim().isEmpty())
            return row;

        JTextArea valueArea = new JTextArea(valueText);
        valueArea.setFont(JBFont.label().deriveFont(Font.PLAIN, FontSyncUtil.getBaseFontSize() + VALUE_FONT_SIZE_OFFSET));
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);
        valueArea.setOpaque(false);
        valueArea.setEditable(false);
        valueArea.setBorder(null);

        return addRow(panel, gbc, labelText, valueArea, row);
    }

    private static int addRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull String labelText, final @NotNull JComponent valueComponent, final int row) {
        gbc.gridy = row;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);

        JLabel label = new JLabel(labelText);
        label.setForeground(JBColor.GRAY);
        label.setFont(JBFont.label().deriveFont(Font.BOLD, FontSyncUtil.getBaseFontSize() + LABEL_FONT_SIZE_OFFSET));

        Dimension prefSize = label.getPreferredSize();
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

    private static String formatDate(final ZonedDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(Config.getDateFormatterPattern());
    }
}
