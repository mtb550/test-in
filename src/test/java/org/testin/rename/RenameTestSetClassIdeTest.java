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

package org.testin.rename;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.model.dto.dirs.TestSetDirectoryDto;

import java.util.concurrent.atomic.AtomicBoolean;

public class RenameTestSetClassIdeTest extends AbstractCodegenIdeTest {

    private void renamed(final @NotNull TestSetDirectoryDto ts, final @NotNull String newName) {
        final @NotNull AtomicBoolean done = new AtomicBoolean();
        NodeRename.apply(getProject(), ts, newName, () -> done.set(true));

        Await.until("the rename of the test set never finished", done::get);
    }

    // Rule-CODEGEN-051, Rule-CODEGEN-004, Rule-CODEGEN-052
    public void testRenamingATestSetRenamesItsClass() {
        renamed(createdTestSet("Login"), "Sign in");

        assertTrue("the class did not take the test set's new name", generatedClass("nafath.SignInTest").isPresent());
        assertTrue("the class kept the test set's old name", generatedClass("nafath.LoginTest").isEmpty());
    }
}
