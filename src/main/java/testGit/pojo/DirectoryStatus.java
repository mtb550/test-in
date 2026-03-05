package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DirectoryStatus {
    AC("Active"),
    IN("Inactive"),
    AR("Archived");

    private final String description;
}