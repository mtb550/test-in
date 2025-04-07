package com.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TestCase {
    private int order;

    private String id;

    private String title;

    private String expectedResult;

    private String steps;

    private String priority;

    private String automationRef;

    private List<GroupType> group;

    private String createBy;

    private String updateBy;

    private String createAt;

    private String updateAt;

    private String module;

}
