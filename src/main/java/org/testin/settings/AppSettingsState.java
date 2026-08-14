package org.testin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Level;

@State(name = "testin.settings.AppSettingsState", storages = @Storage("testinSettings.xml"))
@Service(Service.Level.APP)
public final class AppSettingsState implements PersistentStateComponent<AppSettingsState> {

    // Not configured is the empty string, never null - loadState guarantees it.
    // Readers write these straight into DTO fields and enum lookups that require
    // a value, so a null here would surface as a failure far from its cause.
    public @NotNull String rootTestinPath = "";
    public boolean openTreeOnStartup = true;
    public @NotNull String logLevel = "INFO";
    public @NotNull String defaultDownloadFolder = "";
    public @NotNull String testerName = "";
    public @NotNull String testerRole = "";

    @Override
    public @NotNull AppSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(final @NotNull AppSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);

        // copyBean writes whatever the file held, so a settings file from an older
        // build - or edited by hand - can put a null over a default. Normalized
        // here, in the one place it can happen, rather than by every reader:
        // Level.valueOf(logLevel) and every marker write would fail on a null.
        rootTestinPath = orEmpty(rootTestinPath);
        logLevel = orDefault(logLevel, Level.INFO.name());
        defaultDownloadFolder = orEmpty(defaultDownloadFolder);
        testerName = orEmpty(testerName);
        testerRole = orEmpty(testerRole);
    }

    private static @NotNull String orEmpty(final @Nullable String value) {
        return value != null ? value : "";
    }

    private static @NotNull String orDefault(final @Nullable String value, final @NotNull String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
