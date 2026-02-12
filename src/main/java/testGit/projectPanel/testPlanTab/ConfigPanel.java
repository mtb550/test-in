package testGit.projectPanel.testPlanTab;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ConfigPanel {
    private ComboBox<String> platformCombo;
    private ComboBox<String> languageCombo;
    private ComboBox<String> browserCombo;
    private ComboBox<String> deviceCombo;
    private JBLabel browserLabel;  // Moved to class level
    private JBLabel deviceLabel;  // Moved to class level

    public JComponent createPanel() {
        JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;

        addPlatformComponents(panel, gbc);
        addLanguageComponents(panel, gbc);
        addBrowserComponents(panel, gbc);
        addDeviceComponents(panel, gbc);

        platformCombo.addActionListener(e -> updateVisibility());

        return panel;
    }

    private void addPlatformComponents(JBPanel<?> panel, GridBagConstraints gbc) {
        JBLabel platformLabel = new JBLabel("🧱 Platform:");
        platformCombo = new ComboBox<>(new String[]{"", "api", "web", "mobile"});
        panel.add(platformLabel, gbc);
        gbc.gridx++;
        panel.add(platformCombo, gbc);
    }

    private void addLanguageComponents(JBPanel<?> panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        JBLabel languageLabel = new JBLabel("🌍 Language:");
        languageCombo = new ComboBox<>(new String[]{"", "en", "ar"});
        panel.add(languageLabel, gbc);
        gbc.gridx++;
        panel.add(languageCombo, gbc);
    }

    private void addBrowserComponents(JBPanel<?> panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        browserLabel = new JBLabel("🌐 Browser:");  // Now assigns to class field
        browserLabel.setToolTipText("Hidden when platform is API or Mobile");
        browserCombo = new ComboBox<>(new String[]{"", "chrome", "safari", "edge", "firefox"});
        panel.add(browserLabel, gbc);
        gbc.gridx++;
        panel.add(browserCombo, gbc);
    }

    private void addDeviceComponents(JBPanel<?> panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        deviceLabel = new JBLabel("📱 Device Type:");  // Now assigns to class field
        deviceLabel.setToolTipText("Hidden when platform is API or Web");
        deviceCombo = new ComboBox<>(new String[]{"", "iPhone", "Android", "Huawei"});
        panel.add(deviceLabel, gbc);
        gbc.gridx++;
        panel.add(deviceCombo, gbc);
    }

    private void updateVisibility() {
        String selected = (String) platformCombo.getSelectedItem();
        boolean isApi = "api".equalsIgnoreCase(selected);
        boolean isWeb = "web".equalsIgnoreCase(selected);
        boolean isMobile = "mobile".equalsIgnoreCase(selected);

        browserCombo.setVisible(!isApi && !isMobile);
        browserLabel.setVisible(!isApi && !isMobile);
        deviceCombo.setVisible(!isApi && !isWeb);
        deviceLabel.setVisible(!isApi && !isWeb);
    }

    public String getPlatform() {
        return Objects.requireNonNull(platformCombo.getSelectedItem()).toString();
    }

    public String getLanguage() {
        return Objects.requireNonNull(languageCombo.getSelectedItem()).toString();
    }

    public String getBrowser() {
        return browserCombo.isVisible() ? Objects.requireNonNull(browserCombo.getSelectedItem()).toString() : null;
    }

    public String getDeviceType() {
        return deviceCombo.isVisible() ? Objects.requireNonNull(deviceCombo.getSelectedItem()).toString() : null;
    }
}