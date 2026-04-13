package testGit.pojo;

import java.util.Objects;

public enum TestRunStatus {
    CREATED(
            0,
            "Created"
    ),

    IN_PROGRESS(
            1,
            "In Progress"
    ),

    COMPLETED(
            2,
            "Completed"
    );

    private final Integer code;
    private final String label;

    TestRunStatus(final int code, final String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelFor(final Integer code) {
        for (TestRunStatus s : values()) {
            if (Objects.equals(s.code, code)) return s.label;
        }
        return "Unknown";
    }
}

