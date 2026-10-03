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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.setting.AppSettingsState;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Balloons {
    private static final @NotNull String SHOWN = "Nowhere to show this, so it is only here: ";
    private static final @NotNull Path LOG = Path.of(PathManager.getLogPath(), "testin.log");

    private long from;

    private Balloons() {
        from = 0;
    }

    public static @NotNull Balloons heard(final @NotNull Disposable owner) {
        Logger.setLogLevel(Level.INFO);
        Disposer.register(owner, () -> Logger.setLogLevel(Level.valueOf(ApplicationManager.getApplication().getService(AppSettingsState.class).logLevel)));

        final @NotNull Balloons balloons = new Balloons();
        balloons.shown();
        return balloons;
    }

    public @NotNull List<String> shown() {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        final @NotNull String mark = "balloons-" + UUID.randomUUID();
        Logger.info(mark);
        Await.until("the Testin log never caught up with " + mark, () -> since().contains(mark));

        final @NotNull String written = since();
        final int end = written.indexOf(mark);
        from += written.substring(0, end + mark.length()).getBytes(StandardCharsets.UTF_8).length;

        final @NotNull List<String> said = new ArrayList<>();
        for (final String line : written.substring(0, end).split("\\R")) {
            final int at = line.indexOf(SHOWN);
            if (at < 0) continue;

            final @NotNull String html = line.substring(at + SHOWN.length());
            said.add(StringUtil.unescapeXmlEntities(html.replace("<html>", "").replace("</html>", "").replace("<br>", "\n").replace("<b>", "").replace("</b>", "")));
        }
        return said;
    }

    private @NotNull String since() {
        try {
            if (!Files.exists(LOG)) return "";
            if (Files.size(LOG) < from) from = 0;

            try (final RandomAccessFile file = new RandomAccessFile(LOG.toFile(), "r")) {
                file.seek(from);
                final byte @NotNull [] bytes = new byte[(int) (file.length() - from)];
                file.readFully(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (final IOException ex) {
            throw new AssertionError("the Testin log could not be read", ex);
        }
    }
}
