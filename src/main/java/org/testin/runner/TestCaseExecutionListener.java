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

package org.testin.runner;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Failure;
import org.testin.model.RunStatus;

import java.time.Duration;

public interface TestCaseExecutionListener {
    Topic<TestCaseExecutionListener> TOPIC = Topic.create("RunTestCaseNotification", TestCaseExecutionListener.class);

    static void broadcast(final @NotNull Project p, final @NotNull String testName, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        if (p.isDisposed()) return;

        p.getMessageBus().syncPublisher(TOPIC).onStatusChanged(testName, status, duration, failure);
    }

    void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure);
}
