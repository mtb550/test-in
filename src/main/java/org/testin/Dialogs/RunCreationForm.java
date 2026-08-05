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
import org.testin.enums.TestRunConfiguration;
import org.testin.enums.TestStatus;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class RunCreationForm {

    private final JBPanel<?> mainPanel;
    private final CheckboxTree checklistTree;

    private final JBTextField descriptionField;
    private final JBTextField commitIdField;
    private final Map<TestRunConfiguration, JComponent> fieldMap = new EnumMap<>(TestRunConfiguration.class);


    public RunCreationForm(final @NotNull String runName, final CheckedTreeNode root, final @NotNull Map<UUID, TestRunItems> resultsMap) {
        mainPanel = new JBPanel<>(new BorderLayout());

        JBTextField runNameField = new JBTextField(runName);
        runNameField.setEditable(false);
        runNameField.setEnabled(false);

        FormBuilder formBuilder = FormBuilder.createFormBuilder().addLabeledComponent("Test Run name:", runNameField);

        descriptionField = new JBTextField();
        commitIdField = new JBTextField();
        formBuilder.addLabeledComponent(TestRunConfiguration.RELEASE_NOTES.getDisplayName(), descriptionField);
        formBuilder.addLabeledComponent(TestRunConfiguration.COMMIT_ID.getDisplayName(), commitIdField);

        for (TestRunConfiguration field : TestRunConfiguration.values()) {
            if (field.getOptions() != null) {
                JComponent inputComponent = createEditableCombo(field.getOptions());
                fieldMap.put(field, inputComponent);
                JBLabel label = new JBLabel(field.getDisplayName() + ":", field.getIcon(), SwingConstants.LEFT);
                formBuilder.addLabeledComponent(label, inputComponent);
            }
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

    private ComboBox<String> createEditableCombo(final @NotNull String[] items) {
        ComboBox<String> comboBox = new ComboBox<>(items);
        comboBox.setEditable(true);
        return comboBox;
    }

    private CheckboxTree.CheckboxTreeCellRenderer createTreeRenderer(final @NotNull Map<UUID, TestRunItems> resultsMap) {
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

    public String getFieldValue(final @NotNull TestRunConfiguration field) {
        JComponent comp = fieldMap.get(field);

        if (comp instanceof ComboBox<?> comboBox) {
            Object selected = comboBox.getSelectedItem();
            return selected != null ? selected.toString().trim() : "";
        }

        return "";
    }
}