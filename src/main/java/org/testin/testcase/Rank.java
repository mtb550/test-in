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

package org.testin.testcase;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Rank {
    public static final @NotNull String MIDDLE = "m";
    private static final char FIRST = 'a';
    private static final char LAST = 'z';
    private static final char BELOW_FIRST = FIRST - 1;
    private static final char ABOVE_LAST = LAST + 1;
    private static final char SPREAD_FIRST = 'b';
    private static final int SPREAD_DIGITS = LAST - SPREAD_FIRST + 1;

    public static @NotNull String between(final @NotNull String before, final @NotNull String after) {
        if (after.isEmpty()) return after(before);

        return midpoint(before, after);
    }

    public static @NotNull String after(final @NotNull String last) {
        if (last.isEmpty()) return MIDDLE;

        final char lastChar = last.charAt(last.length() - 1);

        if (lastChar < LAST) {
            return last.substring(0, last.length() - 1) + (char) (lastChar + 1);
        }

        return last + MIDDLE;
    }

    public static @NotNull List<String> spread(final int count) {
        if (count <= 0) return List.of();

        int width = 1;
        long slots = SPREAD_DIGITS;
        while (slots < count + 1L) {
            width++;
            slots *= SPREAD_DIGITS;
        }

        final long step = slots / (count + 1L);
        final @NotNull List<String> ranks = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            ranks.add(digits((i + 1) * step, width));
        }

        return List.copyOf(ranks);
    }

    private static @NotNull String digits(final long value, final int width) {
        final char[] rank = new char[width];
        long left = value;

        for (int i = width - 1; i >= 0; i--) {
            rank[i] = (char) (SPREAD_FIRST + (left % SPREAD_DIGITS));
            left /= SPREAD_DIGITS;
        }

        return new String(rank);
    }

    private static @NotNull String midpoint(final @NotNull String before, final @NotNull String after) {
        final @NotNull StringBuilder rank = new StringBuilder();

        for (int i = 0; ; i++) {
            final char low = i < before.length() ? before.charAt(i) : BELOW_FIRST;
            final char high = i < after.length() ? after.charAt(i) : ABOVE_LAST;

            if (high - low > 1) {
                return settled(rank.append((char) ((low + high) / 2)));
            }

            rank.append(low == BELOW_FIRST ? FIRST : low);
        }
    }

    private static @NotNull String settled(final @NotNull StringBuilder rank) {
        if (rank.charAt(rank.length() - 1) == FIRST) rank.append(MIDDLE);

        return rank.toString();
    }
}
