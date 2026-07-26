package org.testin.pojo;

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