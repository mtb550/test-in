package org.testin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "testin.settings.AppSettingsState", storages = @Storage("testinSettings.xml"))
@Service(Service.Level.APP)
public final class AppSettingsState implements PersistentStateComponent<AppSettingsState> {

    // Nullable despite the defaults: loadState copies a deserialized bean over this
    // one, so a settings file written by an older build - or edited by hand - can
    // put a null back into any of these. Readers treat null as "not configured".
    public @Nullable String rootTestinPath = "";
    public boolean openTreeOnStartup = true;
    public @Nullable String logLevel = "INFO";
    public @Nullable String defaultDownloadFolder = "";
    public @Nullable String testerName = "";
    public @Nullable String testerRole = "";

    @Override
    public @NotNull AppSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(final @NotNull AppSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
