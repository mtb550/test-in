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

package org.testin.testproject;

import org.testin.model.DirectoryType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Rule-TREE-PANEL-107: a clone is named after its repository, from any form of
 * address, so no {@code testin.yml} is needed to clone one.
 */
public class CloneTestProjectTest {

    @Test
    public void everyFormOfAddressGivesTheRepositorysName() {
        assertEquals(CloneTestProject.repositoryName("https://github.com/acme/nafath-test-cases.git"), "nafath-test-cases");
        assertEquals(CloneTestProject.repositoryName("git@github.com:acme/nafath-test-cases.git"), "nafath-test-cases");
        assertEquals(CloneTestProject.repositoryName("ssh://git@host:2222/acme/nafath-test-cases.git"), "nafath-test-cases");
        assertEquals(CloneTestProject.repositoryName("https://host/acme/nafath-test-cases/"), "nafath-test-cases");
        assertEquals(CloneTestProject.repositoryName("https://host/acme/nafath-test-cases"), "nafath-test-cases");
        assertEquals(CloneTestProject.repositoryName("  https://host/acme/Checkout.GIT  "), "Checkout");
        assertEquals(CloneTestProject.repositoryName("https://host/acme/checkout?ref=main#readme"), "checkout");
    }

    @Test
    public void anAccountInTheAddressIsNotPartOfTheName() {
        assertEquals(CloneTestProject.repositoryName("https://muteb@bitbucket.org/acme/tests.git"), "tests");
    }

    /**
     * The name becomes the first Java package of everything under the project,
     * so one Java refuses is made into one it accepts (Rule-TREE-PANEL-095).
     */
    @Test
    public void aNameJavaRefusesIsMadeIntoOneItAccepts() {
        assertTrue(DirectoryType.TP.canTakeName(CloneTestProject.repositoryName("https://github.com/acme/new.git")),
                "new is a word Java keeps for itself");
        assertTrue(DirectoryType.TP.canTakeName(CloneTestProject.repositoryName("https://github.com/acme/nafath-test-cases.git")),
                "and a name it already accepts is kept as it is");
    }
}
