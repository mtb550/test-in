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
package org.testin.view;

import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.model.result.TestRunItems;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.model.node.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.util.List;

public abstract class AbstractViewPanelIdeTest extends AbstractTempRootIdeTest {
    protected ViewOnScreen view;
    protected TestProjectDirectoryDto testProject;
    private String rootBefore = "";

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        rootBefore = settings().rootTestinPath;
        settings().rootTestinPath = root.toString();
        testProject = EditorFixtures.testProject(getProject(), root);
        view = ViewOnScreen.closed(getProject(), getTestRootDisposable());
    }

    @Override
    protected void tearDown() {
        settings().rootTestinPath = rootBefore;
        super.tearDown();
    }

    protected @NotNull TestSetDirectoryDto aTestSet(final @NotNull String name) {
        return EditorFixtures.testSet(getProject(), testProject, name);
    }

    protected @NotNull TestCaseDto aTestCase(final @NotNull TestSetDirectoryDto ts, final @NotNull String description, final @NotNull String order) {
        return EditorFixtures.testCase(getProject(), ts, description, order);
    }

    protected @NotNull TestRunDirectoryDto aTestRun(final @NotNull List<TestRunItems> results) {
        return EditorFixtures.testRun(getProject(), testProject, results);
    }

    protected @NotNull List<String> details() {
        return view.words(ViewTab.DETAILS);
    }

    protected void settled() {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }
}
