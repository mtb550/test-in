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

package org.testin.sftp;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * UC-SHARE-021, Rule-SHARE-114.
 * <p>
 * One conflict the tester has settled: what they agreed to, and what the server
 * held when they were asked.
 * <p>
 * The second half is the whole reason this is a record rather than a string.
 * Answering a conflict takes as long as it takes - a question per rewritten
 * field, one window at a time - and a colleague can sync a newer version of that
 * same test case while the windows are open. The answers were written to the
 * server unconditionally, so that newer version was overwritten without a word,
 * and the colleague's next sync then took Testin's copy back over their own: one
 * edit lost, on both machines, with nothing anywhere saying it had happened
 * (#312, A33).
 * <p>
 * So the answer carries the version it was an answer to, and the send compares
 * it with what the server holds now. A file that moved under the questions is
 * left for the next sync, which asks about it again - against the version that
 * is actually there.
 *
 * @param settled         what the tester agreed the test case should be
 * @param theirsWhenAsked the server's whole version at the moment the questions
 *                        were put, which is what the answers were about
 */
public record Answered(@NotNull String settled, @NotNull String theirsWhenAsked) {

    /**
     * Whether the server still holds the version these answers were about.
     * <p>
     * Compared by digest through the manifest's own entry, so this asks the same
     * question the sync asks of every other file, in the same words.
     */
    public boolean stillAnswers(final @NotNull Manifest.Entry onServer) {
        return Manifest.Entry.of(theirsWhenAsked.getBytes(StandardCharsets.UTF_8)).sha256().equals(onServer.sha256());
    }
}
