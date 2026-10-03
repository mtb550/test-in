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

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.logger.Level;
import org.testin.logger.LogWriter;
import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Said {
    private static final @NotNull String SHOWN = "Nowhere to show this, so it is only here: ";

    private static @NotNull Path log() {
        return Path.of(PathManager.getLogPath(), "testin.log");
    }

    private static @NotNull String read() {
        try {
            return Files.exists(log()) ? Files.readString(log(), StandardCharsets.UTF_8) : "";
        } catch (final IOException beingWritten) {
            return "";
        }
    }

    private static @NotNull Level levelNow() {
        final @NotNull LogWriter writer = ApplicationManager.getApplication().getService(LogWriter.class);
        return Arrays.stream(Level.values()).filter(level -> level != Level.DISABLED && writer.writes(level)).findFirst().orElse(Level.DISABLED);
    }

    private static @NotNull String plain(final @NotNull String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip();
    }

    public static @NotNull List<String> during(final @NotNull Project p, final @NotNull Runnable gesture) {
        final @NotNull List<String> said = new CopyOnWriteArrayList<>();
        final @NotNull Disposable listening = Disposer.newDisposable();
        final @NotNull List<String> lines;
        try {
            p.getMessageBus().connect(listening).subscribe(Notifications.TOPIC, new Notifications() {
                @Override
                public void notify(final @NotNull Notification notification) {
                    said.add(plain(notification.getTitle() + " " + notification.getContent()));
                }
            });
            lines = logged(gesture);
        } finally {
            Disposer.dispose(listening);
        }

        final @NotNull List<String> shown = new ArrayList<>();
        for (final String line : lines) {
            if (line.contains(SHOWN)) shown.add(plain(line.substring(line.indexOf(SHOWN) + SHOWN.length())));
        }
        shown.addAll(said);
        return shown;
    }

    public static @NotNull List<String> logged(final @NotNull Runnable gesture) {
        final @NotNull Level before = levelNow();
        final @NotNull String opened = "said-opened-" + UUID.randomUUID();
        final @NotNull String closed = "said-closed-" + UUID.randomUUID();
        try {
            Logger.setLogLevel(Level.INFO);
            Logger.info(opened);

            gesture.run();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            Logger.info(closed);
            Await.until("the log never caught up with the gesture", () -> read().contains(closed));
        } finally {
            Logger.setLogLevel(before);
        }

        final @NotNull String text = read();
        return List.of(text.substring(text.indexOf(opened), text.indexOf(closed)).split("\\R"));
    }
}
