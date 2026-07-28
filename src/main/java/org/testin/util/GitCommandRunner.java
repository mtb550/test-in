package org.testin.util;

import org.testin.util.logger.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class GitCommandRunner {

    public static String execute(Path workingDirectory, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDirectory.toFile());
            pb.redirectErrorStream(true);


            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines().collect(Collectors.joining("\n"));
                int exitCode = process.waitFor();

                if (exitCode != 0 && !output.isEmpty()) {
                    throw new RuntimeException("Git command failed: " + output);
                }
                return output;

            } catch (final Exception ex) {
                Logger.error("Git command failed: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        } catch (final Exception ex) {
            Logger.error("Git command interrupted: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}