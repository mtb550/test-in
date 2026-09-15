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

package org.testin.clipboard;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Whether a paste is a move, asked of the cases being pasted (#312, A55).
 * <p>
 * The clipboard is the whole IDE's and a cut is one project's, so a copy made
 * after the cut - in another window, or of other cases - is what a paste puts
 * down. Deciding "move" from "a cut is waiting" removed the cut cases and
 * pasted the copied ones as though they had moved.
 */
public class CutStateTest {

    /**
     * An editor nothing here asks anything of: the state only remembers which
     * one the cut came from.
     */
    private static @NotNull TestinEditor anyEditor() {
        return (TestinEditor) Proxy.newProxyInstance(CutStateTest.class.getClassLoader(), new Class<?>[]{TestinEditor.class}, (proxy, method, args) -> null);
    }

    private static @NotNull TestCaseDto testCase() {
        return TestCaseDto.builder().id(UUID.randomUUID()).build();
    }

    @Test
    public void theCasesThatWereCutArePastedAsAMove() {
        final @NotNull TestCaseDto first = testCase();
        final @NotNull TestCaseDto second = testCase();
        final @NotNull CutState state = new CutState();

        state.cut(anyEditor(), List.of(first, second));

        assertTrue(state.isCutOf(List.of(first, second)));
    }

    @Test
    public void otherCasesCopiedAfterTheCutArePastedAsACopy() {
        final @NotNull CutState state = new CutState();

        state.cut(anyEditor(), List.of(testCase()));

        assertTrue(state.isCutting(), "the cut is still waiting");
        assertFalse(state.isCutOf(List.of(testCase())), "a different case on the clipboard is not the cut, so pasting it moves nothing");
    }

    @Test
    public void aClipboardHoldingTheCutAndMoreIsNotTheCut() {
        final @NotNull TestCaseDto cut = testCase();
        final @NotNull CutState state = new CutState();

        state.cut(anyEditor(), List.of(cut));

        assertFalse(state.isCutOf(List.of(cut, testCase())));
    }

    @Test
    public void nothingPastedIsNotAMove() {
        final @NotNull CutState state = new CutState();

        state.cut(anyEditor(), List.of(testCase()));

        assertFalse(state.isCutOf(List.of()));
    }
}
