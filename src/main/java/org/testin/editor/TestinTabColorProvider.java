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

package org.testin.editor;

import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Testin editors are backed by {@link UnifiedVirtualFile}, a light file on a
 * non-physical file system. The platform has no VCS status for such a file and
 * paints its tab title in the "unknown file" color, which is why a test set
 * tab looked different from the .java tab beside it.
 * <p>
 * The title is rendered in the ordinary label foreground instead. The color is
 * resolved lazily from the theme rather than hardcoded, so it stays readable in
 * light themes as well as dark ones.
 */
public final class TestinTabColorProvider implements EditorTabColorProvider {

    private static final @NotNull ColorKey TAB_FOREGROUND =
            ColorKey.createColorKey("TESTIN_TAB_FOREGROUND", JBColor.lazy(UIUtil::getLabelForeground));

    @Override
    public @Nullable Color getEditorTabColor(final @NotNull Project p, final @NotNull VirtualFile file) {
        // Background is left to the platform and to the user's File Colors.
        return null;
    }

    /**
     * The tab title color for a Testin editor; every other file is left to the
     * platform.
     */
    // The method is @ApiStatus.Experimental on EditorTabColorProvider, and
    // overriding it is the only way to color a tab title - the stable half of
    // the interface colors the background. Accepted deliberately: if the
    // platform drops it, the @Override stops compiling rather than silently
    // never being called.
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public @Nullable ColorKey getEditorTabForegroundColor(final @NotNull Project p, final @NotNull VirtualFile file) {
        return file instanceof UnifiedVirtualFile ? TAB_FOREGROUND : null;
    }
}
