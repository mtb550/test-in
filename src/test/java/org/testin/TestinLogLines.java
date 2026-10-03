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

package org.testin;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class TestinLogLines {
    private static final @NotNull Path TESTIN_LOG = Path.of(PathManager.getLogPath(), "testin.log");

    private final long from;
    private final @NotNull String start = "Lines a test reads start here: " + UUID.randomUUID();

    private TestinLogLines() {
        Logger.setLogLevel(Level.INFO);
        from = size();
        Logger.info(start);
    }

    public static @NotNull TestinLogLines fromNow() {
        return new TestinLogLines();
    }

    private static long size() {
        try {
            return Files.exists(TESTIN_LOG) ? Files.size(TESTIN_LOG) : 0;
        } catch (final IOException ex) {
            throw new AssertionError("could not read the size of " + TESTIN_LOG, ex);
        }
    }

    public @NotNull String written() {
        try {
            final byte @NotNull [] all = Files.exists(TESTIN_LOG) ? Files.readAllBytes(TESTIN_LOG) : new byte[0];
            final int skipped = (int) Math.min(from, all.length);
            final @NotNull String since = new String(all, skipped, all.length - skipped, StandardCharsets.UTF_8);
            final int marked = since.indexOf(start);
            return marked < 0 ? "" : since.substring(marked + start.length());
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + TESTIN_LOG, ex);
        }
    }

    public void stop() {
        Logger.setLogLevel(Level.valueOf(Services.getInstance(AppSettingsState.class).logLevel));
    }
}
