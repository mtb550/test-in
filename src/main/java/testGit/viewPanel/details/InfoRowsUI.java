package testGit.viewPanel.details;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InfoRowsUI {

    /**
     * يقوم ببناء جميع الصفوف ويعيد رقم الصف (Row) الأخير بعد الانتهاء
     */
    public static void build(@NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int startRow) {
        int row = startRow;

        addRow("Expected Result:", createValueLabel(dto.getExpected()), panel, gbc, row++);
        addRow("Steps:", createStepsLabel(dto.getSteps()), panel, gbc, row++);
        addRow("Automation Ref:", createValueLabel(dto.getAutoRef()), panel, gbc, row++);
        addRow("Business Ref:", createValueLabel(dto.getBusiRef()), panel, gbc, row++);
        addRow("Module:", createValueLabel(dto.getModule()), panel, gbc, row++);
        addRow("Created By:", createValueLabel(dto.getCreateBy()), panel, gbc, row++);
        addRow("Updated By:", createValueLabel(dto.getUpdateBy()), panel, gbc, row++);
        addRow("Created At:", createValueLabel(dto.getCreateAt() != null ? dto.getCreateAt().toString() : "-"), panel, gbc, row++);
        addRow("Updated At:", createValueLabel(dto.getUpdateAt() != null ? dto.getUpdateAt().toString() : "-"), panel, gbc, row);

    }

    @NotNull
    private static JBLabel createStepsLabel(@Nullable List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            JBLabel label = new JBLabel("-");
            label.setFont(JBUI.Fonts.label(14));
            return label;
        }

        StringBuilder html = new StringBuilder("<html><body style='padding: 0; margin: 0;'>");
        for (int i = 0; i < steps.size(); i++) {
            String formatted = DetailsUtil.format(steps.get(i));
            String escaped = StringUtil.escapeXmlEntities(formatted);

            html.append("<p style='margin-top: 3px; margin-bottom: 5px;'>")
                    .append("<b>").append((i + 1)).append("-</b> ").append(escaped)
                    .append("</p>");
        }
        html.append("</body></html>");

        JBLabel label = new JBLabel(html.toString());
        label.setFont(JBUI.Fonts.label(14));
        return label;
    }

    @NotNull
    private static JBLabel createValueLabel(@Nullable String text) {
        if (StringUtil.isEmptyOrSpaces(text)) {
            JBLabel label = new JBLabel("-");
            label.setFont(JBUI.Fonts.label(14));
            return label;
        }

        String formatted = DetailsUtil.format(text);
        String escaped = StringUtil.escapeXmlEntities(formatted);

        StringBuilder html = new StringBuilder("<html><body style='padding: 0; margin: 0;'>");
        String[] lines = escaped.split("\n");
        for (String line : lines) {
            html.append("<p style='margin-top: 3px; margin-bottom: 5px;'>").append(line).append("</p>");
        }
        html.append("</body></html>");

        JBLabel label = new JBLabel(html.toString());
        label.setFont(JBUI.Fonts.label(14));
        return label;
    }

    private static void addRow(@NotNull String label, @NotNull JComponent input, @NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, int row) {
        JBLabel keyLabel = new JBLabel(label);
        keyLabel.setFont(JBUI.Fonts.label(14));
        keyLabel.setForeground(Gray._120);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(8, 16, 6, 10);
        panel.add(keyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(6, 0, 6, 16);
        panel.add(input, gbc);
    }
}