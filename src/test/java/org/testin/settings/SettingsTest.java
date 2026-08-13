package org.testin.settings;

import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.testng.Assert.*;

/**
 * The settings contract. Two behaviours matter beyond storing values:
 * <p>
 * Moving the Testin root invalidates the tree, so Apply has to rebuild it -
 * and nothing else may, because a rebuild re-indexes every test project.
 * <p>
 * The tester name and role are not cached anywhere: they are read from the
 * state at the moment they are used, and reloading the state (which is how a
 * PersistentStateComponent is restored) replaces them for every later read.
 */
public class SettingsTest {

    private static AppSettingsState state(final String rootPath, final String testerName, final String testerRole) {
        final AppSettingsState settings = new AppSettingsState();
        settings.rootTestinPath = rootPath;
        settings.testerName = testerName;
        settings.testerRole = testerRole;
        return settings;
    }

    // ---------------------------------------------------------------- root path

    @Test
    public void everyEmptyFormOfARootMeansNoRootConfigured() {
        assertEquals(Setting.normalize(null), Path.of(""));
        assertEquals(Setting.normalize(""), Path.of(""));
        assertEquals(Setting.normalize("   "), Path.of(""));
        assertEquals(Setting.normalize("\t\n "), Path.of(""));
    }

    @Test
    public void aStoredRootIsTrimmedBeforeUse() {
        assertEquals(Setting.normalize("  C:/testin  "), Path.of("C:/testin"));
        assertEquals(Setting.normalize("C:/testin"), Path.of("C:/testin"));
    }

    // -------------------------------------------------- changing the testin folder

    @Test
    public void changingTheTestinFolderRequiresTheTreeToReload() {
        assertTrue(Setting.isRootChanged("C:/testin", "C:/other"));
    }

    @Test
    public void configuringARootForTheFirstTimeRequiresTheTreeToReload() {
        assertTrue(Setting.isRootChanged("", "C:/testin"));
        assertTrue(Setting.isRootChanged(null, "C:/testin"));
    }

    @Test
    public void clearingTheRootRequiresTheTreeToReload() {
        assertTrue(Setting.isRootChanged("C:/testin", ""));
    }

    @Test
    public void reApplyingTheSameRootLeavesTheTreeAlone() {
        assertFalse(Setting.isRootChanged("C:/testin", "C:/testin"));
    }

    /**
     * Apply runs on every OK, so a value that only differs by surrounding
     * whitespace must not trigger a full re-index.
     */
    @Test
    public void whitespaceAroundAnUnchangedRootIsNotAChange() {
        assertFalse(Setting.isRootChanged("C:/testin", "  C:/testin  "));
        assertFalse(Setting.isRootChanged("  C:/testin", "C:/testin\t"));
    }

    @Test
    public void theDifferentSpellingsOfNoRootAreNotAChange() {
        assertFalse(Setting.isRootChanged(null, ""));
        assertFalse(Setting.isRootChanged("", "   "));
        assertFalse(Setting.isRootChanged(null, null));
    }

    // ------------------------------------------------- tester name and role

    /**
     * The counterpart of the rule above: renaming the tester must not cost a
     * re-index of every test project.
     */
    @Test
    public void changingTesterNameOrRoleNeverReloadsTheTree() {
        final AppSettingsState before = state("C:/testin", "Sara", "QA Engineer");
        final AppSettingsState after = state("C:/testin", "Omar", "Test Lead");

        assertFalse(Setting.isRootChanged(before.rootTestinPath, after.rootTestinPath));
        assertNotEquals(before.testerName, after.testerName);
        assertNotEquals(before.testerRole, after.testerRole);
    }

    /**
     * How the indexer and the report generators read the tester: from the state,
     * at the moment of use. Nothing copies these into a cache, so a change is
     * visible to the next read without any reload step.
     */
    @Test
    public void theTesterIsReadLiveSoNoCacheCanGoStale() {
        final AppSettingsState settings = state("C:/testin", "Sara", "QA Engineer");

        final Supplier<String> nameAtPointOfUse = () -> settings.testerName;
        final Supplier<String> roleAtPointOfUse = () -> settings.testerRole;

        assertEquals(nameAtPointOfUse.get(), "Sara");
        assertEquals(roleAtPointOfUse.get(), "QA Engineer");

        settings.testerName = "Omar";
        settings.testerRole = "Test Lead";

        assertEquals(nameAtPointOfUse.get(), "Omar");
        assertEquals(roleAtPointOfUse.get(), "Test Lead");
    }

    /**
     * Reloading the persisted state is the one moment the settings object is
     * replaced wholesale - after it, every later read must see the new tester.
     */
    @Test
    public void reloadingTheStateReplacesTheTesterForEveryLaterRead() {
        final AppSettingsState settings = state("C:/testin", "Sara", "QA Engineer");
        final Supplier<String> nameAtPointOfUse = () -> settings.testerName;

        settings.loadState(state("C:/testin", "Omar", "Test Lead"));

        assertEquals(settings.testerName, "Omar");
        assertEquals(settings.testerRole, "Test Lead");
        assertEquals(nameAtPointOfUse.get(), "Omar");
    }

    // ------------------------------------------------------------ state itself

    @Test
    public void reloadingTheStateCarriesEveryField() {
        final AppSettingsState settings = new AppSettingsState();

        final AppSettingsState stored = new AppSettingsState();
        stored.rootTestinPath = "C:/testin";
        stored.readMode = true;
        stored.openTreeOnStartup = false;
        stored.logLevel = "DEBUG";
        stored.testerName = "Omar";
        stored.testerRole = "Test Lead";
        stored.defaultDownloadFolder = "C:/downloads";

        settings.loadState(stored);

        assertEquals(settings.rootTestinPath, "C:/testin");
        assertTrue(settings.readMode);
        assertFalse(settings.openTreeOnStartup);
        assertEquals(settings.logLevel, "DEBUG");
        assertEquals(settings.testerName, "Omar");
        assertEquals(settings.testerRole, "Test Lead");
        assertEquals(settings.defaultDownloadFolder, "C:/downloads");
    }

    @Test
    public void aFreshStateHasNoRootAndOpensTheTree() {
        final AppSettingsState settings = new AppSettingsState();

        assertEquals(settings.rootTestinPath, "");
        assertEquals(Setting.normalize(settings.rootTestinPath), Path.of(""));
        assertTrue(settings.openTreeOnStartup, "the panel opens on startup unless the user turns it off");
        assertEquals(settings.logLevel, "INFO");
        assertEquals(settings.testerName, "");
        assertEquals(settings.testerRole, "");
    }

    @Test
    public void getStateReturnsTheLiveObjectSoWritesArePersisted() {
        final AppSettingsState settings = state("C:/testin", "Sara", "QA Engineer");

        assertSame(settings.getState(), settings);
    }
}
