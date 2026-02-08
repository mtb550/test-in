package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    P("Project"),
    S("Suite"),
    F("Feature"),
    TP("Test Plan"),
    TR("Test Run");

    private final String description;
}