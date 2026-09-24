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

package org.testin.indexer;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.TempTree;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.setting.StartupActivity;

import java.nio.file.Path;

public class ReadAfterTheFolderIsSetIdeTest extends BasePlatformTestCase {

    private Path root;
    private String wasSet;

    private static AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = SyntheticTree.tempRoot();
        wasSet = settings().rootTestinPath;
        settings().rootTestinPath = "";
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            settings().rootTestinPath = wasSet;
            TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    public void testTheFolderSetAfterStartupIsReadWhenTheTreeOpens() {
        StartupActivity.execute(getProject());

        assertTrue("with no folder set there is nothing to read", indexer().testProjects().isEmpty());

        final Path project = SyntheticTree.write(root, 1, 1);
        settings().rootTestinPath = root.toString();

        StartupActivity.execute(getProject());

        assertTrue("the folder was set after the startup pass, and opening the tree read nothing: it asked"
                        + " for the one-time wiring and was turned away with it",
                indexer().nodeExists(project));
    }
}
