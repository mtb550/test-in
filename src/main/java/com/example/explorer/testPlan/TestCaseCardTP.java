package com.example.explorer.testPlan;

import com.example.pojo.TestCase;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class TestCaseCardTP extends JPanel {

    public TestCaseCardTP(int index, TestCase tc) {
        // استخدام BorderLayout مع هوامش محسنة
        setLayout(new BorderLayout());
        setOpaque(true);

        // تحسين الألوان باستخدام درجات متوافقة مع Darcula و Light themes
        setBackground(index % 2 == 0
                ? JBColor.namedColor("Table.alternateRowBackground", new JBColor(Gray._245, Gray._60))
                : JBColor.namedColor("Table.background", new JBColor(Gray._230, Gray._45)));

        setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0), // خط سفلي فقط للفصل
                JBUI.Borders.empty(12, 15) // هوامش داخلية مريحة للعين
        ));

        // محتوى البطاقة (النصوص)
        JBPanel<?> content = createContentPanel(index, tc);
        add(content, BorderLayout.CENTER);

        // منع البطاقة من التمدد بشكل مفرط
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
    }

    private JBPanel<?> createContentPanel(int index, TestCase tc) {
        JBPanel<?> content = new JBPanel<>(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // 1. العنوان والـ Badge
        JBPanel<?> titleLine = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleLine.setOpaque(false);

        JBLabel title = new JBLabel("#" + (index + 1) + ". " + tc.getTitle());
        title.setFont(JBUI.Fonts.label(13).asBold());

        titleLine.add(title);
        titleLine.add(Box.createHorizontalStrut(10));
        titleLine.add(createPriorityBadge(tc));

        // 2. النتائج المتوقعة
        JBLabel expected = createSubLabel("Expected: " + tc.getExpectedResult(), false);
        // 3. الخطوات
        JBLabel steps = createSubLabel("Steps: " + tc.getSteps(), false);
        // 4. مرجع الأتمتة
        JBLabel autoRef = createSubLabel("Automation Ref: " + tc.getAutomationRef(), true);

        // إضافة العناصر للـ GridBag
        gbc.gridy = 0;
        content.add(titleLine, gbc);
        gbc.gridy = 1;
        gbc.insets = JBUI.insetsTop(4);
        content.add(expected, gbc);
        gbc.gridy = 2;
        content.add(steps, gbc);
        gbc.gridy = 3;
        gbc.insets = JBUI.insetsTop(2);
        content.add(autoRef, gbc);

        return content;
    }

    private JBLabel createSubLabel(String text, boolean italic) {
        JBLabel label = new JBLabel(text);
        label.setFont(JBUI.Fonts.label(11).asItalic());
        label.setForeground(JBColor.namedColor("Label.infoForeground", new JBColor(Gray._100, Gray._160)));
        return label;
    }

    private @NotNull JBLabel createPriorityBadge(TestCase tc) {
        String priority = tc.getPriority().toUpperCase();
        JBLabel badge = new JBLabel(priority);
        badge.setFont(JBUI.Fonts.label(10).asBold());
        badge.setOpaque(true);
        badge.setForeground(JBColor.WHITE);

        // استخدام ألوان هادئة للـ Badge
        badge.setBackground(switch (priority.toLowerCase()) {
            case "high" -> new JBColor(new Color(194, 84, 80), new Color(139, 58, 55));
            case "medium" -> new JBColor(new Color(81, 145, 171), new Color(52, 93, 110));
            default -> new JBColor(new Color(103, 169, 103), new Color(69, 114, 69));
        });

        badge.setBorder(JBUI.Borders.empty(1, 6));
        return badge;
    }
}