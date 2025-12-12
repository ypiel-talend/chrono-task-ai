package com.chrono.task.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final Pattern URL_PATTERN = Pattern.compile("https://(.*?)\\.atlassian\\.net/browse/(.*)");

    public JiraService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public static class JiraIssue {
        public String key;
        public String summary;
        public String status;

        public JiraIssue(String key, String summary, String status) {
            this.key = key;
            this.summary = summary;
            this.status = status;
        }
    }

    public boolean isJiraUrl(String url) {
        if (url == null)
            return false;
        return URL_PATTERN.matcher(url).matches();
    }

    public Optional<IssueInfo> parseUrl(String url) {
        if (url == null) {
            return Optional.empty();
        }

        Matcher matcher = URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            String domain = matcher.group(1);
            String issueKey = matcher.group(2);
            return Optional.of(new IssueInfo(url, domain, issueKey));
        }
        return Optional.empty();
    }

    public CompletableFuture<JiraIssue> fetchIssue(String url, String email, String token) {
        final Optional<IssueInfo> issueInfo = this.parseUrl(url);
        if (issueInfo.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid Jira URL"));
        }

        String apiUrl = String.format("https://%s.atlassian.net/rest/api/3/issue/%s",
                issueInfo.get().domain(), issueInfo.get().issueKey());

        String auth = email + ":" + token;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Basic " + encodedAuth)
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode root = objectMapper.readTree(response.body());
                            String summary = root.path("fields").path("summary").asText();
                            String statusName = root.path("fields").path("status").path("name").asText();
                            return new JiraIssue(issueInfo.get().issueKey(), summary, statusName);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse Jira response", e);
                        }
                    } else {
                        throw new RuntimeException("Jira API returned status: " + response.statusCode());
                    }
                });
    }

    public com.chrono.task.model.TaskStatus mapStatus(String jiraStatus) {
        if (jiraStatus == null) {
            return com.chrono.task.model.TaskStatus.UNKNOWN;
        }
        String lower = jiraStatus.toLowerCase();
        return switch (lower) {
            case "new", "candidate" -> com.chrono.task.model.TaskStatus.TODO;
            case "on hold", "accepted", "in progress", "code review", "merge" ->
                com.chrono.task.model.TaskStatus.IN_PROGRESS;
            case "validation", "final check" -> com.chrono.task.model.TaskStatus.VALIDATION;
            case "done", "close", "rejected" -> com.chrono.task.model.TaskStatus.DONE;
            default -> com.chrono.task.model.TaskStatus.UNKNOWN;
        };
    }

    public record IssueInfo(String url, String domain, String issueKey) {
    }
}