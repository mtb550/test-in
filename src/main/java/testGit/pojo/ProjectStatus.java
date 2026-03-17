package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectStatus {
    AC("Active"),
    IN("Inactive"),
    RE("Removed"),
    AR("Archived");

    private final String description;
}