package org.testin.setting;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Level;

/**
 * The persisted shape, and only that: the fields that are literally in
 * testinSettings.xml, all String or boolean.
 * <p>
 * Logic over a field belongs on the type that owns it — {@link TestinRoot} turns
 * rootTestinPath into a normalized Path and answers whether the root moved. It is
 * kept out of here for a reason beyond tidiness: the XML serializer discovers
 * public getter/setter pairs as persisted properties, not just public fields, so
 * a getPath/setPath pair added here would declare a "path" property of a type it
 * cannot write. Today nothing pairs up — getState has no matching setter — and
 * that is worth keeping true.
 */
@State(name = "testin.settings.AppSettingsState", storages = @Storage("testinSettings.xml"))
@Service(Service.Level.APP)
public final class AppSettingsState implements PersistentStateComponent<AppSettingsState> {

    // Not configured is the empty string, never null - loadState guarantees it.
    // Readers write these straight into DTO fields and enum lookups that require
    // a value, so a null here would surface as a failure far from its cause.
    public @NotNull String rootTestinPath = "";
    public @NotNull String logLevel = "INFO";
    public @NotNull String defaultDownloadFolder = "";
    public @NotNull String testerName = "";
    public @NotNull String testerRole = "";

    private static @NotNull String orEmpty(final @Nullable String value) {
        return value != null ? value : "";
    }

    private static @NotNull String orDefault(final @Nullable String value, final @NotNull String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

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
}
