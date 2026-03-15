package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    PR("Project"),
    /// to be removed
    PA("Package"),
    TS("Test Set"),
    TR("Test Run"),
    TCP("Test Cases Directory"),
    TRP("Test Runs Directory");

    private final String description;
}