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

package org.testin.services;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.FailureText;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BackgroundWork {
    private static final @NotNull Runnable NOTHING = () -> {
    };

    public static void run(final @NotNull Project p, final @NotNull String title, final @NotNull String whatFailed, final @NotNull Consumer<@NotNull ProgressIndicator> work) {
        // Rule-SHARE-037
        run(p, title, whatFailed, true, work, NOTHING, NOTHING);
    }

    public static <T> void run(final @NotNull Project p, final @NotNull String title, final @NotNull String whatFailed, final boolean cancellable, final @NotNull Function<@NotNull ProgressIndicator, @NotNull T> work, final @NotNull Consumer<@NotNull T> onSuccess, final @NotNull Runnable onFinished) {
        final @NotNull AtomicReference<Optional<T>> answer = new AtomicReference<>(Optional.empty());
        run(p, title, whatFailed, cancellable, indicator -> answer.set(Optional.of(work.apply(indicator))), () -> answer.get().ifPresent(onSuccess), onFinished);
    }

    private static void run(final @NotNull Project p, final @NotNull String title, final @NotNull String whatFailed, final boolean cancellable, final @NotNull Consumer<@NotNull ProgressIndicator> work, final @NotNull Runnable onSuccess, final @NotNull Runnable onFinished) {
        ProgressManager.getInstance().run(new Task.Backgroundable(p, title, cancellable) {
            private volatile boolean failed;

            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    work.accept(indicator);
                } catch (final ProcessCanceledException stopped) {
                    throw stopped;
                } catch (final Exception ex) {
                    failed = true;

                    final @NotNull String reason = FailureText.of(ex);
                    Logger.error(whatFailed + ": " + reason);
                    Services.getInstance(p, Notifier.class).error(p, whatFailed, reason);
                }
            }

            @Override
            public void onSuccess() {
                if (!failed) onSuccess.run();
            }

            @Override
            public void onFinished() {
                onFinished.run();
            }
        });
    }
}
