package org.testin.Dialogs;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestRunConfiguration;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.DirectoryDto;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class RunCreationForm {

    private final JBPanel<?> mainPanel;
    private final CheckboxTree checklistTree;

    private final Map<TestRunConfiguration, JComponent> fieldMap = new EnumMap<>(TestRunConfiguration.class);

    public RunCreationForm(final String runName, final CheckedTreeNode root, final Map<UUID, TestRunItems> resultsMap) {
        mainPanel = new JBPanel<>(new BorderLayout());

        JBTextField runNameField = new JBTextField(runName);
        runNameField.setEditable(false);
        runNameField.setEnabled(false);

        FormBuilder formBuilder = FormBuilder.createFormBuilder()
                .addLabeledComponent("Run name:", runNameField);

        for (TestRunConfiguration field : TestRunConfiguration.values()) {

            JComponent inputComponent = createEditableCombo(field.getOptions());
            fieldMap.put(field, inputComponent);

            JBLabel label = new JBLabel(field.getDisplayName() + ":", field.getIcon(), SwingConstants.LEFT);

            if (field == TestRunConfiguration.PLATFORM) {
                formBuilder.addSeparator();
            }

            formBuilder.addLabeledComponent(label, inputComponent);
        }

        JPanel configurationPanel = formBuilder.getPanel();
        configurationPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(10)
        ));

        mainPanel.add(configurationPanel, BorderLayout.NORTH);

        checklistTree = new CheckboxTree(createTreeRenderer(resultsMap), root, new CheckboxTreeBase.CheckPolicy(true, true, true, true));
        TreeUtil.expandAll(checklistTree);

        mainPanel.add(new JBScrollPane(checklistTree), BorderLayout.CENTER);
    }

    private ComboBox<String> createEditableCombo(final String[] items) {
        ComboBox<String> comboBox = new ComboBox<>(items != null ? items : new String[0]);
        comboBox.setEditable(true);
        return comboBox;
    }

    private CheckboxTree.CheckboxTreeCellRenderer createTreeRenderer(final Map<UUID, TestRunItems> resultsMap) {
        return new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(final @NotNull JTree tree, final @NotNull Object value, final boolean selected,
                                          final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
                if (!(value instanceof CheckedTreeNode node)) return;
                final Object userObj = node.getUserObject();

                if (userObj instanceof DirectoryDto dir) {
                    getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

                } else if (userObj instanceof TestCaseDto tc) {
                    final TestRunItems result = resultsMap.get(tc.getId());
                    if (result != null) {
                        final TestStatus status = result.getStatus();
                        getTextRenderer().append(tc.getDescription(), status.getStyle());
                        getTextRenderer().append(status.getDisplayText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);

                    } else {
                        getTextRenderer().append(tc.getDescription(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    }

                } else if (userObj instanceof String str) {
                    getTextRenderer().append(str, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }
        };
    }

    public void populateConfiguration(final TestRunDto tr) {
        tr.setPlatform(getFieldValue(TestRunConfiguration.PLATFORM))
                .setLanguage(getFieldValue(TestRunConfiguration.LANGUAGE))
                .setBrowser(getFieldValue(TestRunConfiguration.BROWSER))
                .setDeviceType(getFieldValue(TestRunConfiguration.DEVICE_TYPE));
    }

    public String getFieldValue(final TestRunConfiguration field) {
        JComponent comp = fieldMap.get(field);

        if (comp instanceof JBTextField textField)
            return textField.getText().trim();

        else if (comp instanceof ComboBox<?> comboBox)
            return String.valueOf(comboBox.getSelectedItem()).trim();

        return "";
    }
}