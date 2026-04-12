package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import testGit.pojo.RunMetadataFields;
import testGit.pojo.dto.TestRunDto;

import javax.swing.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

// TODO: remove this metadata class and put make its logic inside run editor.
public class TestRunMetadataHeader {

    private final Map<RunMetadataFields, JComponent> fieldsMap = new EnumMap<>(RunMetadataFields.class);

    @Getter
    private final JPanel panel;

    public TestRunMetadataHeader() {
        final FormBuilder builder = FormBuilder.createFormBuilder();

        for (final RunMetadataFields field : RunMetadataFields.values()) {
            final JBLabel label = new JBLabel(field.getDisplayName() + ":", field.getIcon(), SwingConstants.LEFT);

            final JComponent component = Optional.ofNullable(field.getOptions())
                    .map(options -> (JComponent) new ComboBox<>(options))
                    .orElseGet(JBTextField::new);

            fieldsMap.put(field, component);
            builder.addLabeledComponent(label, component);

            if (field == RunMetadataFields.BUILD_NUMBER) {
                builder.addSeparator();
            }
        }

        this.panel = builder.getPanel();
        this.panel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(10)
        ));
    }

    public void applyToMetadata(final TestRunDto metadata) {
        Optional.ofNullable(metadata).ifPresent(m -> {
            m.setBuildNumber(getFieldValue(RunMetadataFields.BUILD_NUMBER));
            m.setPlatform(getFieldValue(RunMetadataFields.PLATFORM));
            m.setLanguage(getFieldValue(RunMetadataFields.LANGUAGE));
            m.setBrowser(getFieldValue(RunMetadataFields.BROWSER));
            m.setDeviceType(getFieldValue(RunMetadataFields.DEVICE_TYPE));
        });
    }

    private String getFieldValue(final RunMetadataFields field) {
        return Optional.ofNullable(fieldsMap.get(field))
                .map(comp -> comp instanceof JTextField tf ? tf.getText().trim() :
                        comp instanceof ComboBox<?> cb ? String.valueOf(cb.getSelectedItem()) : "")
                .orElse("");
    }

    public boolean validate() {
        final JComponent buildField = fieldsMap.get(RunMetadataFields.BUILD_NUMBER);
        return buildField instanceof JTextField && !((JTextField) buildField).getText().trim().isEmpty();
    }

    public void setRunNameDisabled(final String name) {
        final JComponent buildField = fieldsMap.get(RunMetadataFields.BUILD_NUMBER);
        if (buildField instanceof JTextField textField) {
            textField.setText(name);
            textField.setEditable(false);
            textField.setEnabled(false);
        }
    }
}