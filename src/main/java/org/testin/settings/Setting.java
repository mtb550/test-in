package org.testin.settings;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

@Service(Service.Level.PROJECT)
public final class Setting {

    @NotNull
    public Path getTestinPath() {
        String path = AppSettingsState.getInstance().rootTestinPath;
        if (path == null || path.trim().isEmpty()) {
            return Path.of("");
        }
        return Path.of(path.trim());
    }

    public void setTestinPath(final @Nullable Path path) {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.rootTestinPath = path != null ? path.toString() : "";
    }

    public void setAutomationPath(final @Nullable Path path) {
        AppSettingsState settings = AppSettingsState.getInstance();
        settings.rootAutomationPath = path != null ? path.toString() : "";
    }

}