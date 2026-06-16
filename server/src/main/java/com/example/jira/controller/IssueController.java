package com.example.jira.controller;
import com.example.jira.model.Issue;
import com.example.jira.repository.IssueRepository;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository issueRepository;

    public IssueController(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
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

        return issueRepository.save(issue);
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

        // parentId is immutable after creation — do not allow re-parenting via PUT
        // (parentId stays whatever it was stored as)

        return issueRepository.save(issue);
    }


    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    public void deleteIssue(@PathVariable String id) {
        issueRepository.deleteById(new ObjectId(id));
    }
}
