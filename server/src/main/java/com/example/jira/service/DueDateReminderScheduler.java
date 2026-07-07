package com.example.jira.service;

import com.example.jira.model.Issue;
import com.example.jira.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DueDateReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DueDateReminderScheduler.class);

    private final IssueRepository issueRepository;
    private final NotificationService notificationService;

    public DueDateReminderScheduler(IssueRepository issueRepository,
                                    NotificationService notificationService) {
        this.issueRepository = issueRepository;
        this.notificationService = notificationService;
    }

    /**
     * Runs every hour. Finds issues whose due date falls within the next 23–25 hours
     * and sends a single reminder per issue/assignee (deduplicated via NotificationService).
     */
    @Scheduled(fixedRate = 3600000)
    public void sendDueDateReminders() {
        Instant now = Instant.now();
        Instant windowStart = now.plus(23, ChronoUnit.HOURS);
        Instant windowEnd = now.plus(25, ChronoUnit.HOURS);

        List<Issue> upcoming = issueRepository.findByDueDateBetween(windowStart, windowEnd);
        log.debug("Due-date reminder scan: {} issue(s) in 23–25h window", upcoming.size());

        for (Issue issue : upcoming) {
            String assigneeId = issue.getAssigneeId();
            if (assigneeId == null || assigneeId.isBlank()) {
                continue;
            }

            String issueId = issue.getId();
            String dedupKey = "DUE_REMINDER:" + issueId + ":" + assigneeId;
            String message = "Reminder: task '" + issue.getTitle() + "' is due in approximately 24 hours.";

            notificationService.notifyUser(assigneeId, "DUE_REMINDER", message, dedupKey);
        }
    }
}
