package com.example.jira.service;

import com.example.jira.model.Attachment;
import com.example.jira.model.Issue;
import com.example.jira.model.Project;
import com.example.jira.repository.AttachmentRepository;
import com.example.jira.repository.IssueRepository;
import com.example.jira.repository.Projectrepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentService {

    // Allowed MIME types
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpg",
            "image/jpeg",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // Allowed file extensions (extra guard)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "docx"
    );

    // 10 MB in bytes
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final AttachmentRepository attachmentRepository;
    private final IssueRepository issueRepository;
    private final Projectrepository projectRepository;
    private final AuditLogService auditLogService;

    @Value("${attachment.upload-dir:uploads}")
    private String uploadDir;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             IssueRepository issueRepository,
                             Projectrepository projectRepository,
                             AuditLogService auditLogService) {
        this.attachmentRepository = attachmentRepository;
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.auditLogService = auditLogService;
    }

    // =========================================================================
    // UPLOAD
    // =========================================================================
    public Attachment upload(String issueId, MultipartFile file, String userId) {
        // 1. Validate file not empty
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty.");
        }

        // 2. Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File size exceeds the 10 MB limit.");
        }

        // 3. Validate extension
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = getExtension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not allowed. Allowed types: PDF, PNG, JPG, JPEG, DOCX.");
        }

        // 4. Validate MIME type
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (!ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File MIME type not allowed. Allowed types: PDF, PNG, JPG, JPEG, DOCX.");
        }

        // 5. Load issue
        Issue issue = issueRepository.findById(new ObjectId(issueId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found."));

        // 6. Permission check — only project members
        checkMembership(issue, userId);

        // 7. Generate unique filename and persist to disk
        String storedName = UUID.randomUUID().toString() + "." + ext;
        Path dirPath = Paths.get(uploadDir).toAbsolutePath();
        try {
            Files.createDirectories(dirPath);
            Path destination = dirPath.resolve(storedName);
            file.transferTo(destination.toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file: " + e.getMessage());
        }

        // 8. Save metadata in MongoDB
        Attachment attachment = new Attachment();
        attachment.setIssueId(issueId);
        attachment.setUploaderId(userId);
        attachment.setOriginalName(originalName);
        attachment.setStoredName(storedName);
        attachment.setFilePath(dirPath.resolve(storedName).toString());
        attachment.setContentType(contentType);
        attachment.setFileSize(file.getSize());
        attachment.setUploadedAt(Instant.now());

        Attachment saved = attachmentRepository.save(attachment);

        // 9. Audit log
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("originalName", originalName);
        newValues.put("storedName", storedName);
        newValues.put("fileSize", file.getSize());
        newValues.put("issueId", issueId);
        auditLogService.log("CREATE", userId, saved.getId(), "Attachment", null, newValues);

        return saved;
    }

    // =========================================================================
    // LIST BY ISSUE
    // =========================================================================
    public List<Attachment> listByIssue(String issueId, String userId) {
        Issue issue = issueRepository.findById(new ObjectId(issueId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found."));
        checkMembership(issue, userId);
        return attachmentRepository.findByIssueId(issueId);
    }

    // =========================================================================
    // DOWNLOAD — returns the File object; controller handles streaming
    // =========================================================================
    public Attachment getForDownload(String attachmentId, String userId) {
        Attachment attachment = attachmentRepository.findById(new ObjectId(attachmentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found."));

        Issue issue = issueRepository.findById(new ObjectId(attachment.getIssueId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found."));

        checkMembership(issue, userId);
        return attachment;
    }

    // =========================================================================
    // DELETE SINGLE
    // =========================================================================
    public void delete(String attachmentId, String userId) {
        Attachment attachment = attachmentRepository.findById(new ObjectId(attachmentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found."));

        Issue issue = issueRepository.findById(new ObjectId(attachment.getIssueId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found."));

        checkMembership(issue, userId);

        // Audit before deletion
        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("originalName", attachment.getOriginalName());
        previousValues.put("storedName", attachment.getStoredName());
        previousValues.put("fileSize", attachment.getFileSize());
        previousValues.put("issueId", attachment.getIssueId());

        // Delete file from disk
        deleteFileFromDisk(attachment.getFilePath());

        // Delete metadata from MongoDB
        attachmentRepository.deleteById(new ObjectId(attachmentId));

        // Audit log
        auditLogService.log("DELETE", userId, attachmentId, "Attachment", previousValues, null);
    }

    // =========================================================================
    // CASCADE DELETE — called when an Issue is deleted
    // =========================================================================
    public void deleteAllByIssueId(String issueId) {
        List<Attachment> attachments = attachmentRepository.findByIssueId(issueId);
        for (Attachment a : attachments) {
            deleteFileFromDisk(a.getFilePath());
        }
        attachmentRepository.deleteByIssueId(issueId);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void checkMembership(Issue issue, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required.");
        }
        if (issue.getProjectId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Issue has no associated project.");
        }
        Project project;
        try {
            project = projectRepository.findById(new ObjectId(issue.getProjectId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid project ID.");
        }

        boolean isMember = (project.getMemberIds() != null && project.getMemberIds().contains(userId))
                || (project.getOwnerId() != null && project.getOwnerId().equals(userId));

        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only project members can access attachments.");
        }
    }

    private void deleteFileFromDisk(String filePath) {
        if (filePath != null) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                // Log but do not fail the request; file may already be missing
                System.err.println("Warning: could not delete file from disk: " + filePath + " — " + e.getMessage());
            }
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "";
    }
}
