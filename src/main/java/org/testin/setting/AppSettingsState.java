/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.setting;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Level;
import org.testin.logger.Logger;

import java.util.Objects;

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

    /**
     * Whether Testin's dialogs draw the strip of keyboard shortcuts along the
     * bottom (#13).
     * <p>
     * On until a tester says otherwise, because it is what teaches the keys. A
     * tester who has learned them is reading a row that tells them nothing and
     * costs a line of every dialog, so this turns it off everywhere at once
     * rather than dialog by dialog - the knowledge is theirs, not the dialog's.
     * <p>
     * A machine setting, because that is what it is about: this person, on this
     * screen, knows these keys. Nothing about it belongs to a project or to a
     * repository, so it is not in testin.yml.
     * <p>
     * No normalizing in loadState: a primitive cannot arrive null, and a settings
     * file written before this field existed simply leaves it at the default,
     * because the serializer only writes a value that differs from one.
     */
    public boolean showShortcutHints = true;

    /**
     * A stored string as every reader should see it: never null, and never
     * padded.
     * <p>
     * Trimmed as well as defaulted, because the settings page writes every one
     * of these trimmed and compares the typed value trimmed against what is
     * stored. A file written before it did that - or edited by hand - therefore
     * held a value the page could never match, so Apply stayed black on a page
     * nobody had changed, for the life of the dialog (#312, N8).
     */
    private static @NotNull String orEmpty(final @Nullable String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    @Override
    public @NotNull AppSettingsState getState() {
        return this;
    }

    // UC-SETTING-007, Rule-SETTING-025
    @Override
    public void loadState(final @NotNull AppSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);

        // copyBean writes whatever the file held, so a settings file from an older
        // build - or edited by hand - can put a null over a default. Normalized
        // here, in the one place it can happen, rather than by every reader:
        // Level.valueOf(logLevel) and every marker write would fail on a null.
        rootTestinPath = orEmpty(rootTestinPath);
        logLevel = Level.known(orEmpty(logLevel));
        defaultDownloadFolder = orEmpty(defaultDownloadFolder);
        testerName = orEmpty(testerName);
        testerRole = orEmpty(testerRole);

        applyLogLevel();
    }

    /**
     * UC-SETTING-007, Rule-SETTING-024.
     * <p>
     * The stored level, applied the moment the settings are read.
     * <p>
     * The writer starts at DISABLED and only two things moved it off: opening a
     * project, and pressing Apply. So {@code testin.log} was empty for
     * everything before the first project opened - the indexer starting, a root
     * that could not be read, a marker that would not parse - which is the part
     * a tester turns TRACE on to see. The settings are read before any of it,
     * and this is where they are read (#312, A94).
     * <p>
     * Also from {@link #initializeComponent}, because a machine with no settings
     * file has no state to load and still has a level: the one in the field.
     */
    private void applyLogLevel() {
        Logger.setLogLevel(Level.valueOf(logLevel));
    }

    /**
     * Called by the platform whether or not there was a file to load, which is
     * the whole reason it is used: a first run has no state and still has
     * defaults worth applying.
     */
    @Override
    public void initializeComponent() {
        applyLogLevel();
    }
}
