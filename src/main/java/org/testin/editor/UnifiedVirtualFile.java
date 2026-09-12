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

import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;

@Getter
public class UnifiedVirtualFile extends LightVirtualFile {

    private final @NotNull DirectoryDto dir;

    public UnifiedVirtualFile(final @NotNull DirectoryDto dir, final @NotNull EditorType ft) {
        super(dir.getName());
        this.dir = dir;
        this.setFileType(ft);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public @NotNull String getUrl() {
        return TestinFileSystem.PROTOCOL + ":///" + dir.getPath().toAbsolutePath().toString().replace("\\", "/");
    }

    @Override
    public @NotNull String getPath() {
        return dir.getPath().toAbsolutePath().toString();
    }

    /**
     * Testin's own file system, and the light file's default when the platform
     * cannot hand it over.
     * <p>
     * Never null, which is not what the lookup promises: {@code getFileSystem}
     * answers null for a protocol the manager has no entry for, and this method
     * is declared not-null and called by anything that meets one of these
     * files. The Database plugin does, while working out an editor tab's title,
     * and threw an IDE error report over a Testin tab - from a coroutine, so
     * the report named Testin and pointed at their code.
     * <p>
     * A compiler cannot see this. The annotation is rewritten into a throw by
     * the IDE's instrumenter, so it exists only in a running IDE, which is why
     * this survived every build.
     */
    @Override
    public @NotNull VirtualFileSystem getFileSystem() {
        final VirtualFileSystem registered = VirtualFileManager.getInstance().getFileSystem(TestinFileSystem.PROTOCOL);

        return registered != null ? registered : super.getFileSystem();
    }

    public @NotNull TestSetDirectoryDto getTestSet() {
        return (TestSetDirectoryDto) dir;
    }

    public @NotNull TestRunDirectoryDto getTestRun() {
        return (TestRunDirectoryDto) dir;
    }
}