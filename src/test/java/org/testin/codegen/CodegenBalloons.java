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

package org.testin.codegen;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class CodegenBalloons {
    private static final @NotNull String NOWHERE = "Nowhere to show this, so it is only here: ";

    private final @NotNull Path log = Path.of(PathManager.getLogPath(), "testin.log");

    private long from;

    private CodegenBalloons() {
        Logger.setLogLevel(Level.INFO);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        from = 0;
        logged();
        from = sizeOf(log);
    }

    public static @NotNull CodegenBalloons watching() {
        return new CodegenBalloons();
    }

    private static long sizeOf(final @NotNull Path file) {
        try {
            return Files.exists(file) ? Files.size(file) : 0;
        } catch (final IOException ex) {
            throw new AssertionError("Could not measure " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String plain(final @NotNull String html) {
        return StringUtil.unescapeXmlEntities(html.replace("<br>", "\n").replaceAll("<[^>]+>", " ")).replaceAll("[ \\t]+", " ").strip();
    }

    private @NotNull String written() {
        if (!Files.exists(log)) return "";

        try (RandomAccessFile file = new RandomAccessFile(log.toFile(), "r")) {
            final long start = Math.min(from, file.length());
            final byte @NotNull [] bytes = new byte[(int) (file.length() - start)];
            file.seek(start);
            file.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + log + ": " + ex.getMessage(), ex);
        }
    }

    public @NotNull List<String> logged() {
        final @NotNull String sentinel = "codegen balloons read up to " + UUID.randomUUID();
        Logger.info(sentinel);
        Await.until("the Testin log never reached the line written to read it up to", () -> written().contains(sentinel));

        return written().lines().takeWhile(line -> !line.contains(sentinel)).toList();
    }

    public @NotNull List<String> shown() {
        return logged().stream()
                .filter(line -> line.contains(NOWHERE))
                .map(line -> plain(line.substring(line.indexOf(NOWHERE) + NOWHERE.length())))
                .toList();
    }

    public void stop() {
        Logger.setLogLevel(Level.valueOf(Services.getInstance(AppSettingsState.class).logLevel));
    }
}
