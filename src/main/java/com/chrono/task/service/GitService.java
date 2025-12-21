package com.chrono.task.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class GitService {

    public boolean isGitInstalled() {
        try {
            Process process = new ProcessBuilder("git", "--version").start();
            return process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public void initRepository(File path) throws IOException, InterruptedException {
        File gitDir = new File(path, ".git");
        if (!gitDir.exists()) {
            runCommand(path, "git", "init");
        }
    }

    public void backup(File path, String fileName) throws IOException, InterruptedException {
        runCommand(path, "git", "add", fileName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        runCommand(path, "git", "commit", "-m", "Backup " + timestamp);
    }

    public String getLastCommitMessage(File path) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "log", "-1", "--pretty=%B");
        pb.directory(path);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    sb.append(line).append(" ");
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            return "No previous commit";
        }
        return sb.toString().trim();
    }

    private void runCommand(File workingDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // Ignore output
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0 && !command[2].equals("commit")) { // Commit might fail if no changes
            throw new IOException("Git command failed with exit code " + exitCode + ": " + String.join(" ", command));
        }
    }
}
