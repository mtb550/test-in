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

package org.testin.editor.toolbar;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.CheckBoxList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.ShownFields;
import org.testin.testcase.TestCaseEditorAttributes;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FieldsPopupIdeTest extends BasePlatformTestCase {

    private @NotNull String chosenBefore = "";

    private static int indexOf(final @NotNull CheckBoxList<TestCaseEditorAttributes> fields, final @NotNull TestCaseEditorAttributes field) {
        for (int i = 0; i < fields.getModel().getSize(); i++)
            if (fields.getItemAt(i) == field) return i;
        throw new AssertionError(field.getName() + " is not on the list");
    }

    private static void drawn(final @NotNull CheckBoxList<TestCaseEditorAttributes> fields) {
        fields.setSize(300, 800);
        for (int i = 0; i < fields.getModel().getSize(); i++)
            fields.getCellRenderer().getListCellRendererComponent(fields, fields.getModel().getElementAt(i), i, false, false);
    }

    private static void spaceOn(final @NotNull CheckBoxList<TestCaseEditorAttributes> fields, final @NotNull TestCaseEditorAttributes field) {
        fields.setSelectedIndex(indexOf(fields, field));
        final @NotNull KeyEvent space = new KeyEvent(fields, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, ' ');
        for (final KeyListener listener : fields.getKeyListeners()) listener.keyTyped(space);
    }

    @Override
    protected void setUp() {
        try {
            super.setUp();
            chosenBefore = Objects.toString(PropertiesComponent.getInstance().getValue(ShownFields.IN_TEST_SETS), "");
            PropertiesComponent.getInstance().unsetValue(ShownFields.IN_TEST_SETS);
        } catch (final Exception ex) {
            throw new AssertionError("Could not set up " + getName(), ex);
        }
    }

    @Override
    protected void tearDown() {
        try {
            if (chosenBefore.isEmpty()) PropertiesComponent.getInstance().unsetValue(ShownFields.IN_TEST_SETS);
            else PropertiesComponent.getInstance().setValue(ShownFields.IN_TEST_SETS, chosenBefore);
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("Could not tear down " + getName(), ex);
        }
    }

    // Rule-EDITOR-PANEL-023
    public void testOrderAndDescriptionAreAlwaysShownAndIdCanBeSwitchedOn() {
        final @NotNull TestCaseDetailsPopupBtn button = new TestCaseDetailsPopupBtn(() -> {
        });
        final @NotNull CheckBoxList<TestCaseEditorAttributes> fields = button.fieldList(() -> {
        });
        drawn(fields);

        assertFalse("ID is shown before anyone switched it on", button.getSelectedDetails().contains(TestCaseEditorAttributes.ID));

        spaceOn(fields, TestCaseEditorAttributes.ORDER);
        spaceOn(fields, TestCaseEditorAttributes.DESCRIPTION);
        spaceOn(fields, TestCaseEditorAttributes.ID);

        assertTrue("Order could be switched off", fields.isItemSelected(TestCaseEditorAttributes.ORDER) && button.getSelectedDetails().contains(TestCaseEditorAttributes.ORDER));
        assertTrue("Description could be switched off", fields.isItemSelected(TestCaseEditorAttributes.DESCRIPTION) && button.getSelectedDetails().contains(TestCaseEditorAttributes.DESCRIPTION));
        assertTrue("ID could not be switched on", button.getSelectedDetails().contains(TestCaseEditorAttributes.ID));

        ShownFields.write(ShownFields.IN_TEST_SETS, EnumSet.of(TestCaseEditorAttributes.ID));
        assertTrue("a remembered choice without Order and Description hid them", ShownFields.read(ShownFields.IN_TEST_SETS, TestCaseEditorAttributes.class).containsAll(EnumSet.of(TestCaseEditorAttributes.ORDER, TestCaseEditorAttributes.DESCRIPTION)));
    }

    // Rule-EDITOR-PANEL-024
    public void testABurstOfTicksCostsOneRedraw() {
        final @NotNull AtomicInteger redraws = new AtomicInteger();
        final @NotNull TestCaseDetailsPopupBtn button = new TestCaseDetailsPopupBtn(redraws::incrementAndGet);
        final @NotNull CheckBoxList<TestCaseEditorAttributes> fields = button.fieldList(redraws::incrementAndGet);
        drawn(fields);

        spaceOn(fields, TestCaseEditorAttributes.STEPS);
        spaceOn(fields, TestCaseEditorAttributes.MODULE);
        spaceOn(fields, TestCaseEditorAttributes.TEST_DATA);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("the ticks were not taken", button.getSelectedDetails().containsAll(EnumSet.of(TestCaseEditorAttributes.STEPS, TestCaseEditorAttributes.MODULE, TestCaseEditorAttributes.TEST_DATA)));
        assertEquals("a burst of three ticks did not cost exactly one redraw", 1, redraws.get());

        spaceOn(fields, TestCaseEditorAttributes.REFERENCE);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("a tick after the burst was drawn did not redraw", 2, redraws.get());
    }
}
