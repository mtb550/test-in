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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SeparatedValues {
    private static final String BYTE_ORDER_MARK = "﻿";

    // Rule-EDITOR-PANEL-254, Rule-SHARE-124
    public static @NotNull List<List<String>> split(final @NotNull String text, final char separator) {
        final @NotNull List<List<String>> records = new ArrayList<>();
        @NotNull List<String> fields = new ArrayList<>();
        final @NotNull StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        final int start = text.startsWith(BYTE_ORDER_MARK) ? 1 : 0;
        for (int i = start; i < text.length(); i++) {
            final char c = text.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"' && current.isEmpty()) {
                inQuotes = true;
            } else if (c == separator) {
                fields.add(current.toString());
                current.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                fields.add(current.toString());
                current.setLength(0);
                records.add(fields);
                fields = new ArrayList<>();
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty() || !fields.isEmpty()) {
            fields.add(current.toString());
            records.add(fields);
        }

        return records;
    }
}
