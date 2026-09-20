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

package org.testin.git;

import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunConfiguration;
import org.testin.testcase.TestEditorAttributes;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * What a merge calls one field (#305, Rule-SHARE-081).
 * <p>
 * The class exists because two spellings of one field name drifted apart: a
 * question row read {@code configuration.PLATFORM} while the settled line beside
 * it said <b>Platform</b>. So the thing worth asserting is not the words - the
 * three enums own those - but that every answer comes from them, and that a key
 * none of them carries is handed back rather than lost.
 */
public class FieldNameTest {

    @Test
    public void aTestCaseFieldIsNamedByTheEditorsOwnEnum() {
        assertEquals(FieldName.of("updatedAt"), TestEditorAttributes.UPDATED_AT.getName(),
                "a camelCase JSON field is the enum constant with the underscore");
    }

    @Test
    public void aRunsConfigurationKeyIsNamedByTheQuestionTheTesterAnswered() {
        final TestRunConfiguration question = TestRunConfiguration.values()[0];

        assertEquals(FieldName.of("configuration." + question.name()), question.getDisplayName(),
                "the key is named as the run creation dialog names it");
    }

    @Test
    public void aResultAnalysisKeyIsNamedByItsHeading() {
        final ResultAnalysis heading = ResultAnalysis.values()[0];

        assertEquals(FieldName.of("resultAnalysis." + heading.name()), heading.getLabel(),
                "the key is named as the analysis dialog heads it");
    }

    @Test
    public void aFieldNoEnumCarriesKeepsItsOwnName() {
        assertEquals(FieldName.of("somethingOnlyTheFileHas"), "somethingOnlyTheFileHas",
                "the merge works on the file, which may hold more than the model does");
        assertEquals(FieldName.of("configuration.SOMETHING_ELSE"), "configuration.SOMETHING_ELSE",
                "and a key inside an object keeps the whole name, so it is still findable");
    }
}
