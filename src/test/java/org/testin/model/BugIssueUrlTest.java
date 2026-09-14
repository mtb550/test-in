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

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;

/**
 * How a reported bug's address is read and shown (#28).
 */
public class BugIssueUrlTest {

    @Test
    public void anIssueReadsAsGitHubWritesAReference() {
        assertEquals(BugIssueUrl.reference("https://github.com/mtb550/product/issues/123"), "mtb550/product#123");
        assertEquals(BugIssueUrl.reference("https://ghe.acme.com/qa/product/issues/7"), "qa/product#7");
    }

    @Test
    public void somethingElseReadsAsItself() {
        assertEquals(BugIssueUrl.reference(""), "", "no link is no text");
        assertEquals(BugIssueUrl.reference("https://github.com/mtb550/product/pull/5"), "https://github.com/mtb550/product/pull/5");
    }

    @Test
    public void theAddressIsFoundInWhatGhPrinted() {
        assertEquals(BugIssueUrl.firstIn("Creating issue in mtb550/product\n\nhttps://github.com/mtb550/product/issues/42\n"),
                Optional.of("https://github.com/mtb550/product/issues/42"));
        assertEquals(BugIssueUrl.firstIn("could not create issue: HTTP 401"), Optional.empty());
    }
}
