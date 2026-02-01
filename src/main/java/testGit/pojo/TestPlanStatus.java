package testGit.pojo;

import java.util.Objects;

public enum TestPlanStatus {
    CREATED(0, "Created"),
    IN_PROGRESS(1, "In Progress"),
    COMPLETED(2, "Completed");

    private final Integer code;
    private final String label;

    TestPlanStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelFor(Integer code) {
        for (TestPlanStatus s : values()) {
            if (Objects.equals(s.code, code)) return s.label;
        }
        return "Unknown";
    }
}

