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
package org.testin.search;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.TempTree;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class SearchFindsTestRunsIdeTest extends BasePlatformTestCase {

    private Path root;

    private TestCaseDto testCase;

    private Path ranIt;

    private Path ranSomethingElse;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-search");

        final TestProjectDirectoryDto tp = WriteAction.computeAndWait(() -> {
            final TestProjectDirectoryDto project = mapper().setTestProjectNode(getProject(), root.resolve("NAFATH"));
            indexer().addTestProject(project);

            return project;
        });

        testCase = aTestCaseIn(tp);
        ranIt = aRunOver(tp, "Cycle-1", testCase.getId());
        ranSomethingElse = aRunOver(tp, "Cycle-2", UUID.randomUUID());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private DirectoryMapper mapper() {
        return Services.getInstance(getProject(), DirectoryMapper.class);
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private TestCaseDto aTestCaseIn(final TestProjectDirectoryDto tp) {
        final TestSetDirectoryDto login = WriteAction.computeAndWait(() -> {
            final TestSetDirectoryDto set = mapper().getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            indexer().addTestSet(set);

            return set;
        });

        final TestCaseDto tc = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Log in with a valid user")
                .order("m")
                .build();
        tc.setParent(login);

        indexer().putTestCaseVerbatim(login.getPath(), tc);
        return tc;
    }

    private Path aRunOver(final TestProjectDirectoryDto tp, final String named, final UUID testCaseId) {
        final Path runPath = WriteAction.computeAndWait(() -> {
            final Path path = tp.getTestRunsDirectory().getPath().resolve(named);
            final TestRunDirectoryDto tr = mapper().setTestRunNode(getProject(), path, tp.getTestRunsDirectory());
            indexer().addTestRunDir(tr);

            return path;
        });

        indexer().putTestRun(runPath, new TestRunDto().setResults(List.of(new TestRunItems().setId(testCaseId).setStatus(TestStatus.PASSED))));
        return runPath;
    }

    private List<Hit> forTheId() {
        return Hits.forQuery(getProject(), testCase.getId().toString()).hits();
    }

    // UC-INTERNAL-001, Rule-INTERNAL-098
    public void testSearchingATestCaseIdAlsoFindsTheTestRunThatRanIt() {
        assertTrue("a test case id found no row for the test run holding its verdict",
                forTheId().stream().anyMatch(hit -> hit.node().getPath().equals(ranIt)));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-098
    public void testTheRunRowCarriesTheTestCaseSoItOpensWhereTheVerdictIs() {
        final Hit inTheRun = forTheId().stream()
                .filter(hit -> hit.node().getPath().equals(ranIt))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for the test run that ran the test case"));

        assertEquals("the row does not carry the test case, so choosing it would open the run at no row",
                testCase.getId(), inTheRun.testCase().orElseThrow().getId());
        assertEquals("a run row is named by the test case it holds", testCase.getDescription(), inTheRun.name());
    }

    // UC-INTERNAL-001, Rule-INTERNAL-098
    public void testATestRunThatDoesNotCoverTheTestCaseIsNoRow() {
        assertFalse("a test run with no verdict for the test case was listed",
                forTheId().stream().anyMatch(hit -> hit.node().getPath().equals(ranSomethingElse)));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-001
    public void testTheTestCaseItselfIsStillTheFirstRowForItsId() {
        final List<Hit> hits = forTheId();

        assertFalse("nothing was found for a test case id", hits.isEmpty());
        assertEquals("the test case's own row is no longer first", testCase.getParent().getPath(), hits.getFirst().node().getPath());
    }
}
