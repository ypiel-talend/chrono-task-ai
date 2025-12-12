package com.chrono.task.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JiraServiceTest {

    private final JiraService jiraService = new JiraService();

    @Test
    public void testIsJiraUrl() {
        Assertions.assertTrue(jiraService.isJiraUrl("https://qlik-dev.atlassian.net/browse/QTDI-2311"));
        Assertions.assertTrue(jiraService.isJiraUrl("https://mycompany.atlassian.net/browse/ABC-123"));

        Assertions.assertFalse(jiraService.isJiraUrl("http://qlik-dev.atlassian.net/browse/QTDI-2311")); // Non-https
                                                                                                         // (regex
                                                                                                         // expects
                                                                                                         // https)
        Assertions.assertFalse(jiraService.isJiraUrl("https://google.com"));
        Assertions.assertFalse(jiraService.isJiraUrl("random text"));
        Assertions.assertFalse(jiraService.isJiraUrl(null));
    }
}
