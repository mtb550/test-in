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

package org.testin.util;

import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Shortcut matching (#48, MagicConstant).
 * <p>
 * The rule compares a KeyEvent against a KeyStroke, and the two report their
 * modifiers differently — which is what the inspection was pointing at.
 */
public class ShortcutMatchesTest {

    private static KeyEvent event(final int keyCode, final int modifiersEx) {
        return new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0L, modifiersEx, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    @Test
    public void aShortcutWithNoModifierMatches() {
        assertTrue(Shortcuts.matches(event(KeyEvent.VK_ENTER, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)));
    }

    @Test
    public void aShortcutWithAModifierMatches() {
        assertTrue(Shortcuts.matches(event(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)));
    }

    @Test
    public void aDifferentModifierDoesNotMatch() {
        assertFalse(Shortcuts.matches(event(KeyEvent.VK_C, InputEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)));
    }

    @Test
    public void aBareKeyDoesNotMatchTheSameKeyWithAModifier() {
        assertFalse(Shortcuts.matches(event(KeyEvent.VK_C, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)));
    }
}
