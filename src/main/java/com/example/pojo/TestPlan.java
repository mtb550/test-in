package com.example.pojo;

import lombok.Data;

@Data
public class TestPlan {
    private Integer id;
    private String name;
    private Integer type;           // 0 = test plan, 1 = test run
    private Integer link;           // if type = test run, this is the test plan id
    private Integer project_id;     // owning project

    private String created_by;
    private String created_at;

    private String modified_by;
    private String modified_at;

    private String assigned_to;
    private String last_execution;

    private Integer status;         // 0 = created, 1 = in progress, 2 = completed
}

