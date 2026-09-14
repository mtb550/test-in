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

package org.testin.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a run item's stacktrace holds: the text of the error, and the screenshots
 * a tester pasted between it (#28).
 * <p>
 * The Error Capture box stores a pasted screenshot as a
 * {@code data:image/png;base64,...} run inside the same string as the text, so
 * everything that wants the two apart - a bug report attaching the screenshots
 * and putting the text in its body, #50's report opening both - asks here
 * rather than matching the pattern itself.
 * <p>
 * Only {@code data:image/png} is recognized, because the framework's text area is
 * the only thing that writes one. A run that will not decode to a PNG is not a
 * screenshot, and stays text: what was stored is never dropped.
 *
 * @param segments the stacktrace in the order it was written
 */
public record Stacktrace(@NotNull List<Segment> segments) {

    private static final @NotNull Pattern SCREENSHOT = Pattern.compile("data:image/png;base64,([A-Za-z0-9+/=]+)");

    private static final byte @NotNull [] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    /**
     * One piece of a stacktrace: text, or a screenshot.
     */
    public sealed interface Segment permits Text, Screenshot {
    }

    /**
     * Text as it was typed or pasted.
     */
    public record Text(@NotNull String text) implements Segment {
    }

    /**
     * A pasted screenshot, decoded.
     *
     * @param png the image's bytes
     */
    public record Screenshot(byte @NotNull [] png) implements Segment {
    }

    public static @NotNull Stacktrace of(final @NotNull String stored) {
        final @NotNull List<Segment> segments = new ArrayList<>();
        final @NotNull StringBuilder text = new StringBuilder();
        final @NotNull Matcher screenshot = SCREENSHOT.matcher(stored);
        int from = 0;

        while (screenshot.find()) {
            text.append(stored, from, screenshot.start());
            from = screenshot.end();

            final byte @NotNull [] png = decoded(screenshot.group(1));
            if (!isPng(png)) {
                text.append(screenshot.group());
                continue;
            }

            if (!text.isEmpty()) segments.add(new Text(text.toString()));
            text.setLength(0);
            segments.add(new Screenshot(png));
        }

        text.append(stored, from, stored.length());
        if (!text.isEmpty()) segments.add(new Text(text.toString()));

        return new Stacktrace(List.copyOf(segments));
    }

    /**
     * The bytes behind a base64 run, and none when it will not decode.
     */
    private static byte @NotNull [] decoded(final @NotNull String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (final IllegalArgumentException notBase64) {
            return new byte[0];
        }
    }

    /**
     * Whether the bytes open with the PNG signature. The decoder accepts runs a
     * picture never ends in, so a pasted-looking string in an error message is
     * only a screenshot when what it decodes to is one.
     */
    private static boolean isPng(final byte @NotNull [] bytes) {
        return bytes.length > PNG_SIGNATURE.length
                && Arrays.equals(bytes, 0, PNG_SIGNATURE.length, PNG_SIGNATURE, 0, PNG_SIGNATURE.length);
    }

    /**
     * The text with the screenshots taken out, stripped of the blank lines they
     * leave at either end.
     */
    public @NotNull String text() {
        final @NotNull StringBuilder text = new StringBuilder();
        for (final Segment segment : segments) {
            if (segment instanceof Text piece) text.append(piece.text());
        }
        return text.toString().strip();
    }

    /**
     * The screenshots, in the order they were pasted.
     */
    public @NotNull List<byte[]> screenshots() {
        final @NotNull List<byte[]> screenshots = new ArrayList<>();
        for (final Segment segment : segments) {
            if (segment instanceof Screenshot image) screenshots.add(image.png());
        }
        return List.copyOf(screenshots);
    }

    /**
     * The first line of text that says anything - never a screenshot, so a
     * stacktrace that opens with one still has a readable first line - and empty
     * when there is no text at all.
     */
    public @NotNull String firstLine() {
        return text().lines().filter(line -> !line.isBlank()).findFirst().orElse("").strip();
    }
}
