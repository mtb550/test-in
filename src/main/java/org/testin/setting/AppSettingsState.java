package org.testin.setting;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Level;

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
     * The account this machine connects to a test project's server with (#94).
     * <p>
     * A machine setting and not a repository one: testin.yml is committed, so an
     * account written there would be everybody's. Empty means the tester has not
     * said, and the sync asks.
     */
    public @NotNull String sftpUser = "";

    /**
     * The private key file to offer that server, when no SSH agent is holding
     * one. Empty means the agent, or a password, does the proving.
     */
    public @NotNull String sftpKeyFile = "";

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

    private static @NotNull String orEmpty(final @Nullable String value) {
        return Objects.requireNonNullElse(value, "");
    }

    private static @NotNull String orDefault(final @Nullable String value, final @NotNull String fallback) {
        final @NotNull String stored = orEmpty(value);
        return stored.isBlank() ? fallback : stored;
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
        sftpUser = orEmpty(sftpUser);
        sftpKeyFile = orEmpty(sftpKeyFile);
    }
}
