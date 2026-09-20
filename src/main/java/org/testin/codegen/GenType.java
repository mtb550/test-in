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
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Once;

import java.util.List;

@Getter
public enum GenType {
    REMOVE_TEST_PROJECT(
            Bundle.message("codegen.remove.test.project")
    ),

    RENAME_TEST_PROJECT(
            Bundle.message("codegen.rename.test.project")
    ),

    REMOVE_TEST_SET_PACKAGE(
            Bundle.message("codegen.remove.test.set.package")
    ),

    RENAME_TEST_SET_PACKAGE(
            Bundle.message("codegen.rename.test.set.package")
    ),

    MOVE_TEST_SET_PACKAGE(
            Bundle.message("codegen.move.test.set.package")
    ),

    CREATE_TEST_SET(
            Bundle.message("codegen.create.test.set")
    ),

    REMOVE_TEST_SET(
            Bundle.message("codegen.remove.test.set")
    ),

    RENAME_TEST_SET(
            Bundle.message("codegen.rename.test.set")
    ),

    MOVE_TEST_SET(
            Bundle.message("codegen.move.test.set")
    ),

    CREATE_TEST_CASE(
            Bundle.message("codegen.create.test.case")
    ),

    REMOVE_TEST_CASE(
            Bundle.message("codegen.remove.test.case")
    ),

    // UC-CODEGEN-002, Rule-CODEGEN-077
    MOVE_TEST_CASE(
            Bundle.message("codegen.move.test.case")
    ),

    // UC-CODEGEN-002, Rule-CODEGEN-078
    COPY_TEST_CASE(
            Bundle.message("codegen.copy.test.case")
    ),

    UPDATE_TEST_CASE_DESCRIPTION(
            Bundle.message("codegen.update.test.case")
    ),

    UPDATE_TEST_CASE_EXPECTED_RESULT(
            Bundle.message("codegen.update.test.case"),
            "expected result"
    ),

    UPDATE_TEST_CASE_MODULE(
            Bundle.message("codegen.update.test.case"),
            "module"
    ),

    UPDATE_TEST_CASE_TEST_DATA(
            Bundle.message("codegen.update.test.case"),
            "test data"
    ),

    UPDATE_TEST_CASE_PRE_CONDITIONS(
            Bundle.message("codegen.update.test.case"),
            "pre-conditions"
    ),

    UPDATE_TEST_CASE_STEPS(
            Bundle.message("codegen.update.test.case"),
            "steps"
    ),

    UPDATE_TEST_CASE_GROUP(
            Bundle.message("codegen.update.test.case")
    ),

    UPDATE_TEST_CASE_PRIORITY(
            Bundle.message("codegen.update.test.case"),
            "priority"
    ),

    UPDATE_TEST_CASE_ORDER(
            Bundle.message("codegen.update.test.case")
    ),

    RECONCILE_TEST_CASE(
            Bundle.message("codegen.restore.test.case")
    ),

    UPDATE_TEST_CASE_STATUS(
            Bundle.message("codegen.update.test.case")
    ),

    NO_CODE_CHANGE(
            Bundle.message("codegen.no.code.change"),
            "read-only attribute"
    );

    private final @NotNull String description;

    private final @NotNull GenAction action;

    GenType(final @NotNull String description) {
        this.description = description;
        this.action = new JavaCodeUpdate();
    }

    GenType(final @NotNull String description, final @NotNull String dataOnlyField) {
        this.description = description;
        this.action = new NoOpCodeUpdate(dataOnlyField);
    }

    private static final @NotNull Key<Boolean> INDEXING_SAID = Key.create("testin.codegen.indexingSaid");

    private final class JavaCodeUpdate implements GenAction {
        // UC-CODEGEN-019, Rule-CODEGEN-005
        @Override
        public void execute(final @NotNull Project p, final @NotNull Object obj) {
            if (!canGenerate(p)) return;

            CodeGenerators.find(GenType.this).execute(p, obj);
        }

        // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-EDITOR-PANEL-046
        @Override
        public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
            if (!canGenerate(p) || items.isEmpty()) return;

            ApplicationManager.getApplication().invokeLater(() ->
                    WriteCommandAction.runWriteCommandAction(p, description, null,
                            () -> CodeGenerators.find(GenType.this).executeAll(p, items)));
        }

        // UC-CODEGEN-019, Rule-CODEGEN-005, Rule-CODEGEN-006, Rule-CODEGEN-082
        private boolean canGenerate(final @NotNull Project p) {
            if (!CodeOn.isOnOrWarnOnce(p)) return false;
            if (!DumbService.isDumb(p)) return true;

            if (Once.claim(p, INDEXING_SAID)) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Refused.WHILE_INDEXING, description);

                DumbService.getInstance(p).runWhenSmart(() -> p.putUserData(INDEXING_SAID, null));
            }

            Logger.info("Skipped " + name() + ": the IDE is indexing");
            return false;
        }
    }

    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        action.executeAll(p, items);
    }
}
