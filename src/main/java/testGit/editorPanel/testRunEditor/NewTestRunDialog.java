package testGit.editorPanel.testRunEditor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.TestRun;

import javax.swing.*;

public class NewTestRunDialog extends DialogWrapper {
    private final JBTextField buildNumberField = new JBTextField();
    private final ComboBox<String> platformCombo = new ComboBox<>(new String[]{"Windows", "Linux", "MacOS", "Android", "iOS"});
    private final ComboBox<String> languageCombo = new ComboBox<>(new String[]{"English", "Arabic", "French"});
    private final ComboBox<String> browserCombo = new ComboBox<>(new String[]{"Chrome", "Firefox", "Safari", "Edge"});
    private final ComboBox<String> deviceCombo = new ComboBox<>(new String[]{"Desktop", "Mobile", "Tablet"});

    public NewTestRunDialog() {
        super(true);
        setTitle("Create New Test Run");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Build number:", AllIcons.Actions.Edit, SwingConstants.LEFT), buildNumberField)
                .addSeparator()
                .addLabeledComponent(new JBLabel("Platform:", AllIcons.Nodes.PpLib, SwingConstants.LEFT), platformCombo)
                .addLabeledComponent(new JBLabel("Language:", AllIcons.Nodes.Lambda, SwingConstants.LEFT), languageCombo)
                .addLabeledComponent(new JBLabel("Browser:", AllIcons.Nodes.WebFolder, SwingConstants.LEFT), browserCombo)
                .addLabeledComponent(new JBLabel("Device type:", AllIcons.Nodes.Include, SwingConstants.LEFT), deviceCombo)
                .getPanel();
    }

    public TestRun getMetadata() {
        TestRun metadata = new TestRun();
        metadata.setBuildNumber(buildNumberField.getText());
        metadata.setPlatform((String) platformCombo.getSelectedItem());
        metadata.setLanguage((String) languageCombo.getSelectedItem());
        metadata.setBrowser((String) browserCombo.getSelectedItem());
        metadata.setDeviceType((String) deviceCombo.getSelectedItem());
        return metadata;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (buildNumberField.getText().trim().isEmpty()) {
            return new ValidationInfo("Build number is required", buildNumberField);
        }
        return null;
    }
}