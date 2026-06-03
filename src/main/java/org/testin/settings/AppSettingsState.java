package org.testin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.logger.Log;

@State(
        name = "testin.settings.AppSettingsState",
        storages = @Storage("testinSettings.xml")
)
public class AppSettingsState implements PersistentStateComponent<AppSettingsState> {
    public String rootTestinPath = "";
    public String rootAutomationPath = "";
    public boolean readMode = false;

    public static AppSettingsState getInstance() {
        Log.info("AppSettingsState.getInstance()");
        return ApplicationManager.getApplication().getService(AppSettingsState.class);
    }

    @Nullable
    @Override
    public AppSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AppSettingsState state) {
        Log.info("AppSettingsState.loadState()");
        XmlSerializerUtil.copyBean(state, this);
    }
}