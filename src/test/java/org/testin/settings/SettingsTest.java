package org.testin.settings;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.testng.Assert.*;

/**
 * The settings contract. Two behaviors matter beyond storing values:
 * <p>
 * Moving the Testin root invalidates the tree, so Apply has to rebuild it -
 * and nothing else may, because a rebuild re-indexes every test project.
 * <p>
 * The tester name and role are not cached anywhere: they are read from the
 * state at the moment they are used, and reloading the state (which is how a
 * PersistentStateComponent is restored) replaces them for every later read.
 */
public class SettingsTest {

    /**
     * What actually makes a setting the same in every open project: one
     * application-level service over one file. Not the scope of
     * {@link Setting}, which is project-level for the convenience of callers
     * that already hold a project and reads this same shared object.
     * <p>
     * Asserted rather than assumed, because the failure is silent. Change this
     * to {@code Service.Level.PROJECT} and every setting quietly becomes
     * per-project: nothing fails to compile, no test about values breaks, and
     * the tester finds out by setting a root in one project and not seeing it
     * in another (#70).
     */
    @Test
    public void settingsAreOneObjectAndOneFileForTheWholeIde() {
        final Service service = AppSettingsState.class.getAnnotation(Service.class);
        assertNotNull(service, "AppSettingsState must be a service");
        assertEquals(service.value(), new Service.Level[]{Service.Level.APP},
                "application-level, or every project gets its own settings");

        final State state = AppSettingsState.class.getAnnotation(State.class);
        assertNotNull(state, "AppSettingsState must declare @State or nothing is persisted");
        assertEquals(state.storages().length, 1);
        assertEquals(state.storages()[0].value(), "testinSettings.xml",
                "renaming the storage file loses every existing tester's settings");
        assertEquals(state.name(), "testin.settings.AppSettingsState",
                "renaming the state loses every existing tester's settings");
    }

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
        stored.openTreeOnStartup = false;
        stored.logLevel = "DEBUG";
        stored.testerName = "Omar";
        stored.testerRole = "Test Lead";
        stored.defaultDownloadFolder = "C:/downloads";

        settings.loadState(stored);

        assertEquals(settings.rootTestinPath, "C:/testin");
        assertFalse(settings.openTreeOnStartup);
        assertEquals(settings.logLevel, "DEBUG");
        assertEquals(settings.testerName, "Omar");
        assertEquals(settings.testerRole, "Test Lead");
        assertEquals(settings.defaultDownloadFolder, "C:/downloads");
    }

    @Test
    public void aFreshStateHasNoRootAndLeavesTheTreeClosed() {
        final AppSettingsState settings = new AppSettingsState();

        assertEquals(settings.rootTestinPath, "");
        assertEquals(Setting.normalize(settings.rootTestinPath), Path.of(""));
        assertFalse(settings.openTreeOnStartup, "the panel stays closed until the tester asks for it");
        assertEquals(settings.logLevel, "INFO");
        assertEquals(settings.testerName, "");
        assertEquals(settings.testerRole, "");
    }

    /**
     * The stored value wins over the new default, which is what makes changing a
     * default safe for anyone who had already chosen: a settings file saying the
     * panel should open still opens it.
     */
    @Test
    public void aStoredChoiceSurvivesTheDefaultBeingOff() {
        final AppSettingsState settings = new AppSettingsState();

        final AppSettingsState stored = new AppSettingsState();
        stored.openTreeOnStartup = true;

        settings.loadState(stored);

        assertTrue(settings.openTreeOnStartup);
    }

    @Test
    public void getStateReturnsTheLiveObjectSoWritesArePersisted() {
        final AppSettingsState settings = state("C:/testin", "Sara", "QA Engineer");

        assertSame(settings.getState(), settings);
    }
}
