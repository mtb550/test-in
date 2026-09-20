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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Key;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.testin.clipboard.CutState;
import org.testin.indexer.DeletedNodes;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.runner.TestCaseExecutionTracker;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.Once;

import java.nio.file.Path;

public final class StartupActivity implements ProjectActivity {
    private static final @NotNull Key<Boolean> STARTED = Key.create("testin.started");

    // UC-SETTING-002, Rule-SETTING-014
    public static void execute(final @NotNull Project p) {
        if (!Once.claim(p, STARTED)) return;

        final @NotNull AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

        final @NotNull Path testinPath = TestinRoot.normalize(settings.rootTestinPath);
        final boolean rootConfigured = TestinRoot.isConfigured(testinPath);

        if (!rootConfigured) {
            Logger.info("No Testin folder is set yet, so nothing is read until one is");
        }

        Logger.info("StartupActivity.execute()");

        Services.getInstance(DeletedNodes.class).sweep();

        Logger.info("testin Path: " + testinPath);

        final @NotNull BoundTestProject bound = Services.getInstance(p, BoundTestProject.class);

        if (bound.isNamed()) {
            Logger.info("Test project for this repository: '" + bound.name() + "'");
        } else {
            Logger.warn("No test project chosen for " + p.getName());
        }

        if (TestinRoot.isConfigured(testinPath)) {
            // UC-INTERNAL-008, Rule-INTERNAL-091
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            indexer.convertEveryProject();
            indexer.indexWithProgress();
        }

        TestCaseExecutionTracker.initGlobalListener(p);

        CutState.initClipboardWatch(p);

        TestCaseExecutionSubscriber.initRecording(p);
    }

    // UC-SETTING-002, Rule-SETTING-014
    private static void warnIfUnconfigured(final @NotNull Project p) {
        final @NotNull AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
        if (TestinRoot.isConfigured(TestinRoot.normalize(settings.rootTestinPath))) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (p.isDisposed()) return;

            Services.getInstance(p, Notifier.class).warnWithAction(p,
                    Bundle.message("startup.setup.title"),
                    Bundle.message("startup.setup.message"),
                    Bundle.message("startup.setup.action"),
                    () -> ShowSettingsUtil.getInstance().showSettingsDialog(p, SettingsConfigurable.class)
            );
        });
    }

    @Override
    public @NotNull Object execute(final @NotNull Project p, final @NotNull Continuation<? super kotlin.Unit> continuation) {
        execute(p);
        warnIfUnconfigured(p);
        return kotlin.Unit.INSTANCE;
    }
}