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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.method.update.NoOpCodeUpdate;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Once;

import java.util.List;

@Getter
public final class GenType<T> {
    private static final @NotNull Key<Boolean> INDEXING_SAID = Key.create("testin.codegen.indexingSaid");

    public static final @NotNull GenType<DirectoryDto> REMOVE_TEST_PROJECT = new GenType<>(
            Bundle.message("codegen.remove.test.project")
    );

    public static final @NotNull GenType<Renamed> RENAME_TEST_PROJECT = new GenType<>(
            Bundle.message("codegen.rename.test.project")
    );

    public static final @NotNull GenType<DirectoryDto> REMOVE_TEST_SET_PACKAGE = new GenType<>(
            Bundle.message("codegen.remove.test.set.package")
    );

    public static final @NotNull GenType<Renamed> RENAME_TEST_SET_PACKAGE = new GenType<>(
            Bundle.message("codegen.rename.test.set.package")
    );

    public static final @NotNull GenType<Moved> MOVE_TEST_SET_PACKAGE = new GenType<>(
            Bundle.message("codegen.move.test.set.package")
    );

    public static final @NotNull GenType<DirectoryDto> CREATE_TEST_SET = new GenType<>(
            Bundle.message("codegen.create.test.set")
    );

    public static final @NotNull GenType<DirectoryDto> REMOVE_TEST_SET = new GenType<>(
            Bundle.message("codegen.remove.test.set")
    );

    public static final @NotNull GenType<Renamed> RENAME_TEST_SET = new GenType<>(
            Bundle.message("codegen.rename.test.set")
    );

    public static final @NotNull GenType<Moved> MOVE_TEST_SET = new GenType<>(
            Bundle.message("codegen.move.test.set")
    );

    public static final @NotNull GenType<TestCaseDto> CREATE_TEST_CASE = new GenType<>(
            Bundle.message("codegen.create.test.case")
    );

    public static final @NotNull GenType<TestCaseDto> REMOVE_TEST_CASE = new GenType<>(
            Bundle.message("codegen.remove.test.case")
    );

    // UC-CODEGEN-002, Rule-CODEGEN-077
    public static final @NotNull GenType<MovedTestCase> MOVE_TEST_CASE = new GenType<>(
            Bundle.message("codegen.move.test.case")
    );

    // UC-CODEGEN-002, Rule-CODEGEN-078
    public static final @NotNull GenType<CopiedTestCase> COPY_TEST_CASE = new GenType<>(
            Bundle.message("codegen.copy.test.case")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_DESCRIPTION = new GenType<>(
            Bundle.message("codegen.update.test.case")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_EXPECTED_RESULT = new GenType<>(
            Bundle.message("codegen.update.test.case"),
            "expected result"
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_MODULE = new GenType<>(
            Bundle.message("codegen.update.test.case"),
            "module"
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_TEST_DATA = new GenType<>(
            Bundle.message("codegen.update.test.case"),
            "test data"
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_PRE_CONDITIONS = new GenType<>(
            Bundle.message("codegen.update.test.case"),
            "pre-conditions"
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_STEPS = new GenType<>(
            Bundle.message("codegen.update.test.case"),
            "steps"
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_GROUP = new GenType<>(
            Bundle.message("codegen.update.test.case")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_PRIORITY = new GenType<>(
            Bundle.message("codegen.update.test.case"),
            "priority"
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_ORDER = new GenType<>(
            Bundle.message("codegen.update.test.case")
    );

    public static final @NotNull GenType<TestCaseDto> RECONCILE_TEST_CASE = new GenType<>(
            Bundle.message("codegen.restore.test.case")
    );

    public static final @NotNull GenType<TestCaseDto> UPDATE_TEST_CASE_STATUS = new GenType<>(
            Bundle.message("codegen.update.test.case")
    );

    public static final @NotNull GenType<TestCaseDto> NO_CODE_CHANGE = new GenType<>(
            Bundle.message("codegen.no.code.change"),
            "read-only attribute"
    );

    private final @NotNull String description;
    private final @NotNull GenAction<T> action;

    private GenType(final @NotNull String description) {
        this.description = description;
        this.action = new JavaCodeUpdate();
    }

    private GenType(final @NotNull String description, final @NotNull String dataOnlyField) {
        this.description = description;
        this.action = new NoOpCodeUpdate<>(dataOnlyField);
    }

    // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-EDITOR-PANEL-046
    public void executeAll(final @NotNull Project p, final @NotNull List<? extends T> items) {
        ApplicationManager.getApplication().invokeLater(() -> executeAllNow(p, items), p.getDisposed());
    }

    // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-CODEGEN-018
    public void executeAllNow(final @NotNull Project p, final @NotNull List<? extends T> items) {
        action.executeAll(p, items);
    }

    @Override
    public @NotNull String toString() {
        return description;
    }

    private final class JavaCodeUpdate implements GenAction<T> {
        // UC-CODEGEN-019, Rule-CODEGEN-005
        @Override
        public void execute(final @NotNull Project p, final @NotNull T payload) {
            if (cannotGenerate(p)) return;

            CodeGenerators.find(GenType.this).execute(p, payload);
        }

        // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-EDITOR-PANEL-046
        @Override
        public void executeAll(final @NotNull Project p, final @NotNull List<? extends T> items) {
            if (cannotGenerate(p) || items.isEmpty()) return;

            WriteCommandAction.runWriteCommandAction(p, description, null,
                    () -> CodeGenerators.find(GenType.this).executeAll(p, items));
        }

        // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-CODEGEN-006, Rule-CODEGEN-082
        private boolean cannotGenerate(final @NotNull Project p) {
            if (!CodeOn.isOnOrWarnOnce(p)) return true;
            if (!DumbService.isDumb(p)) return false;

            if (Once.claim(p, INDEXING_SAID)) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Refused.WHILE_INDEXING, description);

                DumbService.getInstance(p).runWhenSmart(() -> p.putUserData(INDEXING_SAID, null));
            }

            Logger.info("Skipped " + description + ": the IDE is indexing");
            return true;
        }
    }
}
