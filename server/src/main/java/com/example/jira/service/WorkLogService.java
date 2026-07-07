package com.example.jira.service;

import com.example.jira.model.Issue;
import com.example.jira.model.Project;
import com.example.jira.model.Sprint;
import com.example.jira.model.User;
import com.example.jira.model.WorkLog;
import com.example.jira.repository.IssueRepository;
import com.example.jira.repository.Projectrepository;
import com.example.jira.repository.SprintRepository;
import com.example.jira.repository.UserRepository;
import com.example.jira.repository.WorkLogRepository;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final IssueRepository issueRepository;
    private final Projectrepository projectRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final AuditLogService auditLogService;

    public WorkLogService(WorkLogRepository workLogRepository,
                          IssueRepository issueRepository,
                          Projectrepository projectRepository,
                          UserRepository userRepository,
                          SprintRepository sprintRepository,
                          AuditLogService auditLogService) {
        this.workLogRepository = workLogRepository;
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.sprintRepository = sprintRepository;
        this.auditLogService = auditLogService;
    }

    public WorkLog createWorkLog(WorkLog log, String userId) {
        validateWorkLog(log);

        Issue issue = issueRepository.findById(log.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found."));

        checkPermission(issue, userId);

        log.setCreatedAt(Instant.now());
        log.setUpdatedAt(Instant.now());
        WorkLog saved = workLogRepository.save(log);

        recalculateTotals(log.getTaskId());

        return saved;
    }

    public WorkLog updateWorkLog(ObjectId id, WorkLog newLog, String userId, boolean confirm) {
        if (!confirm) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation required to edit work log.");
        }

        WorkLog oldLog = workLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work log not found."));

        validateWorkLog(newLog);

        Issue issue = issueRepository.findById(oldLog.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Associated issue not found."));

        checkPermission(issue, userId);

        // Store snapshot of previous values for audit
        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("date", oldLog.getDate());
        previousValues.put("duration", oldLog.getDuration());
        previousValues.put("description", oldLog.getDescription());

        // Update fields
        oldLog.setDate(newLog.getDate());
        oldLog.setDuration(newLog.getDuration());
        oldLog.setDescription(newLog.getDescription());
        oldLog.setUpdatedAt(Instant.now());

        WorkLog saved = workLogRepository.save(oldLog);

        // Store snapshot of new values for audit
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("date", saved.getDate());
        newValues.put("duration", saved.getDuration());
        newValues.put("description", saved.getDescription());

        // Save audit log
        auditLogService.log("UPDATE", userId, saved.getId().toHexString(), "WorkLog", previousValues, newValues);

        // Recalculate totals
        recalculateTotals(saved.getTaskId());

        return saved;
    }

    public void deleteWorkLog(ObjectId id, String userId, boolean confirm) {
        if (!confirm) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation required to delete work log.");
        }

        WorkLog oldLog = workLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work log not found."));

        Issue issue = issueRepository.findById(oldLog.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Associated issue not found."));

        checkPermission(issue, userId);

        // Store snapshot of previous values for audit
        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("date", oldLog.getDate());
        previousValues.put("duration", oldLog.getDuration());
        previousValues.put("description", oldLog.getDescription());

        workLogRepository.deleteById(id);

        // Save audit log
        auditLogService.log("DELETE", userId, oldLog.getId().toHexString(), "WorkLog", previousValues, null);

        // Recalculate totals
        recalculateTotals(oldLog.getTaskId());
    }

    public List<WorkLog> getWorkLogsByIssue(ObjectId taskId) {
        return workLogRepository.findByTaskId(taskId);
    }

    private void validateWorkLog(WorkLog log) {
        if (log.getTaskId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task ID is required.");
        }
        if (log.getDuration() == null || log.getDuration() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work duration cannot be negative.");
        }
        if (log.getDate() != null && log.getDate().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work log date cannot be in the future.");
        }
    }

    private void checkPermission(Issue issue, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required.");
        }

        ObjectId userObjectId;
        try {
            userObjectId = new ObjectId(userId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authenticated user ID.");
        }

        User user = userRepository.findById(userObjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found."));

        // 1. Task Assignee
        if (issue.getAssigneeId() != null && issue.getAssigneeId().equals(userId)) {
            return;
        }

        // 2. Global Project Manager / Admin roles
        if ("PROJECT_MANAGER".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }

        // 3. Project Owner (Project Manager of this specific project)
        if (issue.getProjectId() != null) {
            try {
                Project project = projectRepository.findById(new ObjectId(issue.getProjectId())).orElse(null);
                if (project != null && project.getOwnerId() != null && project.getOwnerId().equals(userId)) {
                    return;
                }
            } catch (Exception e) {
                // Ignore project fetch errors and proceed to deny
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Task Assignee or Project Manager is allowed to modify work logs.");
    }

    public void recalculateTotals(ObjectId taskId) {
        Issue issue = issueRepository.findById(taskId).orElse(null);
        if (issue == null) return;

        List<WorkLog> logs = workLogRepository.findByTaskId(taskId);
        double total = logs.stream()
                .mapToDouble(l -> l.getDuration() != null ? l.getDuration() : 0.0)
                .sum();

        issue.setTotalLoggedHours(total);
        issue.setUpdatedAt(Instant.now());
        issueRepository.save(issue);

        if (issue.getSprintId() != null && !issue.getSprintId().isBlank()) {
            recalculateSprintTotals(issue.getSprintId());
        }
    }

    public void recalculateSprintTotals(String sprintId) {
        Sprint sprint = sprintRepository.findById(new ObjectId(sprintId)).orElse(null);
        if (sprint == null) return;

        List<Issue> issues = issueRepository.findBySprintId(sprintId);
        List<ObjectId> issueIds = issues.stream()
                .map(Issue::getObjectId)
                .filter(Objects::nonNull)
                .toList();

        double sprintTotal = 0.0;
        if (!issueIds.isEmpty()) {
            List<WorkLog> logs = workLogRepository.findByTaskIdIn(issueIds);
            sprintTotal = logs.stream()
                    .mapToDouble(l -> l.getDuration() != null ? l.getDuration() : 0.0)
                    .sum();
        }

        sprint.setTotalLoggedHours(sprintTotal);
        sprintRepository.save(sprint);
    }
}
