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

package org.testin.lightmode;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.components.AbstractDetailsPopupBtn;
import org.testin.util.Bundle;

/**
 * The light mode window's view menu: four checkboxes for what it shows (#13).
 * <p>
 * The same button the two editors put on their toolbars, which is what its own
 * Javadoc anticipated - "a third editor needs no new plumbing". The popup, the
 * check-box list, the per-machine persistence and the debounced redraw all come
 * from there; what is here is the enum and the key.
 */
class ViewMenuBtn extends AbstractDetailsPopupBtn<LightModePart> {

    ViewMenuBtn(final @NotNull Runnable onChanged) {
        // v2: the expected result left the list, so a v1 selection holds an
        // option that no longer exists, and reading it back would log an
        // unknown-attribute error on every open. The versioning rule is
        // AbstractDetailsPopupBtn's; what is local is that it had to be used.
        super(Bundle.message("light.view.menu"),
                "testin.lightMode.parts.v2",
                LightModePart.class,
                onChanged);
    }
}
