package org.testin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "testin.settings.AppSettingsState", storages = @Storage("testinSettings.xml"))
@Service(Service.Level.APP)
public final class AppSettingsState implements PersistentStateComponent<AppSettingsState> {

    public String rootTestinPath = "";
    public boolean openTreeOnStartup = true;
    public String logLevel = "INFO";
    public String defaultDownloadFolder = "";
    public String testerName = "";
    public String testerRole = "";

    @Override
    public @NotNull AppSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(final @NotNull AppSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
