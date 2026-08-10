package org.testin.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;

import java.nio.file.Path;

@Service(Service.Level.PROJECT)
public final class Setting {

    private final @NotNull Project p;

    public Setting(final @NotNull Project p) {
        this.p = p;
    }

    @NotNull
    public Path getTestinPath() {
        String path = Services.getInstance(p, AppSettingsState.class).rootTestinPath;
        if (path == null || path.trim().isEmpty()) {
            return Path.of("");
        }
        return Path.of(path.trim());
    }

    public void setTestinPath(final @Nullable Path path) {
        AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
        settings.rootTestinPath = path != null ? path.toString() : "";
    }

}