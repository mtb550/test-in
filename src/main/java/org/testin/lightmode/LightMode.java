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

package org.testin.lightmode;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.dto.dirs.TestRunDirectoryDto;

import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class LightMode implements Disposable {
    private @NotNull Optional<LightModeWindow> window = Optional.empty();

    // UC-EDITOR-PANEL-046
    public void toggle(final @NotNull RunEditor editor, final @NotNull Runnable onChange) {
        final boolean wasShowingThisRun = isOpenOn(editor.getParent());

        window.ifPresent(LightModeWindow::close);

        if (!wasShowingThisRun) {
            window = Optional.of(new LightModeWindow(editor, () -> {
                window = Optional.empty();
                onChange.run();
            }));
        }

        onChange.run();
    }

    // UC-EDITOR-PANEL-046
    public void refresh(final @NotNull TestRunDirectoryDto run) {
        if (!run.isStillOpen()) {
            closeIfShowing(run);
            return;
        }

        window.filter(open -> open.shows(run)).ifPresent(LightModeWindow::refresh);
    }

    private void closeIfShowing(final @NotNull TestRunDirectoryDto run) {
        window.filter(open -> open.shows(run)).ifPresent(LightModeWindow::close);
    }

    // UC-EDITOR-PANEL-046
    public void editorClosing(final @NotNull TestRunDirectoryDto run) {
        window.filter(open -> open.shows(run)).ifPresent(open -> {
            open.closeQuietly();
            window = Optional.empty();
        });
    }

    // UC-EDITOR-PANEL-046
    public void tick(final @NotNull TestRunDirectoryDto run) {
        window.filter(open -> open.shows(run)).ifPresent(LightModeWindow::tick);
    }

    public boolean isOpenOn(final @NotNull TestRunDirectoryDto run) {
        return window.filter(open -> open.shows(run)).isPresent();
    }

    @Override
    public void dispose() {
        window.ifPresent(LightModeWindow::closeQuietly);
    }
}
