package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.Group;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GitDiffProcessor {

    public static List<TestCaseDiff> getPendingChanges(final @NotNull Project p, Path repoRoot) {
        List<TestCaseDiff> allChanges = new ArrayList<>();

        String statusOutput = GitCommandRunner.execute(repoRoot, "git", "status", "--porcelain", "-uall");

        if (statusOutput.trim().isEmpty()) {
            return allChanges;
        }

        String[] lines = statusOutput.split("\n");

        for (String line : lines) {
            if (line.trim().length() < 4) continue;

            String statusCode = line.substring(0, 2);
            String relativePathStr = line.substring(3).trim();

            if (relativePathStr.startsWith("\"") && relativePathStr.endsWith("\"")) {
                relativePathStr = relativePathStr.substring(1, relativePathStr.length() - 1);
            }

            if (!relativePathStr.endsWith(".json")) continue;

            Path absolutePath = repoRoot.resolve(relativePathStr);
            Path relativePath = Path.of(relativePathStr);

            if (statusCode.contains("A") || statusCode.contains("?")) {
                TestCaseDto newDto = Services.getInstance(p, Mapper.class).readValue(absolutePath.toFile(), TestCaseDto.class);
                allChanges.add(new TestCaseDiff(
                        newDto.getId().toString(),
                        relativePath,
                        TestCaseDiff.DiffType.ADDED,
                        null,
                        newDto,
                        List.of(new TestCaseDiff.FieldChange(
                                "Test Case",
                                "",
                                newDto.getDescription(),
                                TestCaseDiff.ChangeType.CREATE_TEST_CASE
                        ))
                ));

            } else if (statusCode.contains("M")) {
                TestCaseDto newDto = Services.getInstance(p, Mapper.class).readValue(absolutePath.toFile(), TestCaseDto.class);

                String gitPath = relativePathStr.replace("\\", "/");
                String oldJsonString = GitCommandRunner.execute(repoRoot, "git", "show", "HEAD:" + gitPath);

                TestCaseDto oldDto = Services.getInstance(p, Mapper.class).readValue(oldJsonString, TestCaseDto.class);

                List<TestCaseDiff.FieldChange> fieldChanges = compareFields(oldDto, newDto);
                if (!fieldChanges.isEmpty()) {
                    allChanges.add(new TestCaseDiff(
                            newDto.getId().toString(),
                            relativePath,
                            TestCaseDiff.DiffType.MODIFIED,
                            oldDto,
                            newDto,
                            fieldChanges
                    ));
                }
            } else if (statusCode.contains("D")) {
                String gitPath = relativePathStr.replace("\\", "/");
                String oldJsonString = GitCommandRunner.execute(repoRoot, "git", "show", "HEAD:" + gitPath);
                TestCaseDto oldDto = Services.getInstance(p, Mapper.class).readValue(oldJsonString, TestCaseDto.class);

                allChanges.add(new TestCaseDiff(
                        oldDto.getId().toString(),
                        relativePath,
                        TestCaseDiff.DiffType.DELETED,
                        oldDto,
                        null,
                        List.of(new TestCaseDiff.FieldChange(
                                "Test Case",
                                oldDto.getDescription(),
                                "",
                                TestCaseDiff.ChangeType.REMOVE_TEST_CASE
                        ))
                ));
            }
        }
        return allChanges;
    }

    private static List<TestCaseDiff.FieldChange> compareFields(TestCaseDto oldDto, TestCaseDto newDto) {
        List<TestCaseDiff.FieldChange> changes = new ArrayList<>();

        if (!Objects.equals(oldDto.getDescription(), newDto.getDescription())) {
            changes.add(new TestCaseDiff.FieldChange(
                    "Description",
                    oldDto.getDescription(),
                    newDto.getDescription(),
                    TestCaseDiff.ChangeType.CHANGE_DESCRIPTION
            ));
        }
        if (!Objects.equals(oldDto.getExpectedResult(), newDto.getExpectedResult())) {
            changes.add(new TestCaseDiff.FieldChange(
                    "Expected Result",
                    oldDto.getExpectedResult(),
                    newDto.getExpectedResult(),
                    TestCaseDiff.ChangeType.CHANGE_EXPECTED_RESULT
            ));
        }
        if (!Objects.equals(oldDto.getPriority(), newDto.getPriority())) {
            String oldP = oldDto.getPriority().name();
            String newP = newDto.getPriority().name();
            changes.add(new TestCaseDiff.FieldChange(
                    "Priority",
                    oldP,
                    newP,
                    TestCaseDiff.ChangeType.CHANGE_PRIORITY
            ));
        }
        if (!Objects.equals(oldDto.getGroup(), newDto.getGroup())) {
            String oldG = oldDto.getGroup().stream().map(Group::getName).reduce((a, b) -> a + ", " + b).orElse("");
            String newG = newDto.getGroup().stream().map(Group::getName).reduce((a, b) -> a + ", " + b).orElse("");
            changes.add(new TestCaseDiff.FieldChange(
                    "Group",
                    oldG,
                    newG,
                    TestCaseDiff.ChangeType.CHANGE_GROUP
            ));
        }

        return changes;
    }
}
