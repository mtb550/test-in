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
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

/**
 * Rule-INTERNAL-034.
 * <p>
 * Where a test case's file sits inside its test project, which a bug report
 * links to (#28).
 * <p>
 * An IDE test because the answer comes from the indexed test projects, which
 * are the project's services.
 */
public class TestCaseFileIdeTest extends BasePlatformTestCase {

    private Path root;

    private static void deleteTree(final Path path) {
        if (path == null) return;

        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(each -> {
                try {
                    Files.deleteIfExists(each);
                } catch (final Exception ignored) {
                    // Left for the operating system.
                }
            });
        } catch (final Exception ignored) {
            // Nothing to walk, or nothing to remove.
        }
    }

    private static TestCaseDto testCase(final TestSetDirectoryDto ts) {
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
            deleteTree(root);
        } finally {
            super.tearDown();
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private TestProjectDirectoryDto testProject(final String name) {
        return WriteAction.computeAndWait(() -> {
            final TestProjectDirectoryDto tp = Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(getProject(), root.resolve(name));
            indexer().addTestProject(tp);
            return tp;
        });
    }

    private TestSetDirectoryDto testSet(final TestProjectDirectoryDto tp) {
        return WriteAction.computeAndWait(() -> {
            final TestSetDirectoryDto ts = Services.getInstance(getProject(), DirectoryMapper.class)
                    .getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            indexer().addTestSet(ts);
            return ts;
        });
    }

    public void testACaseIsFoundInsideTheTestProjectThatHoldsIt() {
        final TestProjectDirectoryDto tp = testProject("NAFATH");
        testProject("NAFATH2");
        final TestCaseDto tc = testCase(testSet(tp));

        final TestCaseFile file = indexer().testCaseFile(tc).orElseThrow();

        assertEquals("the case was placed in the wrong test project", tp.getPath(), file.testProject());
        assertEquals("the case's file is not where the store writes it",
                Path.of(tp.getTestCasesDirectory().getPath().getFileName().toString(), "Login", tc.getId() + ".tc"), file.inProject());
    }

    public void testACaseNoIndexedTestProjectHoldsHasNoFile() {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).build();

        assertTrue("a case with no test set was given a file", indexer().testCaseFile(tc).isEmpty());
    }
}
