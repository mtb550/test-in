package org.testin.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;


public class Config {

    @Setter
    @Getter
    @Nullable
    private static Path testinPath = null;

    @Setter
    @Getter
    @Nullable
    private static Path automationPath = null;

    @Setter
    @Getter
    @NotNull
    private static Project project; // todo, to be removed as we can get the project from toolwindow class e.getProject()


    public static ObjectMapper getMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}