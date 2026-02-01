package testGit.util;

public enum NodeType {
    PROJECT(0),
    SUITE(1),
    FEATURE(2);

    private final int code;

    NodeType(int code) {
        this.code = code;
    }

    public static NodeType fromCode(int code) {
        for (NodeType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Unknown node type: " + code);
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return switch (this) {
            case PROJECT -> "Project";
            case SUITE -> "Suite";
            case FEATURE -> "Feature";
        };
    }
}
