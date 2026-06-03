package org.testin.util;

import org.testin.pojo.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GitDiffProcessor {

    public static List<TestCaseDiff> getPendingChanges(Path repoRoot) throws Exception {
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
                TestCaseDto newDto = Mapper.readValue(absolutePath.toFile(), TestCaseDto.class);
                allChanges.add(new TestCaseDiff(
                        newDto.getId().toString(),
                        relativePath,
                        TestCaseDiff.DiffType.ADDED,
                        null,
                        newDto,
                        List.of(new TestCaseDiff.FieldChange("File", "", "New Test Case Created"))
                ));

            } else if (statusCode.contains("M")) {
                TestCaseDto newDto = Mapper.readValue(absolutePath.toFile(), TestCaseDto.class);

                String gitPath = relativePathStr.replace("\\", "/");
                String oldJsonString = GitCommandRunner.execute(repoRoot, "git", "show", "HEAD:" + gitPath);

                TestCaseDto oldDto = Mapper.readValue(oldJsonString, TestCaseDto.class);

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
            }
        }
        return allChanges;
    }

    private static List<TestCaseDiff.FieldChange> compareFields(TestCaseDto oldDto, TestCaseDto newDto) {
        List<TestCaseDiff.FieldChange> changes = new ArrayList<>();

        if (!Objects.equals(oldDto.getDescription(), newDto.getDescription())) {
            changes.add(new TestCaseDiff.FieldChange("Description", oldDto.getDescription(), newDto.getDescription()));
        }
        if (!Objects.equals(oldDto.getExpectedResult(), newDto.getExpectedResult())) {
            changes.add(new TestCaseDiff.FieldChange("Expected Result", oldDto.getExpectedResult(), newDto.getExpectedResult()));
        }
        if (!Objects.equals(oldDto.getPriority(), newDto.getPriority())) {
            String oldP = oldDto.getPriority().name();
            String newP = newDto.getPriority().name();
            changes.add(new TestCaseDiff.FieldChange("Priority", oldP, newP));
        }

        return changes;
    }
}