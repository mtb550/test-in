package com.example.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "com.example.settings.AppSettingsState",
        storages = @Storage("TestCaseManagerSettings.xml")
)
public class AppSettingsState implements PersistentStateComponent<AppSettingsState> {

    public String rootFolderPath;

    public static AppSettingsState getInstance() {
        System.out.println("AppSettingsState.getInstance()");
        return ApplicationManager.getApplication().getService(AppSettingsState.class);
    }

    @Nullable
    @Override
    public AppSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AppSettingsState state) {
        System.out.println("AppSettingsState.loadState()");
        XmlSerializerUtil.copyBean(state, this);
    }
}