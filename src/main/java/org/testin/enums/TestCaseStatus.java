package org.testin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestCaseStatus {
    REVIEWED(
            "Reviewed"
    ),

    PENDING(
            "Pending"
    ),

    Disabled(
            "Disabled"
    ),

    TO_BE_UPDATED(
            "To Be Updated"
    );

    private final String displayText;
}