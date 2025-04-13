package com.example.pojo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Tree {

    private Integer id;

    private String name;

    private Integer type;

    private Integer link;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAlias("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAlias("created_by")
    private String createdBy;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAlias("modified_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAlias("modified_by")
    private String modifiedBy;

    public Tree(String name, Integer type, Integer link, LocalDateTime modifiedAt, String modifiedBy) {
        this.name = name;
        this.type = type;
        this.link = link;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }

    public Tree(int id, String name, Integer type, Integer link) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.link = link;
    }

    public Tree(String name, Integer type, Integer link) {
        this.name = name;
        this.type = type;
        this.link = link;
    }
}
