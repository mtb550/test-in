package com.example.demo;

import java.util.List;

public class Feature {
    private final String name;
    private final List<TestCase> testCases;

    public Feature(String name, List<TestCase> testCases) {
        this.name = name;
        this.testCases = testCases;
    }

    public String getName() {
        return name;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }
}
