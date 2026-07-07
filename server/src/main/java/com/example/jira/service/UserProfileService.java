package com.example.jira.service;

import com.example.jira.dto.ChangePasswordRequest;
import com.example.jira.dto.DeactivateAccountRequest;
import com.example.jira.dto.RequestEmailChangeRequest;
import com.example.jira.dto.UpdateProfileRequest;
import com.example.jira.dto.UserResponse;
import com.example.jira.dto.VerifyEmailRequest;
import com.example.jira.model.AuditLog;
import com.example.jira.model.User;
import com.example.jira.repository.AuditLogRepository;
import com.example.jira.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserProfileService {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/png",
            "image/jpg",
            "image/jpeg"
    );

    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg"
    );

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final EmailService emailService;

    @Value("${avatar.upload-dir:uploads/avatars}")
    private String avatarUploadDir;

    @Value("${avatar.max-size:2097152}")
    private long maxAvatarSize;

    @Value("${email.verification.expiry-minutes:60}")
    private long verificationExpiryMinutes;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public UserProfileService(UserRepository userRepository,
                              AuditLogRepository auditLogRepository,
                              PasswordEncoder passwordEncoder,
                              PasswordValidator passwordValidator,
                              EmailService emailService) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.emailService = emailService;
    }

    public UserResponse getUserProfile(String userId) {
        return UserResponse.fromUser(loadUser(userId));
    }

    public UserResponse updateProfile(String userId, String requesterId, UpdateProfileRequest request) {
        verifyOwnership(userId, requesterId);
        User user = loadUser(userId);

        Map<String, Object> previousValues = new HashMap<>();
        Map<String, Object> newValues = new HashMap<>();

        if (request.getName() != null && !request.getName().isBlank()) {
            previousValues.put("name", user.getName());
            user.setName(request.getName().trim());
            newValues.put("name", user.getName());
        }

        if (request.getPhone() != null) {
            previousValues.put("phone", user.getPhone());
            user.setPhone(request.getPhone().trim());
            newValues.put("phone", user.getPhone());
        }

        if (request.getGroup() != null) {
            previousValues.put("group", user.getGroup());
            user.setGroup(request.getGroup().trim());
            newValues.put("group", user.getGroup());
        }

        User saved = userRepository.save(user);
        auditLogServiceLog("UPDATE", userId, userId, "User", previousValues, newValues);
        return UserResponse.fromUser(saved);
    }

    public UserResponse uploadAvatar(String userId, String requesterId, MultipartFile file) {
        verifyOwnership(userId, requesterId);
        User user = loadUser(userId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file must not be empty.");
        }

        if (file.getSize() > maxAvatarSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Avatar size exceeds the 2 MB limit.");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = getExtension(originalName).toLowerCase();
        if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported image format. Allowed formats: PNG, JPG, JPEG.");
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        if (!ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported image MIME type. Allowed types: PNG, JPG, JPEG.");
        }

        deleteAvatarFile(user.getAvatarStoredName());

        String storedName = UUID.randomUUID() + "." + extension;
        Path directory = Paths.get(avatarUploadDir).toAbsolutePath();
        try {
            Files.createDirectories(directory);
            Path destination = directory.resolve(storedName);
            file.transferTo(destination.toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store avatar: " + e.getMessage());
        }

        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("avatar", user.getAvatar());

        user.setAvatarStoredName(storedName);
        user.setAvatar("/api/users/" + userId + "/avatar");

        User saved = userRepository.save(user);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("avatar", saved.getAvatar());
        auditLogServiceLog("UPDATE", userId, userId, "User", previousValues, newValues);

        return UserResponse.fromUser(saved);
    }

    public Path getAvatarPath(String userId) {
        User user = loadUser(userId);
        if (user.getAvatarStoredName() == null || user.getAvatarStoredName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found.");
        }

        Path filePath = Paths.get(avatarUploadDir).toAbsolutePath().resolve(user.getAvatarStoredName());
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar file not found.");
        }
        return filePath;
    }

    public void requestEmailChange(String userId, String requesterId, RequestEmailChangeRequest request) {
        verifyOwnership(userId, requesterId);
        User user = loadUser(userId);

        if (request.getNewEmail() == null || request.getNewEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New email is required.");
        }

        String newEmail = request.getNewEmail().trim().toLowerCase();
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New email must differ from current email.");
        }

        userRepository.findByEmail(newEmail).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use.");
            }
        });

        String token = UUID.randomUUID().toString();
        user.setPendingEmail(newEmail);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationExpiry(Instant.now().plus(verificationExpiryMinutes, ChronoUnit.MINUTES));
        userRepository.save(user);

        String confirmationLink = appBaseUrl + "/api/users/" + userId + "/email/verify?token=" + token;
        String message = "Click the link below to confirm your email change:\n\n" + confirmationLink;
        emailService.sendEmail(newEmail, "Verify your new email address", message);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("pendingEmail", newEmail);
        auditLogServiceLog("EMAIL_CHANGE_REQUEST", userId, userId, "User", null, newValues);
    }

    public UserResponse verifyEmailChange(String userId, String requesterId, VerifyEmailRequest request) {
        verifyOwnership(userId, requesterId);

        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is required.");
        }

        return completeEmailVerification(userId, request.getToken());
    }

    public UserResponse verifyEmailChangeFromLink(String userId, String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is required.");
        }

        return completeEmailVerification(userId, token);
    }

    private UserResponse completeEmailVerification(String userId, String token) {
        User user = loadUser(userId);

        if (user.getEmailVerificationToken() == null
                || !user.getEmailVerificationToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token.");
        }

        if (user.getEmailVerificationExpiry() == null
                || user.getEmailVerificationExpiry().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token has expired.");
        }

        if (user.getPendingEmail() == null || user.getPendingEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending email change found.");
        }

        userRepository.findByEmail(user.getPendingEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use.");
            }
        });

        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("email", user.getEmail());

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);

        User saved = userRepository.save(user);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("email", saved.getEmail());
        auditLogServiceLog("EMAIL_CHANGE_VERIFY", userId, userId, "User", previousValues, newValues);

        return UserResponse.fromUser(saved);
    }

    public void changePassword(String userId, String requesterId, ChangePasswordRequest request) {
        verifyOwnership(userId, requesterId);
        User user = loadUser(userId);

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        passwordValidator.validate(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogServiceLog("PASSWORD_CHANGE", userId, userId, "User", null, Map.of("changed", true));
    }

    public UserResponse deactivateAccount(String userId, String requesterId, DeactivateAccountRequest request) {
        verifyOwnership(userId, requesterId);
        User user = loadUser(userId);

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is already deactivated.");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        Map<String, Object> previousValues = new HashMap<>();
        previousValues.put("active", true);

        user.setActive(false);
        user.setDeactivatedAt(Instant.now());

        User saved = userRepository.save(user);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("active", false);
        newValues.put("deactivatedAt", saved.getDeactivatedAt());
        auditLogServiceLog("DEACTIVATE", userId, userId, "User", previousValues, newValues);

        return UserResponse.fromUser(saved);
    }

    public List<AuditLog> getAuditLogs(String userId, String requesterId) {
        verifyOwnership(userId, requesterId);
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public UserResponse recordSuccessfulLogin(User user) {
        user.setLastLoginAt(Instant.now());
        User saved = userRepository.save(user);
        return UserResponse.fromUser(saved);
    }

    private User loadUser(String userId) {
        ObjectId objectId = parseUserId(userId);
        return userRepository.findById(objectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ObjectId parseUserId(String userId) {
        try {
            return new ObjectId(userId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id");
        }
    }

    private void verifyOwnership(String userId, String requesterId) {
        if (requesterId == null || requesterId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }
        if (!userId.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage your own profile.");
        }
    }

    private void deleteAvatarFile(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(avatarUploadDir).toAbsolutePath().resolve(storedName));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete previous avatar: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "";
    }

    private void auditLogServiceLog(String action,
                                    String userId,
                                    String entityId,
                                    String entityType,
                                    Map<String, Object> previousValues,
                                    Map<String, Object> newValues) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUserId(userId);
        log.setEntityId(entityId);
        log.setEntityType(entityType);
        log.setPreviousValues(previousValues);
        log.setNewValues(newValues);
        log.setTimestamp(Instant.now());
        auditLogRepository.save(log);
    }
}
