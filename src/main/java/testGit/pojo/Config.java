package testGit.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;


public class Config {

    @Setter
    @Getter
    private static Path testGitPath;

    @Setter
    @Getter
    private static Path automationPath;

    @Setter
    @Getter
    private static Project project;

    /// to be removed

    public static ObjectMapper getMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}