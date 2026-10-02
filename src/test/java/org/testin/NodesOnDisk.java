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

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.UUID;

public record NodesOnDisk(@NotNull Project p) {

    private @NotNull DirectoryMapper mapper() {
        return Services.getInstance(p, DirectoryMapper.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(p, Nodes.class);
    }

    public @NotNull TestProjectDirectoryDto testProject(final @NotNull Path path) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectDirectoryDto tp = mapper().setTestProjectNode(path);
            nodes().addTestProject(tp);
            return tp;
        });
    }

    public @NotNull TestSetPackageDirectoryDto testSetPackage(final @NotNull DirectoryDto parent, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetPackageDirectoryDto tsp = mapper().getTestSetPackageNode(parent.getPath().resolve(name), parent);
            nodes().addTestSetPackage(tsp);
            return tsp;
        });
    }

    public @NotNull TestSetDirectoryDto testSet(final @NotNull DirectoryDto parent, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetDirectoryDto ts = mapper().getTestSetNode(parent.getPath().resolve(name), parent);
            nodes().addTestSet(ts);
            return ts;
        });
    }

    public @NotNull TestRunDirectoryDto testRun(final @NotNull DirectoryDto parent, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestRunDirectoryDto tr = mapper().setTestRunNode(parent.getPath().resolve(name), parent);
            nodes().addTestRunDir(tr);
            return tr;
        });
    }

    public @NotNull TestCaseDto testCase(final @NotNull TestSetDirectoryDto ts) {
        final @NotNull TestCaseDto tc = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Log in with a valid user")
                .order("m")
                .build();

        if (!Services.getInstance(p, TestCases.class).putTestCaseVerbatim(ts.getPath(), tc))
            throw new AssertionError("the test case was not written into " + ts.getName());
        return tc;
    }
}
