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

/**
 * The Details tool window: what the selected test case, run or node <i>is</i> -
 * its steps, its history, its open bugs, who made it and when.
 * <p>
 * One surface, beside the tree rather than in the editor tabs. It is not
 * {@link org.testin.ui}, which is the look every surface borrows, and not
 * {@link org.testin.editor}, which is where a tester changes a case rather than
 * reads one (#110).
 */
package org.testin.view;
