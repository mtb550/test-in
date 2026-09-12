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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;

/**
 * Generates the Java for a node and everything beneath it.
 * <p>
 * The create hooks answer for one node: a test set becomes an empty class, a
 * package becomes a folder. That is enough when the tester makes nodes one at a
 * time, which until now was the only way they arrived. A copied test set arrives
 * whole - a class, and a method for every case in it - and a copied package
 * arrives with every set beneath it (#51).
 * <p>
 * Each node is asked what it generates, so a kind that generates nothing says so
 * and the walk carries on into its children.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SubtreeCode {

    /**
     * UC-CODEGEN-004, Rule-CODEGEN-022, Rule-CODEGEN-024.
     * <p>
     * Generates for this node, its test cases, and every node under it, as one
     * command.
     * <p>
     * One command for the whole subtree rather than one per file written: each
     * generator opens a command of its own, and a command inside a command is
     * the outer one, so the walk takes the write lock once, reparses each class
     * once and leaves the tester a single undo for the copy they made. A copied
     * set of fifty cases used to be fifty commands, each with its own freeze
     * (#51).
     */
    public static void generate(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        WriteCommandAction.runWriteCommandAction(p, Bundle.message("codegen.generate.command"), null, () -> walk(p, dir));
    }

    // UC-CODEGEN-004, Rule-CODEGEN-023
    private static void walk(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        Logger.info("Generating code for " + dir.getName());
        JavaCode.of(dir.getType()).getCreated().execute(p, dir);

        // A set's own cases, before its children: the class has to exist before a
        // method can be put in it, and the create hook above is what made it.
        for (final TestCaseDto tc : indexer.getTestCasesForTestSet(dir.getPath())) {
            GenType.CREATE_TEST_CASE.getAction().execute(p, tc);
        }

        for (final DirectoryDto child : indexer.getChildren(dir.getPath())) {
            walk(p, child);
        }
    }
}
