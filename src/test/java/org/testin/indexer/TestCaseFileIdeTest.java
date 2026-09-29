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

package org.testin.indexer;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.TempTree;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class TestCaseFileIdeTest extends BasePlatformTestCase {

    private Path root;

    private static @NotNull TestCaseDto testCase(final TestSetDirectoryDto ts) {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
        tc.setParent(ts);
        return tc;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-case-file");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private @NotNull TestCases testCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestProjectDirectoryDto testProject(final String name) {
        return WriteAction.computeAndWait(() -> {
            final TestProjectDirectoryDto tp = Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(root.resolve(name));
            nodes().addTestProject(tp);
            return tp;
        });
    }

    private @NotNull TestSetDirectoryDto testSet(final TestProjectDirectoryDto tp) {
        return WriteAction.computeAndWait(() -> {
            final TestSetDirectoryDto ts = Services.getInstance(getProject(), DirectoryMapper.class)
                    .getTestSetNode(tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            nodes().addTestSet(ts);
            return ts;
        });
    }

    public void testATestCaseIsFoundInsideTheTestProjectThatHoldsIt() {
        final TestProjectDirectoryDto tp = testProject("NAFATH");
        testProject("NAFATH2");
        final TestCaseDto tc = testCase(testSet(tp));

        final TestCaseFile file = testCases().testCaseFile(tc).orElseThrow();

        assertEquals("the case was placed in the wrong test project", tp.getPath(), file.testProject());
        assertEquals("the case's file is not where the store writes it",
                Path.of(tp.getTestCasesDirectory().getPath().getFileName().toString(), "Login", tc.getId() + ".tc"), file.inProject());
    }

    public void testATestCaseNoIndexedTestProjectHoldsHasNoFile() {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).build();

        assertTrue("a case with no test set was given a file", testCases().testCaseFile(tc).isEmpty());
    }
}
