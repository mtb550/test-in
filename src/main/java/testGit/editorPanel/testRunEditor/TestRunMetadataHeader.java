package testGit.editorPanel.testRunEditor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import testGit.pojo.dto.TestRunDto;

import javax.swing.*;

public class TestRunMetadataHeader {
    private final JBTextField buildNumberField = new JBTextField();
    private final ComboBox<String> platformCombo = new ComboBox<>(new String[]{"Windows", "Linux", "MacOS", "Android", "iOS"});
    private final ComboBox<String> languageCombo = new ComboBox<>(new String[]{"English", "Arabic", "French"});
    private final ComboBox<String> browserCombo = new ComboBox<>(new String[]{"Chrome", "Firefox", "Safari", "Edge"});
    private final ComboBox<String> deviceCombo = new ComboBox<>(new String[]{"Desktop", "Mobile", "Tablet"});

    @Getter
    private final JPanel panel;

    public TestRunMetadataHeader() {
        this.panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Build number:", AllIcons.Actions.Edit, SwingConstants.LEFT), buildNumberField)
                .addSeparator()
                .addLabeledComponent(new JBLabel("Platform:", AllIcons.Nodes.PpLib, SwingConstants.LEFT), platformCombo)
                .addLabeledComponent(new JBLabel("Language:", AllIcons.Nodes.Lambda, SwingConstants.LEFT), languageCombo)
                .addLabeledComponent(new JBLabel("Browser:", AllIcons.Nodes.WebFolder, SwingConstants.LEFT), browserCombo)
                .addLabeledComponent(new JBLabel("Device type:", AllIcons.Nodes.Include, SwingConstants.LEFT), deviceCombo)
                .getPanel();

        this.panel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(10)
        ));
    }

    public void applyToMetadata(final TestRunDto metadata) {
        if (metadata == null) return;
        metadata.setBuildNumber(buildNumberField.getText().trim());
        metadata.setPlatform((String) platformCombo.getSelectedItem());
        metadata.setLanguage((String) languageCombo.getSelectedItem());
        metadata.setBrowser((String) browserCombo.getSelectedItem());
        metadata.setDeviceType((String) deviceCombo.getSelectedItem());
    }

    public boolean validate() {
        return !buildNumberField.getText().trim().isEmpty();
    }

    public void setRunNameDisabled(final String name) {
        buildNumberField.setText(name);
        buildNumberField.setEditable(false);
        buildNumberField.setEnabled(false);
    }
}