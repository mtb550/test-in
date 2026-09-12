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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Where this project's generated Java goes, remembered once it has been found.
 * <p>
 * A project service because the answer is a project's, and it used to be a
 * static field on {@code Config} - one slot for the whole IDE. With two projects
 * open the second one to look overwrote the first, so generation in one could
 * write into the other's source tree, or fail to find a root that was there.
 * Nothing about that surfaced as an error; it read as code generation not
 * working.
 * <p>
 * Scanning the modules is what this saves. {@code JavaSourceRoot.find} walks
 * every module's test source roots, and it is asked on every generated class and
 * method.
 */
@Service(Service.Level.PROJECT)
public final class TestSourceRoot {

    /**
     * Written on the thread that found it and read from wherever generation runs.
     */
    private volatile @NotNull Optional<VirtualFile> cached = Optional.empty();

    /**
     * The root already found, and empty when nothing has looked yet or when what
     * was found has since been deleted.
     * <p>
     * A cached root that stopped being valid is no answer at all, so that is
     * asked here rather than by the caller - which is where it used to live,
     * beside the null check for "nothing has looked yet" (#71).
     */
    public @NotNull Optional<VirtualFile> get() {
        return cached.filter(VirtualFile::isValid);
    }

    public void set(final @NotNull VirtualFile root) {
        cached = Optional.of(root);
    }
}
