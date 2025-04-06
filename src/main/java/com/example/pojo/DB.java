package com.example.pojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DB {
    public static List<Project> loadProjects() {
        List<TestCase> loginTests = Arrays.asList(
                new TestCase("login-01", "Login with valid credentials", "Dashboard shown", "Enter username and password", "High", "test.LoginTest"),
                new TestCase("login-02", "Login with invalid password", "Error message displayed", "Enter wrong password", "Medium", "test.LoginTest.test2")
        );

        List<TestCase> logoutTests = Arrays.asList(
                new TestCase("logout-01", "Logout from profile page", "Redirected to login", "Click logout", "Low", "test.LogoutTest.test5")
        );

        Feature loginFeature = new Feature("Login", loginTests);
        Feature logoutFeature = new Feature("Logout", logoutTests);

        Project project = new Project("Project A", Arrays.asList(loginFeature, logoutFeature));

        return new ArrayList<>(List.of(project));
    }

    public static List<TestCaseHistory> loadTestCaseHistory() {
        List<TestCaseHistory> history = new ArrayList<>();
        history.add(new TestCaseHistory("2024-03-01", "Created test case"));
        history.add(new TestCaseHistory("2024-03-15", "Updated expected result"));
        return history;
    }

    public static Feature getFeature(String projectName, String featureName) {
        return loadProjects().stream()
                .filter(p -> p.getName().equals(projectName))
                .flatMap(p -> p.getFeatures().stream())
                .filter(f -> f.getName().equals(featureName))
                .findFirst()
                .orElse(null);
    }

}
