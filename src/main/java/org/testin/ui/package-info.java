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
 * How everything looks: the dialog framework, the badges, the menus, the
 * animation, the zoom and the fonts - parts named for what they are on screen,
 * never for what they hold.
 * <p>
 * Told apart from its two neighbours by who it serves: {@code ui} serves every
 * surface, {@link org.testin.view} is one surface, and {@link org.testin.editor}
 * is another (#110). A class here is reached by an editor, a tree and a tool
 * window alike, which is the test of whether it belongs.
 */
package org.testin.ui;
