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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SettingsTest {

    private static AppSettingsState state(final String testerName, final String testerRole) {
        final AppSettingsState settings = new AppSettingsState();
        settings.rootTestinPath = "C:/testin";
        settings.testerName = testerName;
        settings.testerRole = testerRole;
        return settings;
    }

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

    @Test
    public void everyEmptyFormOfARootMeansNoRootConfigured() {
        assertEquals(TestinRoot.normalize(null), TestinRoot.NONE);
        assertEquals(TestinRoot.normalize(""), TestinRoot.NONE);
        assertEquals(TestinRoot.normalize("   "), TestinRoot.NONE);
        assertEquals(TestinRoot.normalize("\t\n "), TestinRoot.NONE);
    }

    @Test
    public void aStoredRootIsTrimmedBeforeUse() {
        final Path root = Path.of("C:/testin");

        assertEquals(TestinRoot.normalize("  C:/testin  "), root);
        assertEquals(TestinRoot.normalize("C:/testin"), root);
    }

    @Test
    public void changingTheTestinFolderRequiresTheTreeToReload() {
        assertTrue(TestinRoot.isRootChanged("C:/testin", "C:/other"));
    }

    @Test
    public void configuringARootForTheFirstTimeRequiresTheTreeToReload() {
        assertTrue(TestinRoot.isRootChanged("", "C:/testin"));
        assertTrue(TestinRoot.isRootChanged(null, "C:/testin"));
    }

    @Test
    public void clearingTheRootRequiresTheTreeToReload() {
        assertTrue(TestinRoot.isRootChanged("C:/testin", ""));
    }

    @Test
    public void reApplyingTheSameRootLeavesTheTreeAlone() {
        assertFalse(TestinRoot.isRootChanged("C:/testin", "C:/testin"));
    }

    @Test
    public void whitespaceAroundAnUnchangedRootIsNotAChange() {
        assertFalse(TestinRoot.isRootChanged("C:/testin", "  C:/testin  "));
        assertFalse(TestinRoot.isRootChanged("  C:/testin", "C:/testin\t"));
    }

    @Test
    public void theDifferentSpellingsOfNoRootAreNotAChange() {
        assertFalse(TestinRoot.isRootChanged(null, ""));
        assertFalse(TestinRoot.isRootChanged("", "   "));
        assertFalse(TestinRoot.isRootChanged(null, null));
    }

    @Test
    public void changingTesterNameOrRoleNeverReloadsTheTree() {
        final AppSettingsState before = state("Sara", "QA Engineer");
        final AppSettingsState after = state("Omar", "Test Lead");

        assertFalse(TestinRoot.isRootChanged(before.rootTestinPath, after.rootTestinPath));
        assertNotEquals(before.testerName, after.testerName);
        assertNotEquals(before.testerRole, after.testerRole);
    }

    @Test
    public void theTesterIsReadLiveSoNoCacheCanGoStale() {
        final AppSettingsState settings = state("Sara", "QA Engineer");

        final Supplier<String> nameAtPointOfUse = () -> settings.testerName;
        final Supplier<String> roleAtPointOfUse = () -> settings.testerRole;

        assertEquals(nameAtPointOfUse.get(), "Sara");
        assertEquals(roleAtPointOfUse.get(), "QA Engineer");

        settings.testerName = "Omar";
        settings.testerRole = "Test Lead";

        assertEquals(nameAtPointOfUse.get(), "Omar");
        assertEquals(roleAtPointOfUse.get(), "Test Lead");
    }

    @Test
    public void reloadingTheStateReplacesTheTesterForEveryLaterRead() {
        final AppSettingsState settings = state("Sara", "QA Engineer");
        final Supplier<String> nameAtPointOfUse = () -> settings.testerName;

        settings.loadState(state("Omar", "Test Lead"));

        assertEquals(settings.testerName, "Omar");
        assertEquals(settings.testerRole, "Test Lead");
        assertEquals(nameAtPointOfUse.get(), "Omar");
    }

    @Test
    public void reloadingTheStateCarriesEveryField() {
        final AppSettingsState settings = new AppSettingsState();

        final AppSettingsState stored = new AppSettingsState();
        stored.rootTestinPath = "C:/testin";
        stored.logLevel = "DEBUG";
        stored.testerName = "Omar";
        stored.testerRole = "Test Lead";
        stored.defaultDownloadFolder = "C:/downloads";

        settings.loadState(stored);

        assertEquals(settings.rootTestinPath, "C:/testin");
        assertEquals(settings.logLevel, "DEBUG");
        assertEquals(settings.testerName, "Omar");
        assertEquals(settings.testerRole, "Test Lead");
        assertEquals(settings.defaultDownloadFolder, "C:/downloads");
    }

    @Test
    public void aFreshStateHasNoRootAndNothingConfigured() {
        final AppSettingsState settings = new AppSettingsState();

        assertEquals(settings.rootTestinPath, "");
        assertEquals(TestinRoot.normalize(settings.rootTestinPath), TestinRoot.NONE);
        assertEquals(settings.logLevel, "INFO");
        assertEquals(settings.testerName, "");
        assertEquals(settings.testerRole, "");
    }

    @Test
    public void getStateReturnsTheLiveObjectSoWritesArePersisted() {
        final AppSettingsState settings = state("Sara", "QA Engineer");

        assertSame(settings.getState(), settings);
    }
}
