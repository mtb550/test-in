package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    REMOVED("Removed"),
    ARCHIVED("Archived");

    private final String description;
}