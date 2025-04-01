package com.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Feature {
    private  String name;
    private List<TestCase> testCases;

}
