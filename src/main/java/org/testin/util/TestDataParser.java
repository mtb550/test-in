package org.testin.util;

import org.testin.enums.Group;
import org.testin.enums.Priority;
import org.testin.mappers.Config;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Defensive parsing of values imported from tables and external files.
 */
final class TestDataParser {

    private static final Pattern MULTI_STEP_LINE = Pattern.compile(".*\\s\\d+[-.].*");
    private static final Pattern STEP_SEPARATOR = Pattern.compile("(\\s)(?=\\d+[-.])");
    private static final Pattern STEP_PREFIX = Pattern.compile("^\\d+[-.]\\s*");

    List<String> steps(final String rawSteps) {
        if (rawSteps == null || rawSteps.isBlank()) return new ArrayList<>();
        String text = rawSteps;
        if (!text.contains("\n") && MULTI_STEP_LINE.matcher(text).matches()) {
            text = STEP_SEPARATOR.matcher(text).replaceAll("\n");
        }
        return Arrays.stream(text.split("\n"))
                .map(line -> STEP_PREFIX.matcher(line).replaceFirst("").trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    Priority priority(final String value) {
        if (value == null || value.isBlank()) return Priority.LOW;
        try {
            return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return Priority.LOW;
        }
    }

    ZonedDateTime date(final String value) {
        if (value == null || value.isBlank()) return now();
        try {
            return LocalDateTime.parse(value, Config.EXCEL_DATE_FORMATTER)
                    .atZone(ZoneId.systemDefault());
        } catch (final Exception ignored) {
            return now();
        }
    }

    List<Group> groups(final String rawGroups) {
        if (rawGroups == null || rawGroups.isBlank()) return new ArrayList<>();
        return Arrays.stream(rawGroups.split(","))
                .map(String::trim)
                .filter(group -> !group.isEmpty())
                .map(group -> {
                    try {
                        return Group.valueOf(group.toUpperCase(Locale.ROOT));
                    } catch (final IllegalArgumentException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
