package com.example.jira.controller;

import com.example.jira.model.Attachment;
import com.example.jira.service.AttachmentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    // =========================================================================
    // POST /api/attachments/issue/{issueId}
    // Upload a file and attach it to an issue.
    // Header: X-User-Id — required
    // Body:   multipart/form-data  field name: "file"
    // =========================================================================
    @PostMapping("/issue/{issueId}")
    public ResponseEntity<Attachment> upload(
            @PathVariable String issueId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }
        Attachment saved = attachmentService.upload(issueId, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // =========================================================================
    // GET /api/attachments/issue/{issueId}
    // List all attachments for an issue.
    // Header: X-User-Id — required
    // =========================================================================
    @GetMapping("/issue/{issueId}")
    public ResponseEntity<List<Attachment>> listByIssue(
            @PathVariable String issueId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }
        List<Attachment> attachments = attachmentService.listByIssue(issueId, userId);
        return ResponseEntity.ok(attachments);
    }

    // =========================================================================
    // GET /api/attachments/{attachmentId}/download
    // Download a file by its attachment ID.
    // Header: X-User-Id — required
    // =========================================================================
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable String attachmentId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }

        Attachment attachment = attachmentService.getForDownload(attachmentId, userId);
        File file = new File(attachment.getFilePath());
        if (!file.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on server.");
        }

        Resource resource = new FileSystemResource(file);
        String contentType = attachment.getContentType() != null
                ? attachment.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalName() + "\"")
                .body(resource);
    }

    // =========================================================================
    // DELETE /api/attachments/{attachmentId}
    // Delete an attachment by ID.
    // Header: X-User-Id — required
    // =========================================================================
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String attachmentId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }
        attachmentService.delete(attachmentId, userId);
        return ResponseEntity.noContent().build();
    }
}
