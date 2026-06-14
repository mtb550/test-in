package org.testin.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

@Service(Service.Level.PROJECT)
public final class Setting {

    private final Project project;

    public Setting(Project project) {
        this.project = project;
    }

    @NotNull
    public Path getTestinPath() {
        String path = AppSettingsState.getInstance().rootTestinPath;
        return path != null && !path.trim().isEmpty() ? Path.of(path.trim()) : Path.of("");
    }

    public void setTestinPath(@Nullable final Path path) {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.rootTestinPath = path != null ? path.toString() : "";
    }

    @Nullable
    public Path getAutomationPath() {
        String path = AppSettingsState.getInstance().rootAutomationPath;
        return path != null && !path.trim().isEmpty() ? Path.of(path.trim()) : null;
    }

    public void setAutomationPath(@Nullable final Path path) {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.rootAutomationPath = path != null ? path.toString() : "";
    }

    public boolean isReadMode() {
        return AppSettingsState.getInstance().readMode;
    }

    public void setReadMode(final boolean readMode) {
        AppSettingsState.getInstance().readMode = readMode;
    }

    public String getLogLevel() {
        return AppSettingsState.getInstance().logLevel;
    }

    public void setLogLevel(final String logLevel) {
        AppSettingsState.getInstance().logLevel = logLevel;
    }
}