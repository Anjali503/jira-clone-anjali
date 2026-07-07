package com.example.jira.controller;

import com.example.jira.dto.ChangePasswordRequest;
import com.example.jira.dto.DeactivateAccountRequest;
import com.example.jira.dto.RequestEmailChangeRequest;
import com.example.jira.dto.UpdateProfileRequest;
import com.example.jira.dto.UserResponse;
import com.example.jira.dto.VerifyEmailRequest;
import com.example.jira.model.AuditLog;
import com.example.jira.model.User;
import com.example.jira.repository.UserRepository;
import com.example.jira.service.PasswordValidator;
import com.example.jira.service.UserProfileService;
import org.bson.types.ObjectId;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class Usercontroller {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final UserProfileService userProfileService;

    public Usercontroller(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          PasswordValidator passwordValidator,
                          UserProfileService userProfileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.userProfileService = userProfileService;
    }

    @PostMapping("/signup")
    public UserResponse signup(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        if (userRepository.findByEmail(user.getEmail().trim().toLowerCase()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        passwordValidator.validate(user.getPassword());
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(user.getRole() == null ? "USER" : user.getRole());
        user.setActive(true);

        User saved = userRepository.save(user);
        return UserResponse.fromUser(saved);
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody User loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        User user = userRepository.findByEmail(loginRequest.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated.");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return userProfileService.recordSuccessfulLogin(user);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable String id) {
        return userProfileService.getUserProfile(id);
    }

    @PutMapping("/{id}")
    public UserResponse editProfile(
            @PathVariable String id,
            @RequestBody UpdateProfileRequest updatedUser,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        return userProfileService.updateProfile(id, requesterId, updatedUser);
    }

    @PutMapping("/{id}/profile")
    public UserResponse updateProfile(
            @PathVariable String id,
            @RequestBody UpdateProfileRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        return userProfileService.updateProfile(id, requesterId, request);
    }

    @PostMapping("/{id}/avatar")
    public UserResponse uploadAvatar(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        return userProfileService.uploadAvatar(id, requesterId, file);
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable String id) {
        Path avatarPath = userProfileService.getAvatarPath(id);
        Resource resource = new FileSystemResource(avatarPath.toFile());

        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            String detectedType = Files.probeContentType(avatarPath);
            if (detectedType != null) {
                contentType = detectedType;
            }
        } catch (Exception ignored) {
            // keep default content type
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping("/{id}/email/request-change")
    public ResponseEntity<Map<String, String>> requestEmailChange(
            @PathVariable String id,
            @RequestBody RequestEmailChangeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        userProfileService.requestEmailChange(id, requesterId, request);
        return ResponseEntity.ok(Map.of("message", "Verification email sent to the new address."));
    }

    @PostMapping("/{id}/email/verify")
    public UserResponse verifyEmailChange(
            @PathVariable String id,
            @RequestBody VerifyEmailRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        return userProfileService.verifyEmailChange(id, requesterId, request);
    }

    @GetMapping("/{id}/email/verify")
    public ResponseEntity<String> verifyEmailChangeByLink(
            @PathVariable String id,
            @RequestParam("token") String token) {
        userProfileService.verifyEmailChangeFromLink(id, token);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("Email verified successfully.");
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable String id,
            @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        userProfileService.changePassword(id, requesterId, request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    @PutMapping("/{id}/deactivate")
    public UserResponse deactivateAccount(
            @PathVariable String id,
            @RequestBody DeactivateAccountRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        return userProfileService.deactivateAccount(id, requesterId, request);
    }

    @GetMapping("/{id}/audit-logs")
    public List<AuditLog> getAuditLogs(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String requesterId) {
        return userProfileService.getAuditLogs(id, requesterId);
    }
}
