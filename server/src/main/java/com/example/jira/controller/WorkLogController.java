package com.example.jira.controller;

import com.example.jira.model.WorkLog;
import com.example.jira.service.WorkLogService;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/worklogs")
public class WorkLogController {

    private final WorkLogService workLogService;

    public WorkLogController(WorkLogService workLogService) {
        this.workLogService = workLogService;
    }

    @PostMapping
    public WorkLog createWorkLog(@RequestBody WorkLog log, 
                                 @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }
        return workLogService.createWorkLog(log, userId);
    }

    @GetMapping
    public List<WorkLog> getWorkLogsByIssue(@RequestParam("issueId") String issueId) {
        if (issueId == null || issueId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "issueId parameter is required.");
        }
        ObjectId taskObjectId;
        try {
            taskObjectId = new ObjectId(issueId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid issueId format.");
        }
        return workLogService.getWorkLogsByIssue(taskObjectId);
    }

    @PutMapping("/{id}")
    public WorkLog updateWorkLog(@PathVariable("id") String id,
                                 @RequestBody WorkLog log,
                                 @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                                 @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }
        ObjectId workLogObjectId;
        try {
            workLogObjectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid work log ID format.");
        }
        return workLogService.updateWorkLog(workLogObjectId, log, userId, confirm);
    }

    @DeleteMapping("/{id}")
    public void deleteWorkLog(@PathVariable("id") String id,
                              @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }
        ObjectId workLogObjectId;
        try {
            workLogObjectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid work log ID format.");
        }
        workLogService.deleteWorkLog(workLogObjectId, userId, confirm);
    }
}
