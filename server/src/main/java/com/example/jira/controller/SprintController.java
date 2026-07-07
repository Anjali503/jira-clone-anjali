package com.example.jira.controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import com.example.jira.model.Issue;
import com.example.jira.model.Project;
import com.example.jira.model.Sprint;
import com.example.jira.repository.IssueRepository;
import com.example.jira.repository.Projectrepository;
import com.example.jira.repository.SprintRepository;
import com.example.jira.service.NotificationService;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final Projectrepository projectrepository;
    private final NotificationService notificationService;

    public SprintController(
            SprintRepository sprintRepository,
            IssueRepository issueRepository,
            Projectrepository projectrepository,
            NotificationService notificationService) {
        this.sprintRepository = sprintRepository;
        this.issueRepository = issueRepository;
        this.projectrepository = projectrepository;
        this.notificationService = notificationService;
    }

    // =========================
    // CREATE SPRINT
    // =========================
    @PostMapping
    public Sprint createSprint(@RequestBody Sprint sprint) {
        sprint.setStatus("PLANNED");
        return sprintRepository.save(sprint);
    }

    // =========================
    // GET SPRINTS BY PROJECT
    // =========================
    @GetMapping("/project/{projectId}")
    public List<Sprint> getSprintsByProject(@PathVariable String projectId) {
        return sprintRepository.findByProjectId(projectId);
    }

    // =========================
    // START SPRINT
    // =========================
    @PutMapping("/{id}/start")
    public Sprint startSprint(@PathVariable String id) {

        Sprint sprint = sprintRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        sprint.setStatus("ACTIVE");
        sprint.setStartDate(Instant.now());

        Sprint saved = sprintRepository.save(sprint);
        notifySprintEvent(saved, "SPRINT_START", "has started");
        return saved;
    }

    // =========================
    // COMPLETE SPRINT
    // =========================
    @PutMapping("/{id}/complete")
    public Sprint completeSprint(@PathVariable String id) {

        Sprint sprint = sprintRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        sprint.setStatus("COMPLETED");
        sprint.setEndDate(Instant.now());

        Sprint saved = sprintRepository.save(sprint);
        notifySprintEvent(saved, "SPRINT_END", "has ended");
        return saved;
    }

    // =========================
    // UPDATE SPRINT DETAILS
    // =========================
    @PutMapping("/{id}")
    public Sprint updateSprint(
            @PathVariable String id,
            @RequestBody Sprint updated) {

        Sprint sprint = sprintRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        sprint.setName(updated.getName());
        sprint.setGoal(updated.getGoal());
        sprint.setStartDate(updated.getStartDate());
        sprint.setEndDate(updated.getEndDate());

        return sprintRepository.save(sprint);
    }

    // =========================
    // DELETE SPRINT
    // =========================
    @DeleteMapping("/{id}")
    public void deleteSprint(@PathVariable String id) {
        sprintRepository.deleteById(new ObjectId(id));
    }

    // =========================
    // ASSIGN ISSUE TO SPRINT
    // =========================
    @PutMapping("/{sprintId}/issues/{issueId}")
    public Issue addIssueToSprint(
            @PathVariable String sprintId,
            @PathVariable String issueId) {

        Issue issue = issueRepository.findById(new ObjectId(issueId))
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        issue.setUpdatedAt(Instant.now());
        issue.setProjectId(sprintId); // OR add sprintId field if you prefer

        return issueRepository.save(issue);
    }

    private void notifySprintEvent(Sprint sprint, String type, String actionPhrase) {
        List<String> recipientIds = resolveProjectMemberIds(sprint.getProjectId());
        String sprintId = sprint.getId();
        String sprintName = sprint.getName() != null ? sprint.getName() : "Sprint";

        for (String userId : recipientIds) {
            String dedupKey = type + ":" + sprintId + ":" + userId;
            String message = "Sprint '" + sprintName + "' " + actionPhrase + ".";
            notificationService.notifyUser(userId, type, message, dedupKey);
        }
    }

    private List<String> resolveProjectMemberIds(String projectId) {
        if (projectId == null || projectId.isBlank() || !ObjectId.isValid(projectId)) {
            return List.of();
        }

        Project project = projectrepository.findById(new ObjectId(projectId)).orElse(null);
        if (project == null) {
            return List.of();
        }

        Set<String> ids = new LinkedHashSet<>();
        if (project.getOwnerId() != null && !project.getOwnerId().isBlank()) {
            ids.add(project.getOwnerId());
        }
        if (project.getMemberIds() != null) {
            project.getMemberIds().stream()
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(ids::add);
        }
        return new ArrayList<>(ids);
    }
}
