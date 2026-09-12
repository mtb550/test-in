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

package org.testin.java.codegen.method;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * Splitting an FQCN list into the parts the generator writes a method from (#48).
 * <p>
 * {@code executeSync} guarded {@code fqcn.size() < 2} and {@code execute} did
 * not, while both then read {@code fqcn.get(size - 2)} - so the same short list
 * was reported cleanly through one entry point and threw
 * {@code IndexOutOfBoundsException} through the other. The split now happens in
 * one place, and these are the cases that used to diverge.
 */
public class CreateTestMethodTargetTest {

    @Test
    public void anEmptyListHasNoClassOrMethod() {
        assertFalse(CreateTestMethod.parse(List.of()).isPresent());
    }

    @Test
    public void aSingleSegmentIsAMethodWithNoClass() {
        // The case that threw: getLast() succeeds, get(size - 2) is index -1.
        assertFalse(CreateTestMethod.parse(List.of("shouldDoSomething")).isPresent());
    }

    @Test
    public void aClassAndMethodAreTheSmallestUsableList() {
        final CreateTestMethod.Target target =
                CreateTestMethod.parse(List.of("LoginTest", "shouldLogIn")).orElseThrow();

        assertEquals(target.className(), "LoginTest");
        assertEquals(target.methodName(), "shouldLogIn");
        assertEquals(target.path(), "LoginTest");
        assertEquals(target.packageList(), List.of(), "no package segments are left over");
    }

    @Test
    public void packageSegmentsAreEverythingBeforeTheClass() {
        final CreateTestMethod.Target target =
                CreateTestMethod.parse(List.of("org", "example", "tests", "LoginTest", "shouldLogIn")).orElseThrow();

        assertEquals(target.className(), "LoginTest");
        assertEquals(target.methodName(), "shouldLogIn");
        assertEquals(target.path(), "org.example.tests.LoginTest");
        assertEquals(target.packageList(), List.of("org", "example", "tests"));
    }
}
