package com.example.jira.controller;

import com.example.jira.model.Issue;
import com.example.jira.repository.IssueRepository;
import com.example.jira.service.AttachmentService;
import com.example.jira.service.NotificationService;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository issueRepository;
    private final AttachmentService attachmentService;
    private final NotificationService notificationService;

    public IssueController(IssueRepository issueRepository,
                           AttachmentService attachmentService,
                           NotificationService notificationService) {
        this.issueRepository = issueRepository;
        this.attachmentService = attachmentService;
        this.notificationService = notificationService;
    }

    // =========================
    // CREATE ISSUE / SUBTASK
    // =========================
    @PostMapping
    public Issue createIssue(@RequestBody Issue issue) {

        // If a parentId is provided this is a subtask — enforce inheritance
        if (issue.getParentId() != null && !issue.getParentId().isBlank()) {

            Issue parent = issueRepository.findById(new ObjectId(issue.getParentId()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Parent issue not found: " + issue.getParentId()));

            // Subtasks cannot belong to a different project than their parent
            if (issue.getProjectId() != null
                    && !issue.getProjectId().equals(parent.getProjectId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Subtask projectId must match parent projectId.");
            }

            // Inherit project and sprint from parent
            issue.setProjectId(parent.getProjectId());
            issue.setSprintId(parent.getSprintId());

            // Force type to SUBTASK
            issue.setType("SUBTASK");
        }

        Issue saved = issueRepository.save(issue);

        if (saved.getAssigneeId() != null && !saved.getAssigneeId().isBlank()) {
            notifyAssignment(saved);
        }

        return saved;
    }

    // =========================
    // GET BY PROJECT
    // =========================
    @GetMapping("/project/{projectId}")
    public List<Issue> getIssuesByProject(@PathVariable String projectId) {
        return issueRepository.findByProjectId(projectId);
    }

    // =========================
    // GET SUBTASKS BY PARENT
    // =========================
    @GetMapping("/parent/{parentId}")
    public List<Issue> getSubtasksByParent(@PathVariable String parentId) {
        return issueRepository.findByParentId(parentId);
    }

    // =========================
    // GET BY ID
    // =========================
    @GetMapping("/{id}")
    public Issue getIssueById(@PathVariable String id) {
        return issueRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("Issue not found"));
    }

    // =========================
    // UPDATE
    // =========================
    @PutMapping("/{id}")
    public Issue updateIssue(
            @PathVariable String id,
            @RequestBody Issue updated) {

        Issue issue = issueRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        String previousAssigneeId = issue.getAssigneeId();
        String previousStatus = issue.getStatus();

        // ── Phase 2: Parent-completion validation ──────────────────────────────
        // A parent issue cannot move to DONE while any of its subtasks are
        // still incomplete. This check runs only when the requested status is
        // DONE and is skipped entirely for all other transitions.
        if ("DONE".equals(updated.getStatus())) {
            List<Issue> subtasks = issueRepository.findByParentId(id);

            for (Issue subtask : subtasks) {
                if (!"DONE".equals(subtask.getStatus())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Cannot complete '" + issue.getTitle()
                            + "': subtask '" + subtask.getTitle()
                            + "' is still " + subtask.getStatus() + ".");
                }
            }
        }
        // ── End Phase 2 ────────────────────────────────────────────────────────

        issue.setTitle(updated.getTitle());
        issue.setDescription(updated.getDescription());
        issue.setStatus(updated.getStatus());
        issue.setPriority(updated.getPriority());
        issue.setAssigneeId(updated.getAssigneeId());
        issue.setOrder(updated.getOrder());
        issue.setUpdatedAt(Instant.now());
        issue.setComments(updated.getComments());

        // Persist Phase 1 fields
        issue.setSprintId(updated.getSprintId());
        issue.setDependencies(updated.getDependencies());
        issue.setDueDate(updated.getDueDate());

        // parentId is immutable after creation — do not allow re-parenting via PUT
        // (parentId stays whatever it was stored as)

        Issue saved = issueRepository.save(issue);

        if (!Objects.equals(previousAssigneeId, saved.getAssigneeId())
                && saved.getAssigneeId() != null
                && !saved.getAssigneeId().isBlank()) {
            notifyAssignment(saved);
        }

        if (!Objects.equals(previousStatus, saved.getStatus())) {
            notifyStatusChange(saved, previousStatus);
        }

        return saved;
    }


    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    public void deleteIssue(@PathVariable String id) {
        // Cascade: remove all attachments (files + MongoDB docs) for this issue
        attachmentService.deleteAllByIssueId(id);
        // Then delete the issue itself
        issueRepository.deleteById(new ObjectId(id));
    }

    private void notifyAssignment(Issue issue) {
        String assigneeId = issue.getAssigneeId();
        String issueId = issue.getId();
        String dedupKey = "ASSIGNMENT:" + issueId + ":" + assigneeId;
        String message = "You have been assigned to task '" + issue.getTitle() + "'.";
        notificationService.notifyUser(assigneeId, "ASSIGNMENT", message, dedupKey);
    }

    private void notifyStatusChange(Issue issue, String previousStatus) {
        String recipientId = issue.getAssigneeId();
        if (recipientId == null || recipientId.isBlank()) {
            recipientId = issue.getReporterId();
        }
        if (recipientId == null || recipientId.isBlank()) {
            return;
        }

        String issueId = issue.getId();
        String newStatus = issue.getStatus();
        String dedupKey = "STATUS_CHANGE:" + issueId + ":" + previousStatus + "->" + newStatus;
        String message = "Task '" + issue.getTitle() + "' status changed from "
                + previousStatus + " to " + newStatus + ".";
        notificationService.notifyUser(recipientId, "STATUS_CHANGE", message, dedupKey);
    }
}
