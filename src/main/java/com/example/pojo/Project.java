package com.example.pojo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Project implements Directory {

    @JsonAlias("project_id")
    private Integer id;

    private String name;

    private Integer active;

    private File file;

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
