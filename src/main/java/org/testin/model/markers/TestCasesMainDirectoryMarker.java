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

package org.testin.model.markers;

import lombok.ToString;

/**
 * The test cases directory carries the audit block and nothing else.
 * <p>
 * It is still a class of its own, because the node it belongs to is one. The
 * DTO types its marker field to this class, so a runs marker cannot be handed
 * to a test-cases directory.
 */
@ToString(callSuper = true)
public class TestCasesMainDirectoryMarker extends AbstractMarker {
}
