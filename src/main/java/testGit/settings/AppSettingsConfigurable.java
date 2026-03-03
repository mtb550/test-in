package testGit.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AppSettingsConfigurable implements Configurable {

    private final TextFieldWithBrowseButton rootPathField = new TextFieldWithBrowseButton();

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        System.out.println("AppSettingsConfigurable.getDisplayName()");
        return "TestGit Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        System.out.println("AppSettingsConfigurable.createComponent()");

        rootPathField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Root Folder")
                        .withDescription("Choose the directory where your test projects are stored"),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Root projects folder: "), rootPathField, 1, false)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        System.out.println("AppSettingsConfigurable.isModified()");
        AppSettingsState settings = AppSettingsState.getInstance();
        return !rootPathField.getText().equals(settings.rootFolderPath);
    }

    @Override
    public void apply() {
        System.out.println("AppSettingsConfigurable.apply()");
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.rootFolderPath = rootPathField.getText();
    }

    @Override
    public void reset() {
        System.out.println("AppSettingsConfigurable.reset()");
        AppSettingsState settings = AppSettingsState.getInstance();
        rootPathField.setText(settings.rootFolderPath);
    }
}