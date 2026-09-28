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

package org.testin.model;

import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class DirectoryIdentityTest {

    private static final Path LOGIN = Path.of("NAFATH", "testCases", "Login");

    @Test
    public void twoReadingsOfOneFolderAreOneNode() {
        final TestSetDirectoryDto first = TestSetDirectoryDto.builder().path(LOGIN).name("Login").build();
        final TestSetDirectoryDto second = TestSetDirectoryDto.builder().path(LOGIN).name("Log in").build();

        assertEquals(second, first);
        assertEquals(new HashSet<>(List.of(first, second)).size(), 1, "a set held one folder twice");
    }

    @Test
    public void twoKindsAtOnePathAreTwoNodes() {
        assertNotEquals(TestSetPackageDirectoryDto.builder().path(LOGIN).build(), TestSetDirectoryDto.builder().path(LOGIN).build());
    }

    @Test
    public void twoFoldersAreTwoNodes() {
        assertNotEquals(TestSetDirectoryDto.builder().path(LOGIN.resolveSibling("Logout")).build(), TestSetDirectoryDto.builder().path(LOGIN).build());
    }
}
